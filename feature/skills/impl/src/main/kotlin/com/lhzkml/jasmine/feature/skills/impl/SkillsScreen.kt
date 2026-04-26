package com.lhzkml.jasmine.feature.skills.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.aspectRatio
import com.lhzkml.jasmine.core.designsystem.component.MarkdownText
import com.lhzkml.jasmine.feature.skills.api.Skill
import com.lhzkml.jasmine.feature.skills.api.SkillState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SkillsRoute(
    onBackClick: () -> Unit,
    viewModel: SkillManagerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isBuiltInExpanded by remember { mutableStateOf(true) }
    var skillToShowDetails by remember { mutableStateOf<Skill?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dialog states
    var showImportDialog by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf<Skill?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Skill?>(null) }
    var importUrl by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<String?>(null) }
    var keyValue by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSkills()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "技能管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    // 搜索栏
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .height(IntrinsicSize.Min),
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            placeholder = { Text("搜索技能") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        )

                        // 添加技能按钮
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .clickable { showImportDialog = true }
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "添加技能",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    // 全选/取消全选
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "共 ${uiState.skills.size} 个技能",
                            style = MaterialTheme.typography.labelLarge,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { viewModel.setAllSkillsSelected(selected = true) }
                            ) {
                                Text("全部启用")
                            }
                            TextButton(
                                onClick = { viewModel.setAllSkillsSelected(selected = false) }
                            ) {
                                Text("全部禁用")
                            }
                        }
                    }

                    // 技能列表
                    val filteredSkills = if (searchQuery.isBlank()) {
                        uiState.skills
                    } else {
                        uiState.skills.filter {
                            it.skill.name.contains(searchQuery, ignoreCase = true) ||
                                    it.skill.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "built_in_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape = RoundedCornerShape(20.dp))
                                    .clickable { isBuiltInExpanded = !isBuiltInExpanded }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "内置技能",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = if (isBuiltInExpanded) {
                                        Icons.Rounded.ExpandLess
                                    } else {
                                        Icons.Rounded.ExpandMore
                                    },
                                    contentDescription = if (isBuiltInExpanded) "收起" else "展开",
                                )
                            }
                        }

                        if (isBuiltInExpanded) {
                            items(filteredSkills, key = { it.skill.name }) { skillState ->
                                SkillItemRow(
                                    skillState = skillState,
                                    onSkillEnabledChange = { newCheckedState ->
                                        viewModel.setSkillSelected(skillState, newCheckedState)
                                    },
                                    onViewClick = {
                                        skillToShowDetails = skillState.skill
                                    },
                                    onKeyClick = { showKeyDialog = skillState.skill },
                                    onDeleteClick = { showDeleteDialog = skillState.skill },
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Render BottomSheet logically tied to the Scaffold screen container level natively!
        if (skillToShowDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { skillToShowDetails = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "技能：${skillToShowDetails?.name}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = skillToShowDetails?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                MarkdownText(
                                    text = skillToShowDetails?.instructions ?: "",
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false; importUrl = ""; importResult = null },
                title = { Text("导入技能") },
                text = {
                    Column {
                        Text("输入技能 SKILL.md 文件的 URL:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importUrl,
                            onValueChange = { importUrl = it },
                            placeholder = { Text("https://example.com/SKILL.md") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (importResult != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(importResult!!, style = MaterialTheme.typography.bodySmall,
                                color = if (importResult!!.startsWith("技能已导入")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (importUrl.isNotBlank()) {
                            viewModel.importSkillFromUrl(importUrl) { success, msg ->
                                importResult = msg
                                if (success) { importUrl = "" }
                            }
                        }
                    }) { Text("导入") }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false; importUrl = ""; importResult = null }) { Text("取消") }
                },
            )
        }

        // Key Dialog
        showKeyDialog?.let { skill ->
            val existingSecret = remember(skill) { viewModel.getSecret(skill.name) }
            AlertDialog(
                onDismissRequest = { showKeyDialog = null; keyValue = "" },
                title = { Text("API 密钥 - ${skill.name}") },
                text = {
                    Column {
                        if (skill.requireSecretDescription.isNotBlank()) {
                            Text(skill.requireSecretDescription, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = keyValue.ifEmpty { existingSecret },
                            onValueChange = { keyValue = it },
                            placeholder = { Text("输入 API 密钥...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveSecret(skill.name, keyValue.ifEmpty { existingSecret })
                        showKeyDialog = null; keyValue = ""
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showKeyDialog = null; keyValue = "" }) { Text("取消") }
                },
            )
        }

        // Delete Dialog
        showDeleteDialog?.let { skill ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除技能") },
                text = { Text("确定要删除「${skill.name}」吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSkill(skill.name)
                        showDeleteDialog = null
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillItemRow(
    skillState: SkillState,
    onSkillEnabledChange: (Boolean) -> Unit,
    onViewClick: () -> Unit,
    onKeyClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val skill = skillState.skill
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                    )
                    Text(
                        text = skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Switch(
                    checked = skill.selected,
                    onCheckedChange = onSkillEnabledChange,
                )
            }

            // 操作按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                // 查看详情
                FilledTonalButton(
                    onClick = onViewClick,
                    modifier = Modifier
                        .height(32.dp)
                        .padding(end = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "详情",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                if (skill.requireSecret) {
                    FilledTonalButton(
                        onClick = onKeyClick,
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "密钥",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                if (!skill.builtIn) {
                    FilledTonalButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
