package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiChatMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val thinking: String? = null,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    // Image and Media attributes migrated natively from Gallery (IMAGE / IMAGE_WITH_HISTORY)
    val bitmaps: List<android.graphics.Bitmap> = emptyList(),
    val imageBitMaps: List<androidx.compose.ui.graphics.ImageBitmap> = emptyList(),
    val maxSize: Int = 200,
    val isImageWithHistory: Boolean = false,
    val curIteration: Int = 0,
    val totalIterations: Int = 0,
) {
    fun isHistoryRunning(): Boolean {
        return isImageWithHistory && curIteration < totalIterations - 1
    }
}

data class ToolCallEvent(
    val toolName: String,
    val arguments: String,
    val result: String? = null,
    val isRunning: Boolean = true,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val clientManager: ChatClientManager,
    private val userDataRepository: UserDataRepository,
    val agentEventBus: com.lhzkml.jasmine.core.data.tools.AgentEventBus,
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

    fun onUiCallback(event: String, data: Map<String, String>) {
        val message = if (data.isNotEmpty()) {
            val formattedData = data.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "Responded with: $formattedData"
        } else {
            "Pressed: $event"
        }
        val userMsg = UiChatMessage(role = "user", content = message)
        _messages.value = _messages.value + userMsg

        val assistantPlaceholder = UiChatMessage(role = "assistant", content = "", isStreaming = true)
        _messages.value = _messages.value + assistantPlaceholder

        val model = clientManager.getActiveModel()
        val history = buildApiMessages()

        // Match onSendClick behavior: clear stale state
        _isChatRunning.value = true
        _errorMessage.value = null
        _toolCallEvents.value = emptyList()

        streamJob = viewModelScope.launch {
            try {
                val uiEnabled = userDataRepository.userData.first().uiEnabled
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
                    uiEnabled = uiEnabled,
                )
                updateLastAssistant {
                    it.copy(
                        isStreaming = false,
                        thinking = result.thinking ?: it.thinking,
                        toolCalls = result.toolCalls,
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "请求失败"
                _errorMessage.value = errorMsg
                com.lhzkml.jasmine.core.data.log.FileLogger.logError("ChatViewModel", "UI callback failed: $errorMsg", e)
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
                val uiEnabled = userDataRepository.userData.first().uiEnabled
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
                    onToolCallStart = { toolName, toolArgs ->
                        _toolCallEvents.value = _toolCallEvents.value + ToolCallEvent(
                            toolName = toolName,
                            arguments = toolArgs,
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
                    uiEnabled = uiEnabled,
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
                val errorMsg = e.message ?: "请求失败"
                val cause = e.cause
                // When DeviceControlTool launches an external Activity (e.g. Maps),
                // Android may background the app and kill the streaming socket.
                // In this case we gracefully keep whatever partial response we got.
                val isConnectionAbort = cause is java.net.SocketException ||
                    errorMsg.contains("connection abort", ignoreCase = true) ||
                    errorMsg.contains("Socket", ignoreCase = true)
                if (isConnectionAbort) {
                    // Tool executed successfully; silently close the stream
                    updateLastAssistant { it.copy(isStreaming = false) }
                } else {
                    _errorMessage.value = errorMsg
                    val current = _messages.value.toMutableList()
                    if (current.isNotEmpty() && current.last().role == "assistant" && current.last().content.isEmpty()) {
                        current.removeAt(current.lastIndex)
                        _messages.value = current
                    }
                }
                com.lhzkml.jasmine.core.data.log.FileLogger.logError("ChatViewModel", "Chat request failed: $errorMsg\nFull exception: ${e}\nCause: ${e.cause}", e)
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
