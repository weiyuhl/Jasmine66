package com.lhzkml.jasmine.feature.sandbox.impl.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SandboxScreen() {
    val viewModel: SandboxViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTerminal by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linux 沙盒") },
                actions = {
                    if (state.sandboxReady) {
                        IconButton(onClick = { showTerminal = true }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "打开终端",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alpine Linux",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.sandboxReady) {
                                Text(
                                    text = "磁盘占用: ${state.sandboxDiskUsageMB} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.sandboxPackagesInstalled) {
                                Text(
                                    text = "已安装额外包 (bash, curl, git, python3, nodejs 等)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    if (state.isWorking) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = state.sandboxStatusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { viewModel.onCancelSandbox() },
                            ) {
                                Text("取消")
                            }
                        }
                    }

                    if (state.hasError) {
                        Text(
                            text = state.sandboxStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.sandboxReady && !state.sandboxPackagesInstalled && !state.isWorking) {
                            OutlinedButton(onClick = { viewModel.onInstallPackages() }) {
                                Text("安装常用包")
                            }
                        }
                        if (state.sandboxInstalled || state.isWorking) {
                            OutlinedButton(onClick = { showResetDialog = true }) {
                                Text("重置")
                            }
                        }
                        if (!state.sandboxInstalled && !state.isWorking) {
                            OutlinedButton(onClick = { viewModel.onSetupSandbox() }) {
                                Text("安装沙盒")
                            }
                        }
                    }
                }
            }

            if (state.sandboxReady) {
                TerminalContent(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置沙盒") },
            text = { Text("这将删除整个 Linux 沙盒环境，确定继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.onResetSandbox()
                    },
                ) {
                    Text("重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showTerminal) {
        TerminalSheet(
            viewModel = viewModel,
            onDismiss = { showTerminal = false },
        )
    }
}

@Composable
private fun TerminalContent(
    viewModel: SandboxViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val outputLines = remember { mutableStateListOf<TerminalLine>() }
    var inputText by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val terminalBg = Color(0xFF1E1E1E)
    val terminalInputBg = Color(0xFF252525)
    val terminalText = Color(0xFFD4D4D4)
    val terminalPrompt = Color(0xFF6CB6FF)
    val terminalError = Color(0xFFF48771)
    val terminalDim = Color(0xFF666666)

    val canSubmit = inputText.isNotBlank() && !isRunning

    Column(
        modifier = modifier
            .background(terminalBg)
            .imePadding(),
    ) {
        SelectionContainer(
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (outputLines.isEmpty()) {
                    Text(
                        text = "输入命令并按回车执行。\n输入 'clear' 清屏。",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = terminalDim,
                        ),
                    )
                }
                outputLines.forEach { line ->
                    when (line) {
                        is TerminalLine.Command -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$ ${line.text}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = terminalPrompt,
                                ),
                            )
                        }

                        is TerminalLine.Output -> {
                            Text(
                                text = parseAnsiToAnnotatedString(line.text, terminalText),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                ),
                            )
                        }

                        is TerminalLine.Error -> {
                            Text(
                                text = parseAnsiToAnnotatedString(line.text, terminalError),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                ),
                            )
                        }
                    }
                }
                if (isRunning) {
                    Spacer(Modifier.height(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = terminalPrompt,
                    )
                }
            }
        }

        LaunchedEffect(outputLines.size, isRunning) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        androidx.compose.material3.HorizontalDivider(
            color = terminalDim.copy(alpha = 0.2f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = terminalPrompt,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                enabled = !isRunning,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = terminalText,
                ),
                placeholder = {
                    Text(
                        text = "输入命令...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = terminalDim,
                        ),
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = terminalPrompt,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (canSubmit) {
                            val cmd = inputText.trim()
                            inputText = ""
                            scope.launch {
                                runCommand(cmd, outputLines, viewModel) { isRunning = it }
                            }
                        }
                    },
                ),
                singleLine = true,
            )
            IconButton(
                onClick = {
                    if (canSubmit) {
                        val cmd = inputText.trim()
                        inputText = ""
                        scope.launch {
                            runCommand(cmd, outputLines, viewModel) { isRunning = it }
                        }
                    }
                },
                enabled = canSubmit,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Run command",
                    tint = if (canSubmit) terminalPrompt else terminalDim,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalSheet(
    viewModel: SandboxViewModel,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252525),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "终端",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = Color(0xFF6CB6FF),
                    ),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Alpine Linux",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFD4D4D4).copy(alpha = 0.5f),
                    ),
                )
            }
        }
        TerminalContent(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val MAX_OUTPUT_LINES = 500

private suspend fun runCommand(
    command: String,
    outputLines: MutableList<TerminalLine>,
    viewModel: SandboxViewModel,
    setRunning: (Boolean) -> Unit,
) {
    if (command == "clear") {
        outputLines.clear()
        return
    }
    outputLines.add(TerminalLine.Command(command))
    setRunning(true)
    try {
        val result = withContext(Dispatchers.Default) {
            viewModel.executeCommand(command)
        }
        if (result.isNotEmpty()) {
            outputLines.add(TerminalLine.Output(result))
        }
    } catch (e: Exception) {
        outputLines.add(TerminalLine.Error(e.message ?: "Command failed"))
    }
    val excess = outputLines.size - MAX_OUTPUT_LINES
    if (excess > 0) {
        outputLines.subList(0, excess).clear()
    }
    setRunning(false)
}

sealed interface TerminalLine {
    data class Command(val text: String) : TerminalLine
    data class Output(val text: String) : TerminalLine
    data class Error(val text: String) : TerminalLine
}
