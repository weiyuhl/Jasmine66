package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.core.prompt.executor.ApiType
import com.lhzkml.jasmine.core.prompt.executor.ChatClientConfig
import com.lhzkml.jasmine.core.prompt.executor.ChatClientFactory
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.ContextManager
import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.ModelRegistry
import com.lhzkml.jasmine.core.prompt.llm.SystemPromptManager
import com.lhzkml.jasmine.core.prompt.model.ModelInfo
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatClientManager @Inject constructor(
    private val providerRepo: ChatProviderRepository,
    private val toolManager: ChatToolManager,
    private val skillManager: SkillManager,
    private val messageBuilder: ChatMessageBuilder,
    private val toolLoopExecutor: ToolLoopExecutor,
) {
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _setupState = MutableStateFlow("Not loaded")
    val setupState: StateFlow<String> = _setupState.asStateFlow()

    val configChangesFlow = providerRepo.configChangesFlow

    private var chatClient: ChatClient? = null
    private val stateMutex = Mutex()

    private var contextManager: ContextManager = ContextManager()

    // ==================== State management ====================

    fun getActiveModel(): String {
        val id = providerRepo.getActiveProviderId() ?: return ""
        return providerRepo.getModel(id)
    }

    fun getSystemPromptPresets(): List<Pair<String, String>> {
        return SystemPromptManager.presets.map { it.name to it.prompt }
    }

    fun getCustomSystemPrompt(): String {
        val id = providerRepo.getActiveProviderId() ?: return ""
        return providerRepo.getSystemPrompt(id)
    }

    fun setCustomSystemPrompt(prompt: String) {
        val id = providerRepo.getActiveProviderId() ?: return
        providerRepo.setSystemPrompt(id, prompt)
    }

    fun estimateTokens(messages: List<SimpleChatMessage>): Int {
        val apiMessages = messages.map { messageBuilder.toApiMessage(it) }
        return contextManager.estimateTokens(apiMessages)
    }

    suspend fun refreshState() = stateMutex.withLock {
        val id = providerRepo.getActiveProviderId()
        if (id.isNullOrBlank()) {
            _setupState.value = "Config failed: ActiveProviderId is empty"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }
        val preset = ChatProviderRepository.PRESETS.find { it.id == id }
        if (preset == null) {
            _setupState.value = "Config failed: preset not found ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }
        val apiKey = providerRepo.getApiKey(id)
        if (apiKey.isBlank()) {
            _setupState.value = "Config failed: API Key is empty ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }

        val config = buildActiveConfig(id, preset, apiKey)
        if (config == null) {
            _setupState.value = "Config failed: config build failed"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return@withLock
        }

        chatClient?.close()
        chatClient = try {
            ChatClientFactory.create(config)
        } catch (e: Exception) {
            _setupState.value = "Config failed: factory threw exception (${e.message})"
            null
        }

        val isOk = chatClient != null
        _isConfigured.value = isOk
        if (isOk) {
            _setupState.value = "Config OK: ${config.providerName} (${config.providerId})"
        }

        val model = providerRepo.getModel(id)
        val provider = mapToLLMProvider(preset.apiTypeString, id)
        contextManager = ContextManager.forModel(model, provider)
    }

    // ==================== Core chat method ====================

    suspend fun streamChat(
        messages: List<SimpleChatMessage>,
        model: String,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit = {},
        onResumeAttempt: suspend (Int) -> Unit = {},
        onToolCallStart: suspend (String, String) -> Unit = { _, _ -> },
        onToolCallResult: suspend (String, String) -> Unit = { _, _ -> },
        uiEnabled: Boolean = true,
        webSearchEnabled: Boolean = true,
    ): StreamChatResult {
        // 注意：client 引用在锁外使用，并发 refreshState() 可能关闭旧 client。
        // 已知限制：用户在流式传输期间切换 provider 会导致当前请求失败。
        val (client, ctxManager) = stateMutex.withLock {
            val c = chatClient ?: throw IllegalStateException("Send failed: ${_setupState.value}")
            c to contextManager
        }
        // 验证 client 未被关闭
        if (client is java.io.Closeable) {
            try { client.toString() } catch (_: Exception) {
                throw IllegalStateException("Client was closed during streaming setup")
            }
        }

        val allTools = toolManager.descriptors()
        val tools = if (webSearchEnabled) allTools else allTools.filter { it.name != "web_search" }
        val hasTools = tools.isNotEmpty()

        val apiMessages = messageBuilder.buildApiMessagesWithSystemPrompt(
            messages = messages,
            providerRepo = providerRepo,
            skillManager = skillManager,
            uiEnabled = uiEnabled,
            webSearchEnabled = webSearchEnabled,
        )

        val samplingParams = buildSamplingParams()

        return if (!hasTools) {
            toolLoopExecutor.streamWithoutTools(
                client = client,
                messages = apiMessages,
                model = model,
                samplingParams = samplingParams,
                contextManager = ctxManager,
                onChunk = onChunk,
                onThinking = onThinking,
                onResumeAttempt = onResumeAttempt,
            )
        } else {
            toolLoopExecutor.executeToolLoop(
                client = client,
                messages = apiMessages,
                model = model,
                samplingParams = samplingParams,
                tools = tools,
                contextManager = ctxManager,
                onChunk = onChunk,
                onThinking = onThinking,
                onResumeAttempt = onResumeAttempt,
                onToolCallStart = onToolCallStart,
                onToolCallResult = onToolCallResult,
            )
        }
    }

    // ==================== Model info & balance ====================

    suspend fun listModelsFor(
        providerId: String,
        apiKey: String,
        baseUrl: String,
    ): List<String> {
        return withTransientClient(providerId, apiKey, baseUrl) { client ->
            try {
                val models: List<ModelInfo> = client.listModels()
                ModelRegistry.registerFromApi(client.provider, models)
                models.map { it.id }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun getBalanceFor(
        providerId: String,
        apiKey: String,
        baseUrl: String,
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

    suspend fun close() = stateMutex.withLock {
        chatClient?.close()
        chatClient = null
    }

    // ==================== Private helpers ====================

    private suspend fun <T> withTransientClient(
        providerId: String,
        apiKey: String,
        baseUrl: String,
        action: suspend (ChatClient) -> T,
    ): T? = withContext(Dispatchers.IO) {
        val preset = ChatProviderRepository.PRESETS.find { it.id == providerId } ?: return@withContext null
        if (apiKey.isBlank()) return@withContext null

        val apiType = try {
            ApiType.valueOf(preset.apiTypeString)
        } catch (e: Exception) {
            ApiType.OPENAI
        }

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
            try { tempClient.close() } catch (_: Exception) { }
        }
    }

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
}
