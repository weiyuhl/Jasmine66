package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.prompt.executor.ApiType
import com.lhzkml.jasmine.core.prompt.executor.ChatClientConfig
import com.lhzkml.jasmine.core.prompt.executor.ChatClientFactory
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatClient 的生命周期管理器。
 * 位于 core:data 层，封装对 jasmine-core 的所有直接依赖。
 * feature 层通过注入此类来使用聊天功能，无需直接依赖 jasmine-core。
 */
@Singleton
class ChatClientManager @Inject constructor(
    private val providerRepo: ChatProviderRepository,
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

    /** 获取当前活跃模型名称 */
    fun getActiveModel(): String {
        val id = providerRepo.getActiveProviderId() ?: return ""
        return providerRepo.getModel(id)
    }

    /**
     * 刷新供应商状态并重建 ChatClient。
     * 由 ChatScreen 在进入 Composition 时调用，也由 configChangesFlow 触发。
     */
    fun refreshState() {
        val id = providerRepo.getActiveProviderId()
        if (id.isNullOrBlank()) {
            _setupState.value = "配置失败: ActiveProviderId 为空"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return
        }
        val preset = ChatProviderRepository.PRESETS.find { it.id == id }
        if (preset == null) {
            _setupState.value = "配置失败: 找不到预设 ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return
        }
        val apiKey = providerRepo.getApiKey(id)
        if (apiKey.isBlank()) {
            _setupState.value = "配置失败: API Key 为空 ($id)"
            _isConfigured.value = false
            chatClient?.close()
            chatClient = null
            return
        }

        val config = buildActiveConfig(id, preset, apiKey)
        _isConfigured.value = config != null
        if (config == null) {
            _setupState.value = "配置失败: config 构建失败"
        } else {
            _setupState.value = "配置成功: ${config.providerName} (${config.providerId})"
        }

        // 重建 client
        chatClient?.close()
        chatClient = config?.let {
            try {
                ChatClientFactory.create(it)
            } catch (e: Exception) {
                _setupState.value = "配置失败: 工厂类抛出异常 (${e.message})"
                null
            }
        }
    }

    /**
     * 流式聊天。
     * @param messages  消息历史（使用 core:data 层的 SimpleChatMessage）
     * @param model     模型名称
     * @param onChunk   每收到一个 token 回调
     * @return 聊天结果摘要
     * @throws IllegalStateException 如果供应商未配置
     * @throws Exception 网络或 API 异常
     */
    suspend fun streamChat(
        messages: List<SimpleChatMessage>,
        model: String,
        onChunk: suspend (String) -> Unit,
    ): StreamChatResult {
        val client = chatClient
            ?: throw IllegalStateException("发送失败: ${_setupState.value}")

        // SimpleChatMessage → jasmine-core ChatMessage
        val apiMessages = messages.map { msg ->
            when (msg.role) {
                "user" -> ChatMessage.user(msg.content)
                "assistant" -> ChatMessage.assistant(msg.content)
                "system" -> ChatMessage.system(msg.content)
                else -> ChatMessage.user(msg.content)
            }
        }

        val result = client.chatStreamWithUsage(
            messages = apiMessages,
            model = model,
            onChunk = onChunk,
        )

        return StreamChatResult(
            content = result.content,
            finishReason = result.finishReason,
        )
    }

    /** 关闭当前 client（ViewModel onCleared 时调用） */
    fun close() {
        chatClient?.close()
        chatClient = null
    }

    // ==================== 私有方法 ====================

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

        return ChatClientConfig(
            providerId = id,
            providerName = preset.name,
            apiKey = apiKey,
            baseUrl = providerRepo.getBaseUrl(id),
            apiType = apiType,
        )
    }
}
