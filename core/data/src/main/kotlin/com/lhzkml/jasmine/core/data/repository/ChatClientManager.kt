package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.prompt.executor.ApiType
import com.lhzkml.jasmine.core.prompt.executor.ChatClientConfig
import com.lhzkml.jasmine.core.prompt.executor.ChatClientFactory
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.ContextManager
import com.lhzkml.jasmine.core.prompt.llm.LLModel
import com.lhzkml.jasmine.core.prompt.llm.ModelRegistry
import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.StreamResumeHelper
import com.lhzkml.jasmine.core.prompt.llm.SystemPromptManager
import com.lhzkml.jasmine.core.prompt.llm.chatStreamWithUsageAndThinking
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.ModelInfo
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import com.lhzkml.jasmine.core.prompt.model.ToolCall
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * ChatClient 的生命周期管理器。
 * 位于 core:data 层，封装对 jasmine-core 的所有直接依赖。
 * feature 层通过注入此类来使用聊天功能，无需直接依赖 jasmine-core。
 *
 * 集成功能：
 * - P0: System Prompt 自动注入 + Context Manager 上下文窗口裁剪
 * - P1: SamplingParams 采样参数 + ThinkingChatClient 思考过程 + Stream Resume 断流续传
 * - P2: listModels 动态模型列表 + getBalance 余额查询
 */
@Singleton
class ChatClientManager @Inject constructor(
    private val providerRepo: ChatProviderRepository,
    private val toolManager: ChatToolManager,
) {
    private val _isConfigured = MutableStateFlow(false)
    /** 供应商是否已就绪 */
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _setupState = MutableStateFlow("尚未加载配置")
    /** 诊断信息（调试用） */
    val setupState: StateFlow<String> = _setupState.asStateFlow()

    /** 配置变更信号 — 透传 ChatProviderRepository 的事件流 */
    val configChangesFlow = providerRepo.configChangesFlow

    private var chatClient: ChatClient? = null
    
    /** 状态更新锁，防止并发重建 client */
    private val stateMutex = Mutex()

    /** System Prompt 管理器 */
    private val systemPromptManager = SystemPromptManager()

    /** 断流续传助手 */
    private val streamResumeHelper = StreamResumeHelper(maxResumes = 3)

    /** 上下文窗口管理器（在 refreshState 时根据模型更新） */
    private var contextManager: ContextManager = ContextManager()

    // ==================== 基本状态管理 ====================

    /** 获取当前活跃模型名称 */
    fun getActiveModel(): String {
        val id = providerRepo.getActiveProviderId() ?: return ""
        return providerRepo.getModel(id)
    }

    /** 获取 System Prompt 预设列表 */
    fun getSystemPromptPresets(): List<Pair<String, String>> {
        return SystemPromptManager.presets.map { it.name to it.prompt }
    }

    /** 获取当前供应商的自定义 System Prompt */
    fun getCustomSystemPrompt(): String {
        val id = providerRepo.getActiveProviderId() ?: return ""
        return providerRepo.getSystemPrompt(id)
    }

    /** 设置当前供应商的自定义 System Prompt */
    fun setCustomSystemPrompt(prompt: String) {
        val id = providerRepo.getActiveProviderId() ?: return
        providerRepo.setSystemPrompt(id, prompt)
    }

    /** 估算消息列表的 token 数 */
    fun estimateTokens(messages: List<SimpleChatMessage>): Int {
        val apiMessages = messages.map { toApiMessage(it) }
        return contextManager.estimateTokens(apiMessages)
    }

    /**
     * 刷新供应商状态并重建 ChatClient。
     */
    suspend fun refreshState() = stateMutex.withLock {
        val id = providerRepo.getActiveProviderId()
        if (id.isNullOrBlank()) {
            _setupState.value = "配置失败: ActiveProviderId 为空"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }
        val preset = ChatProviderRepository.PRESETS.find { it.id == id }
        if (preset == null) {
            _setupState.value = "配置失败: 找不到预设 ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }
        val apiKey = providerRepo.getApiKey(id)
        if (apiKey.isBlank()) {
            _setupState.value = "配置失败: API Key 为空 ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }

        val config = buildActiveConfig(id, preset, apiKey)
        if (config == null) {
            _setupState.value = "配置失败: config 构建失败"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }

        // 重建 client
        chatClient?.close()
        chatClient = try {
            ChatClientFactory.create(config)
        } catch (e: Exception) {
            _setupState.value = "配置失败: 工厂类抛出异常 (${e.message})"
            null
        }
        
        val isOk = chatClient != null
        _isConfigured.value = isOk
        if (isOk) {
            _setupState.value = "配置成功: ${config.providerName} (${config.providerId})"
        }

        // 更新 ContextManager（根据模型上下文长度）
        val model = providerRepo.getModel(id)
        val provider = mapToLLMProvider(preset.apiTypeString, id)
        contextManager = ContextManager.forModel(model, provider)
    }

    private fun mapToLLMProvider(apiType: String, providerId: String): LLMProvider {
        return when (providerId.lowercase()) {
            "deepseek" -> LLMProvider.DeepSeek
            "siliconflow" -> LLMProvider.SiliconFlow
            "claude" -> LLMProvider.Claude
            "gemini" -> LLMProvider.Gemini
            "openai" -> LLMProvider.OpenAI
            "openrouter" -> LLMProvider.OpenRouter
            else -> {
                when (apiType.uppercase()) {
                    "OPENAI" -> LLMProvider.OpenAI
                    "CLAUDE" -> LLMProvider.Claude
                    "GEMINI" -> LLMProvider.Gemini
                    else -> LLMProvider.Custom(providerId)
                }
            }
        }
    }

    // ==================== 核心聊天方法 ====================

    /**
     * 流式聊天（集成全部功能）。
     *
     * 内部自动处理：
     * 1. System Prompt 注入
     * 2. Context Manager 裁剪
     * 3. SamplingParams 传入
     * 4. ThinkingChatClient 检测 + onThinking 回调
     * 5. StreamResumeHelper 断流续传
     * 6. Tool Calling 工具调用
     *
     * @param messages  消息历史（使用 core:data 层的 SimpleChatMessage）
     * @param model     模型名称
     * @param onChunk   每收到一个 token 回调
     * @param onThinking 思考过程回调（DeepSeek/Claude 推理模型）
     * @param onResumeAttempt 断流续传尝试回调
     * @param onToolCallStart 工具调用开始回调
     * @param onToolCallResult 工具调用结果回调
     * @return 聊天结果摘要
     */
    suspend fun streamChat(
        messages: List<SimpleChatMessage>,
        model: String,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit = {},
        onResumeAttempt: suspend (Int) -> Unit = {},
        onToolCallStart: suspend (String, String) -> Unit = { _, _ -> },
        onToolCallResult: suspend (String, String) -> Unit = { _, _ -> },
        kaiUiEnabled: Boolean = true,
    ): StreamChatResult {
        val client = chatClient
            ?: throw IllegalStateException("发送失败: ${_setupState.value}")

        val tools = toolManager.descriptors()
        val hasTools = tools.isNotEmpty()

        // 1. 转换消息并注入 System Prompt
        val apiMessages = buildApiMessagesWithSystemPrompt(messages, kaiUiEnabled)

        // 2. 上下文窗口裁剪
        val trimmedMessages = contextManager.trimMessages(apiMessages)

        // 3. 读取采样参数
        val samplingParams = buildSamplingParams()

        if (!hasTools) {
            // 无工具模式：直接调用
            val result = streamResumeHelper.streamWithResume(
                client = client,
                messages = trimmedMessages,
                model = model,
                maxTokens = samplingParams.maxTokens,
                samplingParams = samplingParams.coreSamplingParams,
                onChunk = onChunk,
                onThinking = onThinking,
                onResumeAttempt = onResumeAttempt,
            )

            return StreamChatResult(
                content = result.content,
                finishReason = result.finishReason,
                thinking = result.thinking,
                toolCalls = toolManager.toToolCallInfoList(result.toolCalls),
            )
        }

        // 工具调用模式：执行 agent loop
        return executeToolLoop(
            client = client,
            messages = trimmedMessages,
            model = model,
            samplingParams = samplingParams,
            onChunk = onChunk,
            onThinking = onThinking,
            onResumeAttempt = onResumeAttempt,
            onToolCallStart = onToolCallStart,
            onToolCallResult = onToolCallResult,
        )
    }

    private suspend fun executeToolLoop(
        client: ChatClient,
        messages: List<ChatMessage>,
        model: String,
        samplingParams: ResolvedSamplingParams,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit,
        onResumeAttempt: suspend (Int) -> Unit,
        onToolCallStart: suspend (String, String) -> Unit,
        onToolCallResult: suspend (String, String) -> Unit,
    ): StreamChatResult {
        val mutableMessages = messages.toMutableList()
        val allToolCalls = mutableListOf<ToolCallInfo>()
        var finalContent = ""
        var finalThinking = ""
        var finalFinishReason: String? = null
        var iterations = 0
        val maxIterations = 15
        val recentToolSignatures = mutableListOf<String>()
        val maxRepeatedCalls = 3

        while (iterations < maxIterations) {
            iterations++

            val trimmedMessages = contextManager.trimMessages(mutableMessages.toList())

            val result = streamResumeHelper.streamWithResume(
                client = client,
                messages = trimmedMessages,
                model = model,
                maxTokens = samplingParams.maxTokens,
                samplingParams = samplingParams.coreSamplingParams,
                tools = toolManager.descriptors(),
                onChunk = onChunk,
                onThinking = onThinking,
                onResumeAttempt = onResumeAttempt,
            )

            finalFinishReason = result.finishReason
            if (result.thinking.isNullOrBlank().not()) {
                finalThinking = result.thinking ?: finalThinking
            }

            if (!result.hasToolCalls) {
                if (result.content.isNotEmpty()) {
                    finalContent = result.content
                }
                break
            }

            val signatures = result.toolCalls.map { "${it.name}:${it.arguments.hashCode()}" }
            if (isRepeatingToolCalls(recentToolSignatures, signatures, maxRepeatedCalls)) {
                finalContent = result.content
                finalFinishReason = "repeated_tool_calls"
                break
            }
            recentToolSignatures.addAll(signatures)
            while (recentToolSignatures.size > maxRepeatedCalls * 2) {
                recentToolSignatures.removeAt(0)
            }

            allToolCalls.addAll(toolManager.toToolCallInfoList(result.toolCalls))

            val toolResults = executeToolCallsInParallel(
                toolCalls = result.toolCalls,
                onToolCallStart = onToolCallStart,
                onToolCallResult = onToolCallResult,
            )

            mutableMessages.add(
                ChatMessage.assistantWithToolCalls(
                    toolCalls = result.toolCalls,
                    content = result.content,
                )
            )
            for (toolResult in toolResults) {
                mutableMessages.add(ChatMessage.toolResult(toolResult))
            }

            if (result.content.isNotEmpty()) {
                finalContent = result.content
            }
        }

        if (iterations >= maxIterations && finalContent.isEmpty()) {
            mutableMessages.add(ChatMessage.user("你已经进行了 $maxIterations 轮工具调用。请根据目前收集到的信息，直接给出总结回复，不要再调用工具。"))
            val finalResult = client.chatStreamWithUsageAndThinking(
                messages = contextManager.trimMessages(mutableMessages),
                model = model,
                maxTokens = samplingParams.maxTokens,
                samplingParams = samplingParams.coreSamplingParams,
                tools = emptyList(),
                onChunk = onChunk,
                onThinking = onThinking,
            )
            finalContent = finalResult.content
            finalFinishReason = "max_iterations"
        }

        return StreamChatResult(
            content = finalContent,
            finishReason = finalFinishReason,
            thinking = finalThinking,
            toolCalls = allToolCalls,
        )
    }

    private fun isRepeatingToolCalls(
        recentSignatures: List<String>,
        currentSignatures: List<String>,
        maxRepeatedCalls: Int,
    ): Boolean {
        if (recentSignatures.isEmpty()) return false
        val repeatedCount = currentSignatures.count { it in recentSignatures }
        return repeatedCount >= maxRepeatedCalls
    }

    private suspend fun executeToolCallsInParallel(
        toolCalls: List<ToolCall>,
        onToolCallStart: suspend (String, String) -> Unit,
        onToolCallResult: suspend (String, String) -> Unit,
    ): List<ToolResult> = coroutineScope {
        toolCalls.map { call ->
            async {
                onToolCallStart(call.name, call.arguments)
                val result = withContext(Dispatchers.IO) {
                    withTimeout(120.seconds) {
                        toolManager.execute(call)
                    }
                }
                onToolCallResult(call.name, result.content)
                result
            }
        }.awaitAll()
    }

    // ==================== P2: 模型列表 & 余额 ====================

    /**
     * 获取指定供应商可用的模型列表 (Transient - 不触发全局状态变更)
     * @param apiKey 临时 API Key (UI 实时值)
     * @param baseUrl 临时 Base URL (UI 实时值)
     */
    suspend fun listModelsFor(
        providerId: String,
        apiKey: String,
        baseUrl: String
    ): List<String> {
        return withTransientClient(providerId, apiKey, baseUrl) { client ->
            try {
                val models: List<ModelInfo> = client.listModels()
                // 动态注册模型元数据
                ModelRegistry.registerFromApi(client.provider, models)
                models.map { it.id }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    /**
     * 查询指定供应商的 API 余额 (Transient - 不触发全局状态变更)
     */
    suspend fun getBalanceFor(
        providerId: String,
        apiKey: String,
        baseUrl: String
    ): String? {
        return withTransientClient(providerId, apiKey, baseUrl) { client ->
            try {
                val balance = client.getBalance() ?: return@withTransientClient "该计划暂不支持余额查询"
                if (!balance.isAvailable) return@withTransientClient "该供应商目前无法获取余额信息"

                val builder = StringBuilder()
                balance.balances.forEach { detail ->
                    builder.append("💰 ${detail.currency}: ${detail.totalBalance}")
                    if (!detail.grantedBalance.isNullOrBlank()) {
                        builder.append(" (含赠送 ${detail.grantedBalance})")
                    }
                    builder.append("\n")
                }
                builder.toString().trim().ifBlank { "未查询到可用余额" }
            } catch (e: Exception) {
                "查询失败: ${e.message}"
            }
        }
    }

    /** 关闭当前 client */
    fun close() {
        chatClient?.close()
        chatClient = null
    }

    // ==================== 私有方法 ====================

    /**
     * 使用临时配置创建瞬时客户端执行操作，不触碰全局状态
     */
    private suspend fun <T> withTransientClient(
        providerId: String,
        apiKey: String,
        baseUrl: String,
        action: suspend (ChatClient) -> T,
    ): T? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val preset = ChatProviderRepository.PRESETS.find { it.id == providerId } ?: return@withContext null
        if (apiKey.isBlank()) return@withContext null

        val apiType = try {
            ApiType.valueOf(preset.apiTypeString)
        } catch (e: Exception) {
            ApiType.OPENAI
        }

        // 优先使用用户输入的 baseUrl，若无则使用预设
        val finalUrl = if (baseUrl.isNotBlank()) baseUrl else preset.defaultBaseUrl
        if (finalUrl.isBlank()) return@withContext null

        val config = ChatClientConfig(
            providerId = providerId,
            providerName = preset.name,
            apiKey = apiKey,
            baseUrl = finalUrl,
            apiType = apiType,
        )

        val tempClient = try {
            ChatClientFactory.create(config)
        } catch (e: Exception) {
            return@withContext null
        }

        return@withContext try {
            action(tempClient)
        } finally {
            try {
                tempClient.close()
            } catch (_: Exception) { }
        }
    }

    /**
     * 构建包含 System Prompt 的 API 消息列表
     */
    private fun buildApiMessagesWithSystemPrompt(messages: List<SimpleChatMessage>, kaiUiEnabled: Boolean = true): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        // 注入 System Prompt
        val id = providerRepo.getActiveProviderId() ?: ""
        val customPrompt = providerRepo.getSystemPrompt(id)
        val systemMsg = systemPromptManager.createSystemMessage(
            if (customPrompt.isNotBlank()) customPrompt else null,
            kaiUiEnabled = kaiUiEnabled,
        )
        result.add(systemMsg)

        // 转换用户消息
        for (msg in messages) {
            result.add(toApiMessage(msg))
        }

        return result
    }

    private fun toApiMessage(msg: SimpleChatMessage): ChatMessage {
        return when (msg.role) {
            "user" -> ChatMessage.user(msg.content)
            "assistant" -> {
                if (!msg.toolCalls.isNullOrEmpty()) {
                    val toolCalls = msg.toolCalls.map {
                        com.lhzkml.jasmine.core.prompt.model.ToolCall(
                            id = it.id,
                            name = it.name,
                            arguments = it.arguments,
                        )
                    }
                    ChatMessage.assistantWithToolCalls(toolCalls, msg.content)
                } else {
                    ChatMessage.assistant(msg.content)
                }
            }
            "tool" -> ChatMessage(
                role = "tool",
                content = msg.content,
                toolCallId = msg.toolCallId,
                toolName = msg.toolName,
            )
            "system" -> ChatMessage.system(msg.content)
            else -> ChatMessage.user(msg.content)
        }
    }

    /**
     * 读取当前供应商的采样参数
     */
    private fun buildSamplingParams(): ResolvedSamplingParams {
        val id = providerRepo.getActiveProviderId() ?: return ResolvedSamplingParams()
        val temperature = providerRepo.getTemperature(id)
        val topP = providerRepo.getTopP(id)
        val maxTokens = providerRepo.getMaxTokens(id)

        val core = if (temperature != null || topP != null) {
            SamplingParams(temperature = temperature, topP = topP)
        } else {
            null
        }
        return ResolvedSamplingParams(coreSamplingParams = core, maxTokens = maxTokens)
    }

    private fun buildActiveConfig(
        id: String,
        preset: ProviderPreset,
        apiKey: String,
    ): ChatClientConfig? {
        val apiType = try {
            ApiType.valueOf(preset.apiTypeString)
        } catch (e: IllegalArgumentException) {
            ApiType.OPENAI
        }

        val baseUrlRepo = providerRepo.getBaseUrl(id)
        val finalBaseUrl = if (baseUrlRepo.isNotBlank()) baseUrlRepo else preset.defaultBaseUrl

        if (finalBaseUrl.isBlank()) return null

        return ChatClientConfig(
            providerId = id,
            providerName = preset.name,
            apiKey = apiKey,
            baseUrl = finalBaseUrl,
            apiType = apiType,
        )
    }

    /**
     * 内部解析后的采样参数（含 maxTokens 单独拆出）
     */
    private data class ResolvedSamplingParams(
        val coreSamplingParams: SamplingParams? = null,
        val maxTokens: Int? = null,
    )
}
