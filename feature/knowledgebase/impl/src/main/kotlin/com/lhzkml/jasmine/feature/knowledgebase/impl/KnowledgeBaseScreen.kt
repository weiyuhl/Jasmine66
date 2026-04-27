package com.lhzkml.jasmine.feature.knowledgebase.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
@Composable
internal fun KnowledgeBaseScreen(
    modifier: Modifier = Modifier,
    viewModel: KnowledgeBaseViewModel = hiltViewModel(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        // KnowledgeBase Screen - Empty as per requirements
    }
}
