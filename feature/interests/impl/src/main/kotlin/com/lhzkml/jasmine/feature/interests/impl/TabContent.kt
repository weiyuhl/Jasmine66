
package com.lhzkml.jasmine.feature.interests.impl

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.DraggableScrollbar
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.rememberDraggableScroller
import com.lhzkml.jasmine.core.designsystem.component.scrollbar.scrollbarState
import com.lhzkml.jasmine.core.model.data.FollowableTopic
import com.lhzkml.jasmine.core.ui.InterestsItem

@Composable
fun TopicsTabContent(
    topics: List<FollowableTopic>,
    onTopicClick: (String) -> Unit,
    onFollowButtonClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    withBottomSpacer: Boolean = true,
    selectedTopicId: String? = null,
    shouldHighlightSelectedTopic: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val scrollableState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag(LIST_PANE_TEST_TAG),
            contentPadding = PaddingValues(vertical = 16.dp),
            state = scrollableState,
        ) {
            // Topics listing removed centrally

            if (withBottomSpacer) {
                item {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }
        }
        val scrollbarState = scrollableState.scrollbarState(
            itemsAvailable = topics.size,
        )
        scrollableState.DraggableScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 2.dp)
                .align(Alignment.CenterEnd),
            state = scrollbarState,
            orientation = Orientation.Vertical,
            onThumbMoved = scrollableState.rememberDraggableScroller(
                itemsAvailable = topics.size,
            ),
        )
    }
}

val LIST_PANE_TEST_TAG = "interests:topics"

