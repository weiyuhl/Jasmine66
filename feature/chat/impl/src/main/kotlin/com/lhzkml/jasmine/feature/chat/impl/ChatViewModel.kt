package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiChatMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val thinking: String? = null,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)

data class ToolCallEvent(
    val toolName: String,
    val arguments: String,
    val result: String? = null,
    val isRunning: Boolean = true,
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

    private val _toolCallEvents = MutableStateFlow<List<ToolCallEvent>>(emptyList())
    val toolCallEvents: StateFlow<List<ToolCallEvent>> = _toolCallEvents.asStateFlow()

    val isProviderConfigured: StateFlow<Boolean> = clientManager.isConfigured

    val providerSetupState: StateFlow<String> = clientManager.setupState

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            clientManager.configChangesFlow.collect {
                clientManager.refreshState()
            }
        }
    }

    fun refreshProviderState() {
        viewModelScope.launch {
            clientManager.refreshState()
        }
    }

    fun onPromptChange(value: String) {
        _chatPrompt.value = value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToolCallEvents() {
        _toolCallEvents.value = emptyList()
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
        _toolCallEvents.value = emptyList()

        val userMsg = UiChatMessage(role = "user", content = prompt)
        _messages.value = _messages.value + userMsg

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
                        updateLastAssistant { it.copy(content = it.content + chunk) }
                    },
                    onThinking = { thinkChunk ->
                        updateLastAssistant {
                            it.copy(thinking = (it.thinking ?: "") + thinkChunk)
                        }
                    },
                    onResumeAttempt = { attempt ->
                        _errorMessage.value = "网络中断，正在续传 (第 $attempt 次)..."
                    },
                    onToolCallStart = { toolName, args ->
                        _toolCallEvents.value = _toolCallEvents.value + ToolCallEvent(
                            toolName = toolName,
                            arguments = args,
                            isRunning = true,
                        )
                    },
                    onToolCallResult = { toolName, resultContent ->
                        val events = _toolCallEvents.value.toMutableList()
                        val lastIdx = events.lastIndex
                        if (lastIdx >= 0 && events[lastIdx].toolName == toolName && events[lastIdx].isRunning) {
                            events[lastIdx] = events[lastIdx].copy(
                                result = resultContent,
                                isRunning = false,
                            )
                            _toolCallEvents.value = events
                        } else {
                            _toolCallEvents.value = events + ToolCallEvent(
                                toolName = toolName,
                                arguments = "",
                                result = resultContent,
                                isRunning = false,
                            )
                        }
                    },
                )
                updateLastAssistant {
                    it.copy(
                        isStreaming = false,
                        thinking = result.thinking ?: it.thinking,
                        toolCalls = result.toolCalls,
                    )
                }
                if (_errorMessage.value?.contains("续传") == true) {
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "请求失败"
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

    private fun updateLastAssistant(transform: (UiChatMessage) -> UiChatMessage) {
        val current = _messages.value.toMutableList()
        val lastIndex = current.lastIndex
        if (lastIndex >= 0 && current[lastIndex].role == "assistant") {
            current[lastIndex] = transform(current[lastIndex])
            _messages.value = current
        }
    }

    private fun buildApiMessages(): List<SimpleChatMessage> {
        val current = _messages.value
        if (current.isEmpty()) return emptyList()
        
        val history = if (current.last().role == "assistant" && current.last().content.isEmpty()) {
            current.dropLast(1)
        } else {
            current
        }
        
        return history.map { msg ->
            val toolCalls = if (msg.toolCalls.isNotEmpty()) {
                msg.toolCalls.map { tc ->
                    com.lhzkml.jasmine.core.data.model.ToolCallInfo(
                        id = tc.id,
                        name = tc.name,
                        arguments = tc.arguments,
                    )
                }
            } else {
                null
            }
            SimpleChatMessage(
                role = msg.role,
                content = msg.content,
                toolCalls = toolCalls,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
