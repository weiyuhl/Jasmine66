package com.lhzkml.jasmine.feature.chat.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _chatPrompt = MutableStateFlow("")
    val chatPrompt: StateFlow<String> = _chatPrompt.asStateFlow()

    private val _isChatRunning = MutableStateFlow(false)
    val isChatRunning: StateFlow<Boolean> = _isChatRunning.asStateFlow()

    fun onPromptChange(value: String) {
        _chatPrompt.value = value
    }

    fun onSendClick() {
        if (_chatPrompt.value.isBlank() || _isChatRunning.value) return
        // TODO: Implement actual chat send logic
        _chatPrompt.value = ""
    }
}
