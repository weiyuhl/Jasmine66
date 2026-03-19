package com.lhzkml.jasmine.feature.tools.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent

@Composable
internal fun ToolsScreen(
    modifier: Modifier = Modifier,
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    TrackScreenViewEvent(screenName = "Tools")
    Box(modifier = modifier.fillMaxSize()) {
        // Tools Screen - Empty as per requirements
    }
}
