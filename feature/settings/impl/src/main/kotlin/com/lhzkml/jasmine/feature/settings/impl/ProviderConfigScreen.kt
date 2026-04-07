package com.lhzkml.jasmine.feature.settings.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.ChatProviderRepository
import com.lhzkml.jasmine.core.data.repository.ProviderPreset
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import kotlinx.coroutines.launch

@Composable
internal fun ProviderConfigScreen(
    providerRepo: ChatProviderRepository,
    clientManager: ChatClientManager,
    onBackClick: () -> Unit,
) {
    var editingProviderId by remember { mutableStateOf<String?>(null) }
    var activeProviderId by remember { mutableStateOf(providerRepo.getActiveProviderId()) }

    LaunchedEffect(Unit) {
        providerRepo.configChangesFlow.collect {
            activeProviderId = providerRepo.getActiveProviderId()
        }
    }

    BackHandler(enabled = editingProviderId != null) {
        editingProviderId = null
    }

    AnimatedContent(targetState = editingProviderId, label = "ProviderConfigs") { targetId ->
        if (targetId == null) {
            ProviderListScreen(
                providerRepo = providerRepo,
                activeProviderId = activeProviderId,
                onBackClick = onBackClick,
                onProviderClick = { editingProviderId = it },
                onToggleActive = { newActiveId ->
                    providerRepo.setActiveProviderId(newActiveId)
                    activeProviderId = newActiveId
                }
            )
        } else {
            val preset = ChatProviderRepository.PRESETS.find { it.id == targetId }
            if (preset != null) {
                ProviderDetailScreen(
                    preset = preset,
                    providerRepo = providerRepo,
                    clientManager = clientManager,
                    onBackClick = { editingProviderId = null }
                )
            } else {
                editingProviderId = null
            }
        }
    }
}

// ==================== 供应商列表页面 ====================

@Composable
private fun ProviderListScreen(
    providerRepo: ChatProviderRepository,
    activeProviderId: String?,
    onBackClick: () -> Unit,
    onProviderClick: (String) -> Unit,
    onToggleActive: (String) -> Unit,
) {
    val presets = ChatProviderRepository.PRESETS

    Scaffold(
        topBar = {
            TopAppBar(
                titleRes = R.string.feature_settings_impl_provider_config,
                navigationIcon = JasmineIcons.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onBackClick,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = "所有供应商共享全局设置，开启开关即可切换到该模型。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
            }

            items(presets) { preset ->
                val isActive = preset.id == activeProviderId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onProviderClick(preset.id) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (providerRepo.getApiKey(preset.id).isNotBlank()) "已配置 API Key" else "未配置 API Key",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (providerRepo.getApiKey(preset.id).isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                            )
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    onToggleActive(preset.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 单个供应商详情配置 ====================

@Composable
private fun ProviderDetailScreen(
    preset: ProviderPreset,
    providerRepo: ChatProviderRepository,
    clientManager: ChatClientManager,
    onBackClick: () -> Unit,
) {
    var apiKey by remember { mutableStateOf(providerRepo.getApiKey(preset.id)) }
    var baseUrl by remember { mutableStateOf(providerRepo.getBaseUrl(preset.id)) }
    var model by remember { mutableStateOf(providerRepo.getModel(preset.id)) }
    var systemPrompt by remember { mutableStateOf(providerRepo.getSystemPrompt(preset.id)) }

    // 采样参数
    var temperature by remember { mutableFloatStateOf(providerRepo.getTemperature(preset.id)?.toFloat() ?: 1.0f) }
    var topP by remember { mutableFloatStateOf(providerRepo.getTopP(preset.id)?.toFloat() ?: 1.0f) }
    var maxTokensText by remember { mutableStateOf(providerRepo.getMaxTokens(preset.id)?.toString() ?: "") }

    // 模型列表
    var modelList by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }

    // 余额
    var balanceText by remember { mutableStateOf<String?>(null) }
    var isLoadingBalance by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                titleRes = R.string.feature_settings_impl_provider_config,
                navigationIcon = JasmineIcons.ArrowBack,
                navigationIconContentDescription = "返回",
                onNavigationClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "${preset.name} 配置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ==================== 基础配置 ====================
            SectionHeader("基础配置")

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Model（带获取列表按钮）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = {
                            model = it
                            showModelDropdown = false
                        },
                        label = { Text("模型名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )

                    // 模型下拉列表
                    DropdownMenu(
                        expanded = showModelDropdown && modelList.isNotEmpty(),
                        onDismissRequest = { showModelDropdown = false },
                    ) {
                        modelList.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, fontSize = 14.sp) },
                                onClick = {
                                    model = modelName
                                    showModelDropdown = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 获取模型列表按钮
                OutlinedButton(
                    enabled = !isLoadingModels && apiKey.isNotBlank(),
                    onClick = {
                        isLoadingModels = true
                        coroutineScope.launch {
                            val list = clientManager.listModelsFor(preset.id, apiKey, baseUrl)
                            isLoadingModels = false
                            if (list.isNotEmpty()) {
                                modelList = list
                                showModelDropdown = true
                            }
                        }
                    },
                ) {
                    Text(if (isLoadingModels) "加载中..." else "获取列表")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== System Prompt ====================
            SectionHeader("系统提示词")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "定义 AI 助手的角色和行为，留空则使用默认",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("自定义 System Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 5,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== 采样参数 ====================
            SectionHeader("采样参数")

            // Temperature
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Temperature", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = String.format("%.1f", temperature),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f,
                steps = 19,
                modifier = Modifier.fillMaxWidth(),
            )

            // Top P
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Top P", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = String.format("%.2f", topP),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = topP,
                onValueChange = { topP = it },
                valueRange = 0f..1f,
                steps = 19,
                modifier = Modifier.fillMaxWidth(),
            )

            // Max Tokens
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = maxTokensText,
                onValueChange = { maxTokensText = it.filter { c -> c.isDigit() } },
                label = { Text("Max Tokens (留空使用默认值)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ==================== 余额查询 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader("API 余额")
                OutlinedButton(
                    enabled = !isLoadingBalance && apiKey.isNotBlank(),
                    onClick = {
                        isLoadingBalance = true
                        coroutineScope.launch {
                            balanceText = clientManager.getBalanceFor(preset.id, apiKey, baseUrl) ?: "该供应商不支持余额查询"
                            isLoadingBalance = false
                        }
                    },
                ) {
                    Text(if (isLoadingBalance) "查询中..." else "查询余额")
                }
            }
            balanceText?.let { balance ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = balance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==================== 保存按钮 ====================
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                onClick = {
                    providerRepo.saveProviderConfig(preset.id, apiKey, baseUrl, model)
                    providerRepo.setSystemPrompt(preset.id, systemPrompt)
                    providerRepo.setTemperature(preset.id, temperature.toDouble())
                    providerRepo.setTopP(preset.id, topP.toDouble())
                    val maxTokens = maxTokensText.toIntOrNull()
                    providerRepo.setMaxTokens(preset.id, maxTokens)
                    onBackClick()
                }
            ) {
                Text(
                    text = "保存配置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== 辅助组件 ====================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
