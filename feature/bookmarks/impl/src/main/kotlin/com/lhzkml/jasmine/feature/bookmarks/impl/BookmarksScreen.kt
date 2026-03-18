/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhzkml.jasmine.feature.bookmarks.impl

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.core.designsystem.component.JasmineLoadingWheel
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.DraggableScrollbar
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.rememberDraggableScroller
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.scrollbarState
import com.lhzkml.jasmine.core.designsystem.theme.LocalTintTheme
import com.lhzkml.jasmine.core.designsystem.theme.NiaTheme
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent
import com.lhzkml.jasmine.core.ui.TrackScrollJank
import com.lhzkml.jasmine.feature.bookmarks.api.R

@Composable
internal fun BookmarksScreen(
    onTopicClick: (String) -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BookmarksScreen(
        uiState = uiState,
        onShowSnackbar = onShowSnackbar,
        onTopicClick = onTopicClick,
        modifier = modifier,
    )
}

/**
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun BookmarksScreen(
    uiState: BookmarksUiState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    when (uiState) {
        BookmarksUiState.Loading -> LoadingState(modifier)
        BookmarksUiState.Empty -> EmptyState(modifier)
    }

    TrackScreenViewEvent(screenName = "Saved")
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    JasmineLoadingWheel(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("bookmarks:loading"),
        contentDesc = stringResource(id = R.string.feature_bookmarks_api_loading),
    )
}


@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .testTag("bookmarks:empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconTint = LocalTintTheme.current.iconTint
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.feature_bookmarks_api_mg_empty_bookmarks),
            colorFilter = if (iconTint != Color.Unspecified) ColorFilter.tint(iconTint) else null,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_error),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun LoadingStatePreview() {
    JasmineTheme {
        LoadingState()
    }
}

@Preview
@Composable
private fun BookmarksGridPreview() {
    JasmineTheme {
        EmptyState()
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    JasmineTheme {
        EmptyState()
    }
}
