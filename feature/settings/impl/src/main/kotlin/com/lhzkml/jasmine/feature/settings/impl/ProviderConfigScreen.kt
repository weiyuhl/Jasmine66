package com.lhzkml.jasmine.feature.settings.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.jasmine.core.data.repository.ChatProviderRepository
import com.lhzkml.jasmine.core.data.repository.ProviderPreset
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons

@Composable
internal fun ProviderConfigScreen(
    providerRepo: ChatProviderRepository,
    onBackClick: () -> Unit,
) {
    var editingProviderId by remember { mutableStateOf<String?>(null) }
    // Observe the active provider ID so UI (the switches) updates when one is activated
    var activeProviderId by remember { mutableStateOf(providerRepo.getActiveProviderId()) }

    // Re-read active provider whenever this composable compositions
    LaunchedEffect(Unit) {
        providerRepo.configChangesFlow.collect {
            activeProviderId = providerRepo.getActiveProviderId()
        }
    }

    // Capture system back gesture when editing a specific provider details
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
                    // Config flow normally triggers update, but we optimistically update UI state for snappiness
                    activeProviderId = newActiveId
                }
            )
        } else {
            val preset = ChatProviderRepository.PRESETS.find { it.id == targetId }
            if (preset != null) {
                ProviderDetailScreen(
                    preset = preset,
                    providerRepo = providerRepo,
                    onBackClick = { editingProviderId = null }
                )
            } else {
                // Fallback if ID is somehow invalid
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProviderClick(preset.id) }
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

// ==================== 单个供应商详情配置 ====================

@Composable
private fun ProviderDetailScreen(
    preset: ProviderPreset,
    providerRepo: ChatProviderRepository,
    onBackClick: () -> Unit,
) {
    var apiKey by remember { mutableStateOf(providerRepo.getApiKey(preset.id)) }
    var baseUrl by remember { mutableStateOf(providerRepo.getBaseUrl(preset.id)) }
    var model by remember { mutableStateOf(providerRepo.getModel(preset.id)) }

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

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Model
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        providerRepo.saveProviderConfig(preset.id, apiKey, baseUrl, model)
                        onBackClick() // 保存后退出详情
                    }
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "保存配置",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
