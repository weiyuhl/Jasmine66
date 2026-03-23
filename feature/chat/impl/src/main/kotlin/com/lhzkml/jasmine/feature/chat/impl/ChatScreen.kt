package com.lhzkml.jasmine.feature.chat.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent

@Composable
internal fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Chat")
    Box(modifier = modifier.fillMaxSize()) {
        // Chat Screen - Empty as per requirements
        // Gradient background is handled by JasmineApp.kt for ChatNavKey
    }
}
