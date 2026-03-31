package com.lhzkml.jasmine.feature.chat.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent
import kotlin.math.max

@Composable
internal fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val chatPrompt by viewModel.chatPrompt.collectAsStateWithLifecycle()
    val isChatRunning by viewModel.isChatRunning.collectAsStateWithLifecycle()

    TrackScreenViewEvent(screenName = "Chat")

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    // Material 3 NavigationBar has a standard height of 80.dp
    val bottomBarHeightPx = with(density) { 80.dp.roundToPx() } + navBarsBottom
    // Subtract the bottom bar height from the keyboard inset so ChatComposer rides perfectly on top
    // without floating 80dp in the air.
    val extraPaddingPx = max(0, imeBottom - bottomBarHeightPx)
    val extraPaddingDp = with(density) { extraPaddingPx.toDp() }

    Column(modifier = modifier.fillMaxSize().padding(bottom = extraPaddingDp)) {
        // Chat message area
        Box(
            modifier = Modifier.weight(1f),
        ) {
            // Chat messages will be displayed here
        }

        // Chat composer at bottom
        ChatComposer(
            value = chatPrompt,
            enabled = true,
            onValueChange = viewModel::onPromptChange,
            onSendClick = viewModel::onSendClick,
            isRunning = isChatRunning,
        )
    }
}

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isRunning: Boolean,
) {
    val composerShape = RoundedCornerShape(22.dp)
    val sendEnabled = enabled && value.isNotBlank()

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicText(
                    text = "⏳",
                    style = TextStyle(
                        color = colorScheme.primary,
                        fontSize = 12.sp,
                    ),
                )
                BasicText(
                    text = "正在生成回复...",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colorScheme.outlineVariant, composerShape)
                .background(colorScheme.surface, composerShape)
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                cursorBrush = SolidColor(colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, end = 8.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "询问任何问题",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, colorScheme.outlineVariant, CircleShape)
                        .background(colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "+",
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                IconButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier
                        .size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (sendEnabled) colorScheme.primary else colorScheme.surfaceVariant,
                        contentColor = if (sendEnabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    ),
                ) {
                    BasicText(
                        text = "↑",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}
