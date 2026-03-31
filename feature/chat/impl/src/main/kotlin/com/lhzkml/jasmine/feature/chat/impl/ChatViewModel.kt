package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.repository.ChatProviderRepository
import com.lhzkml.jasmine.core.prompt.executor.ApiType
import com.lhzkml.jasmine.core.prompt.executor.ChatClientConfig
import com.lhzkml.jasmine.core.prompt.executor.ChatClientFactory
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI 层聊天消息
 */
data class UiChatMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    val providerRepo: ChatProviderRepository,
) : ViewModel() {

    private val _chatPrompt = MutableStateFlow("")
    val chatPrompt: StateFlow<String> = _chatPrompt.asStateFlow()

    private val _isChatRunning = MutableStateFlow(false)
    val isChatRunning: StateFlow<Boolean> = _isChatRunning.asStateFlow()

    private val _messages = MutableStateFlow<List<UiChatMessage>>(emptyList())
    val messages: StateFlow<List<UiChatMessage>> = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 供应商是否已配置 */
    private val _isProviderConfigured = MutableStateFlow(false)
    val isProviderConfigured: StateFlow<Boolean> = _isProviderConfigured.asStateFlow()

    private var chatClient: ChatClient? = null
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            providerRepo.configChangesFlow.collect {
                refreshProviderState()
            }
        }
    }

    fun refreshProviderState() {
        val config = buildActiveConfig()
        _isProviderConfigured.value = config != null
        // 重建 client
        chatClient?.close()
        chatClient = config?.let {
            try {
                ChatClientFactory.create(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun onPromptChange(value: String) {
        _chatPrompt.value = value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onSendClick() {
        val prompt = _chatPrompt.value.trim()
        if (prompt.isBlank() || _isChatRunning.value) return

        val client = chatClient
        if (client == null) {
            _errorMessage.value = "请先配置供应商和 API Key"
            return
        }

        _chatPrompt.value = ""
        _isChatRunning.value = true
        _errorMessage.value = null

        // 添加用户消息
        val userMsg = UiChatMessage(role = "user", content = prompt)
        _messages.value = _messages.value + userMsg

        // 添加空的 assistant 占位消息
        val assistantPlaceholder = UiChatMessage(role = "assistant", content = "", isStreaming = true)
        _messages.value = _messages.value + assistantPlaceholder

        val model = providerRepo.getModel(providerRepo.getActiveProviderId() ?: "")
        val history = buildApiMessages()

        streamJob = viewModelScope.launch {
            try {
                val result = client.chatStreamWithUsage(
                    messages = history,
                    model = model,
                    onChunk = { chunk ->
                        // 实时追加到最后一条 assistant 消息
                        val current = _messages.value.toMutableList()
                        val lastIndex = current.lastIndex
                        if (lastIndex >= 0 && current[lastIndex].role == "assistant") {
                            current[lastIndex] = current[lastIndex].copy(
                                content = current[lastIndex].content + chunk,
                            )
                            _messages.value = current
                        }
                    },
                )
                // 流结束，标记为非 streaming
                val current = _messages.value.toMutableList()
                val lastIndex = current.lastIndex
                if (lastIndex >= 0 && current[lastIndex].role == "assistant") {
                    current[lastIndex] = current[lastIndex].copy(isStreaming = false)
                    _messages.value = current
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "请求失败"
                // 移除空的 assistant 占位
                val current = _messages.value.toMutableList()
                if (current.isNotEmpty() && current.last().role == "assistant" && current.last().content.isEmpty()) {
                    current.removeAt(current.lastIndex)
                    _messages.value = current
                }
            } finally {
                _isChatRunning.value = false
            }
        }
    }

    /**
     * 将 UI 消息转换为 API 消息（不含最后的空 assistant 占位）
     */
    private fun buildApiMessages(): List<ChatMessage> {
        return _messages.value
            .filter { it.content.isNotEmpty() }
            .map { msg ->
                when (msg.role) {
                    "user" -> ChatMessage.user(msg.content)
                    "assistant" -> ChatMessage.assistant(msg.content)
                    "system" -> ChatMessage.system(msg.content)
                    else -> ChatMessage.user(msg.content)
                }
            }
    }

    /**
     * 构建当前激活供应商的 ChatClientConfig。
     * 从 core:data 读取基础字符串配置，在这里组装为特定于大模型的配置类。
     */
    private fun buildActiveConfig(): ChatClientConfig? {
        val id = providerRepo.getActiveProviderId() ?: return null
        val preset = ChatProviderRepository.PRESETS.find { it.id == id } ?: return null
        val apiKey = providerRepo.getApiKey(id)
        if (apiKey.isBlank()) return null

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

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        chatClient?.close()
    }
}
