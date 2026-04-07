package com.lhzkml.jasmine.feature.chat.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.jasmine.core.designsystem.component.MarkdownText
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhzkml.jasmine.core.designsystem.theme.customColors
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

    val onUiCallback: (String, Map<String, String>) -> Unit = { event, data ->
        viewModel.onUiCallback(event, data)
    }

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

    // Jasmine Headless WebView JS Sandbox
    var sandboxWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    Box(modifier = Modifier.size(0.dp)) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    sandboxWebView = this
                }
            }
        )
    }

    LaunchedEffect(viewModel.agentEventBus) {
        viewModel.agentEventBus.jsEvents.collect { event ->
            // Update the JS interface per-event to capture the correct continuation
            sandboxWebView?.addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onResultReady(result: String) {
                    if (event.continuation.isActive) {
                        event.continuation.resumeWith(Result.success(result))
                    }
                }
            }, "AiEdgeGallery")

            sandboxWebView?.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val script = """
                        (async function() {
                            var startTs = Date.now();
                            while(true) {
                                if (typeof ai_edge_gallery_get_result === 'function') {
                                    break;
                                }
                                await new Promise(resolve=>{ setTimeout(resolve, 100) });
                                if (Date.now() - startTs > 10000) {
                                    break;
                                }
                            }
                            var result = await ai_edge_gallery_get_result(`${event.data}`, `${event.secret}`);
                            AiEdgeGallery.onResultReady(result);
                        })()
                    """.trimIndent()
                    sandboxWebView?.evaluateJavascript(script, null)
                }
            }
            sandboxWebView?.loadUrl(event.url)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(bottom = extraPaddingDp)) {
        // 消息区域
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyStateView()
            } else {
                MessageList(messages = messages, toolCallEvents = toolCallEvents, onUiCallback = onUiCallback)
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
private fun MessageList(messages: List<UiChatMessage>, toolCallEvents: List<ToolCallEvent>, onUiCallback: ((String, Map<String, String>) -> Unit)? = null) {
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
            ChatBubble(message = msg, onUiCallback = onUiCallback)
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
private fun ChatBubble(message: UiChatMessage, onUiCallback: ((String, Map<String, String>) -> Unit)? = null) {
    val isUser = message.role == "user"
    val colorScheme = MaterialTheme.colorScheme
    val custom = MaterialTheme.customColors

    // Bubble colors
    val bubbleColor = if (isUser) custom.userBubbleBgColor else custom.agentBubbleBgColor
    val textColor = if (isUser) Color.White else colorScheme.onSurface
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )

    val baseModifier = if (isUser) {
        Modifier
            .widthIn(max = 300.dp)
            .background(bubbleColor, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = baseModifier,
        ) {
            // 思考过程折叠区域
            if (!isUser && !message.thinking.isNullOrBlank()) {
                var isThinkingExpanded by remember { mutableStateOf(false) }

                // Auto-expand while thinking is in progress
                if (message.isStreaming && message.content.isEmpty()) {
                    isThinkingExpanded = true
                }

                Row(
                    modifier = Modifier
                        .clickable { isThinkingExpanded = !isThinkingExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "💭 思考过程",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (isThinkingExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = if (isThinkingExpanded) "收起" else "展开",
                        tint = colorScheme.onSurfaceVariant,
                    )
                }

                // Collapsible with left border line
                AnimatedVisibility(
                    visible = isThinkingExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    val lineColor = colorScheme.outlineVariant
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
                            .drawBehind {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = message.thinking ?: "",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                        )
                    }
                }

                // 分隔线
                if (message.content.isNotEmpty()) {
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
                if (message.role == "assistant" && !message.isStreaming && com.lhzkml.jasmine.core.prompt.ui.KaiUiParser.containsUiBlocks(displayText)) {
                    val segments = remember(displayText) { com.lhzkml.jasmine.core.prompt.ui.KaiUiParser.parse(displayText) }
                    Column {
                        for (segment in segments) {
                            when (segment) {
                                is com.lhzkml.jasmine.core.prompt.ui.KaiUiParser.MarkdownSegment -> {
                                    MarkdownText(
                                        text = segment.content,
                                        textColor = textColor,
                                    )
                                }
                                is com.lhzkml.jasmine.core.prompt.ui.KaiUiParser.UiSegment -> {
                                    com.lhzkml.jasmine.core.prompt.ui.KaiUiRenderer(
                                        node = segment.node,
                                        isInteractive = true,
                                        onCallback = onUiCallback ?: { _, _ -> },
                                        wrapInCard = true,
                                    )
                                }
                                is com.lhzkml.jasmine.core.prompt.ui.KaiUiParser.ErrorSegment -> {
                                    Text(
                                        text = segment.rawJson,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = colorScheme.error,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    MarkdownText(
                        text = displayText,
                        textColor = textColor,
                    )
                }
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (event.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = event.toolName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (event.result != null) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
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
    val composerShape = RoundedCornerShape(24.dp)
    val sendEnabled = enabled && value.isNotBlank()

    val colorScheme = MaterialTheme.colorScheme
    val custom = MaterialTheme.customColors

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
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colorScheme.primary,
                    strokeWidth = 2.dp
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
                        containerColor = if (sendEnabled) custom.userBubbleBgColor else colorScheme.surfaceVariant,
                        disabledContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(16.dp).offset(x = 2.dp),
                        tint = if (sendEnabled) Color.White else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
