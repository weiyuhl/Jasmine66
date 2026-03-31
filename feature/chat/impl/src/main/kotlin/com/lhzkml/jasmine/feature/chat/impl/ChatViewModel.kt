package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
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
    /** 思考/推理过程（DeepSeek reasoning / Claude thinking） */
    val thinking: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val clientManager: ChatClientManager,
) : ViewModel() {

    private val _chatPrompt = MutableStateFlow("")
    val chatPrompt: StateFlow<String> = _chatPrompt.asStateFlow()

    private val _isChatRunning = MutableStateFlow(false)
    val isChatRunning: StateFlow<Boolean> = _isChatRunning.asStateFlow()

    private val _messages = MutableStateFlow<List<UiChatMessage>>(emptyList())
    val messages: StateFlow<List<UiChatMessage>> = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 供应商是否已配置 — 委托给 ChatClientManager */
    val isProviderConfigured: StateFlow<Boolean> = clientManager.isConfigured

    /** 诊断信息 — 委托给 ChatClientManager */
    val providerSetupState: StateFlow<String> = clientManager.setupState

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            clientManager.configChangesFlow.collect {
                clientManager.refreshState()
            }
        }
    }

    /** 由 ChatScreen 在进入 Composition 时调用 */
    fun refreshProviderState() {
        clientManager.refreshState()
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

        if (!clientManager.isConfigured.value) {
            _errorMessage.value = "发送失败: ${clientManager.setupState.value}"
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

        val model = clientManager.getActiveModel()
        val history = buildApiMessages()

        streamJob = viewModelScope.launch {
            try {
                val result = clientManager.streamChat(
                    messages = history,
                    model = model,
                    onChunk = { chunk ->
                        // 实时追加到最后一条 assistant 消息
                        updateLastAssistant { it.copy(content = it.content + chunk) }
                    },
                    onThinking = { thinkChunk ->
                        // 实时追加思考过程到最后一条 assistant 消息
                        updateLastAssistant {
                            it.copy(thinking = (it.thinking ?: "") + thinkChunk)
                        }
                    },
                    onResumeAttempt = { attempt ->
                        _errorMessage.value = "网络中断，正在续传 (第 $attempt 次)..."
                    },
                )
                // 流结束，标记为非 streaming，写入最终思考内容
                updateLastAssistant {
                    it.copy(
                        isStreaming = false,
                        thinking = result.thinking ?: it.thinking,
                    )
                }
                // 清除续传提示
                if (_errorMessage.value?.contains("续传") == true) {
                    _errorMessage.value = null
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
     * 更新最后一条 assistant 消息
     */
    private fun updateLastAssistant(transform: (UiChatMessage) -> UiChatMessage) {
        val current = _messages.value.toMutableList()
        val lastIndex = current.lastIndex
        if (lastIndex >= 0 && current[lastIndex].role == "assistant") {
            current[lastIndex] = transform(current[lastIndex])
            _messages.value = current
        }
    }

    /**
     * 将 UI 消息转换为 core:data 层的 SimpleChatMessage（不含最后的空 assistant 占位）
     */
    private fun buildApiMessages(): List<SimpleChatMessage> {
        return _messages.value
            .filter { it.content.isNotEmpty() }
            .map { msg -> SimpleChatMessage(role = msg.role, content = msg.content) }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        clientManager.close()
    }
}
