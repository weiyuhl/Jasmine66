package com.lhzkml.jasmine.feature.chat.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val chatPrompt by viewModel.chatPrompt.collectAsStateWithLifecycle()
    val isChatRunning by viewModel.isChatRunning.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isProviderConfigured by viewModel.isProviderConfigured.collectAsStateWithLifecycle()
    val providerSetupState by viewModel.providerSetupState.collectAsStateWithLifecycle()
    val toolCallEvents by viewModel.toolCallEvents.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }

    // 每次 ChatScreen 进入 Composition 时（包括从设置页返回），主动刷新供应商状态
    LaunchedEffect(Unit) {
        viewModel.refreshProviderState()
    }

    TrackScreenViewEvent(screenName = "Chat")

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomBarHeightPx = with(density) { 80.dp.roundToPx() } + navBarsBottom
    val extraPaddingPx = max(0, imeBottom - bottomBarHeightPx)
    val extraPaddingDp = with(density) { extraPaddingPx.toDp() }

    Column(modifier = modifier.fillMaxSize().padding(bottom = extraPaddingDp)) {
        // 消息区域
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyStateView()
            } else {
                MessageList(messages = messages, toolCallEvents = toolCallEvents)
            }
        }

        // 错误提示
        errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("关闭", fontSize = 12.sp)
                }
            }
        }

        // 未配置供应商时的提示
        if (!isProviderConfigured) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⚙️ $providerSetupState\n(点击上方前往设置配置)",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 输入框
        ChatComposer(
            value = chatPrompt,
            enabled = true,
            onValueChange = viewModel::onPromptChange,
            onSendClick = viewModel::onSendClick,
            onAddClick = { /* TODO: 将来在此打开工具/附件面板 */ },
            isRunning = isChatRunning,
        )
    }
}


// ==================== 消息列表 ====================

@Composable
private fun MessageList(messages: List<UiChatMessage>, toolCallEvents: List<ToolCallEvent>) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, toolCallEvents.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = false,
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(messages) { msg ->
            ChatBubble(message = msg)
        }
        if (toolCallEvents.isNotEmpty()) {
            item {
                ToolCallEventsView(events = toolCallEvents)
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChatBubble(message: UiChatMessage) {
    val isUser = message.role == "user"
    val colorScheme = MaterialTheme.colorScheme

    val bubbleColor = if (isUser) colorScheme.primary else colorScheme.surfaceVariant
    val textColor = if (isUser) colorScheme.onPrimary else colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bubbleColor, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // 思考过程折叠区域（仅 assistant 且有 thinking 内容时显示）
            if (!isUser && !message.thinking.isNullOrBlank()) {
                var isThinkingExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isThinkingExpanded = !isThinkingExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isThinkingExpanded) "▼" else "▶",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "💭 思考过程",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                    )
                    if (message.isStreaming && message.content.isEmpty()) {
                        Text(
                            text = "思考中...",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.5f),
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = isThinkingExpanded) {
                    Text(
                        text = message.thinking ?: "",
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                // 分隔线
                if (message.content.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(textColor.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 主要内容
            val displayText = if (message.content.isEmpty() && message.isStreaming) {
                if (!message.thinking.isNullOrBlank()) "" else "..."
            } else {
                message.content
            }
            if (displayText.isNotEmpty()) {
                Text(
                    text = displayText,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}



// ==================== 工具调用事件 ====================

@Composable
private fun ToolCallEventsView(events: List<ToolCallEvent>) {
    if (events.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        events.forEach { event ->
            ToolCallEventItem(event = event)
        }
    }
}

@Composable
private fun ToolCallEventItem(event: ToolCallEvent) {
    var isExpanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { if (event.result != null) isExpanded = !isExpanded }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (event.isRunning) "⏳" else "✅",
                fontSize = 12.sp,
            )
            Text(
                text = event.toolName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (event.result != null) {
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }

        if (isExpanded && event.result != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorScheme.surface)
                    .padding(8.dp),
            ) {
                Text(
                    text = event.result,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}



// ==================== 输入框 ====================

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit,
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
                        .clip(CircleShape)
                        .clickable(onClick = onAddClick)
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

// ==================== 空状态动画 ====================

@Composable
private fun EmptyStateView() {
    val context = LocalContext.current
    val jsonString = remember {
        context.assets.open("Coding_Slide.json").bufferedReader().use { it.readText() }
    }
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(jsonString)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(240.dp),
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
                speed = 0.6f
            ),
            contentDescription = null
        )
    }
}
