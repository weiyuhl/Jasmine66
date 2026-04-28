package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolResultInfo(
    val toolCallId: String,
    val toolName: String,
    val content: String,
)

data class UiChatMessage(
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val thinking: String? = null,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val toolResults: List<ToolResultInfo> = emptyList(),
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
    private var streamGeneration = 0

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
        addUserMessageAndStream(message, logTag = "UI callback") { errorMsg ->
            _errorMessage.value = errorMsg
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
        addUserMessageAndStream(prompt, logTag = "Chat request") { errorMsg ->
            val cause = exceptionCause().orElse(null)
            val isConnectionAbort = cause is java.net.SocketException ||
                errorMsg.contains("connection abort", ignoreCase = true) ||
                errorMsg.contains("Socket", ignoreCase = true)
            if (isConnectionAbort) {
                updateLastAssistant { it.copy(isStreaming = false) }
            } else {
                _errorMessage.value = errorMsg
                updateLastAssistant { it.copy(isStreaming = false) }
            }
        }
        if (_errorMessage.value?.contains("续传") == true) {
            _errorMessage.value = null
        }
    }

    private val _lastExceptionCause = kotlinx.coroutines.flow.MutableStateFlow<Throwable?>(null)
    private fun exceptionCause() = java.util.Optional.ofNullable(_lastExceptionCause.value)

    private fun addUserMessageAndStream(userContent: String, logTag: String, onError: (String) -> Unit) {
        val userMsg = UiChatMessage(role = "user", content = userContent)
        _messages.update { it + userMsg }

        val assistantPlaceholder = UiChatMessage(role = "assistant", content = "", isStreaming = true)
        _messages.update { it + assistantPlaceholder }

        val model = clientManager.getActiveModel()
        val history = buildApiMessages()

        _isChatRunning.value = true
        _errorMessage.value = null
        _toolCallEvents.value = emptyList()

        streamJob?.cancel()
        val thisGen = ++streamGeneration
        streamJob = viewModelScope.launch {
            try {
                val userData = userDataRepository.userData.first()
                val result = clientManager.streamChat(
                    messages = history,
                    model = model,
                    onChunk = { chunk ->
                        updateLastAssistant { it.copy(content = it.content + chunk) }
                    },
                    onThinking = { thinkChunk ->
                        updateLastAssistant { it.copy(thinking = (it.thinking ?: "") + thinkChunk) }
                    },
                    onToolCallStart = { toolName, toolArgs ->
                        _toolCallEvents.update { it + ToolCallEvent(toolName = toolName, arguments = toolArgs, isRunning = true) }
                    },
                    onToolCallResult = { toolName, resultContent ->
                        _toolCallEvents.update { events ->
                            val lastIdx = events.lastIndex
                            if (lastIdx >= 0 && events[lastIdx].toolName == toolName && events[lastIdx].isRunning) {
                                events.toMutableList().apply { this[lastIdx] = this[lastIdx].copy(result = resultContent, isRunning = false) }
                            } else {
                                events + ToolCallEvent(toolName = toolName, arguments = "", result = resultContent, isRunning = false)
                            }
                        }
                    },
                    uiEnabled = userData.uiEnabled,
                    webSearchEnabled = userData.webSearchEnabled,
                )
                val toolResults = buildToolResultsFromEvents(result.toolCalls)
                updateLastAssistant {
                    it.copy(isStreaming = false, thinking = result.thinking ?: it.thinking, toolCalls = result.toolCalls, toolResults = toolResults)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMsg = e.message ?: "请求失败"
                _lastExceptionCause.value = e.cause
                onError(errorMsg)
                com.lhzkml.jasmine.core.data.log.FileLogger.logError("ChatViewModel", "$logTag failed: $errorMsg", e)
            } finally {
                if (thisGen == streamGeneration) {
                    _isChatRunning.value = false
                }
            }
        }
    }

    private fun updateLastAssistant(transform: (UiChatMessage) -> UiChatMessage) {
        _messages.update { current ->
            val lastIndex = current.lastIndex
            if (lastIndex >= 0 && current[lastIndex].role == "assistant") {
                current.toMutableList().apply {
                    this[lastIndex] = transform(this[lastIndex])
                }
            } else {
                current
            }
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

        return history.flatMap { msg ->
            val messages = mutableListOf<SimpleChatMessage>()

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

            messages.add(SimpleChatMessage(
                role = msg.role,
                content = msg.content,
                toolCalls = toolCalls,
            ))

            // Emit tool result messages so the model sees what its tools returned.
            for (tr in msg.toolResults) {
                messages.add(SimpleChatMessage(
                    role = "tool",
                    content = tr.content,
                    toolCallId = tr.toolCallId,
                    toolName = tr.toolName,
                ))
            }

            messages
        }
    }

    private fun buildToolResultsFromEvents(toolCalls: List<ToolCallInfo>): List<ToolResultInfo> {
        val events = _toolCallEvents.value
        if (events.isEmpty()) return emptyList()
        return events.filter { !it.isRunning && it.result != null }.map { event ->
            val matchingCall = toolCalls.find { it.name == event.toolName }
            ToolResultInfo(
                toolCallId = matchingCall?.id ?: "unknown_${event.toolName}",
                toolName = event.toolName,
                content = event.result ?: "",
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
