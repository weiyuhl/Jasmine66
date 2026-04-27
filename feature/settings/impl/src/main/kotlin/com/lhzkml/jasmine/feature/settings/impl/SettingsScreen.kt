package com.lhzkml.jasmine.feature.settings.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.data.log.FileLogger
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.DARK
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.FOLLOW_SYSTEM
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.LIGHT
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.core.navigation.SettingsNavKey
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent
import com.lhzkml.jasmine.feature.sandbox.api.navigation.SandboxNavKey
import com.lhzkml.jasmine.feature.settings.impl.R.string

/**
 * 设置页面的子页面枚举
 */
private enum class SettingsSubPage {
    NONE,
    BASIC_SETTINGS,
    PROVIDER_CONFIG,
    LICENSES,
}

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    navigator: Navigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.NONE) }

    BackHandler(enabled = currentSubPage != SettingsSubPage.NONE) {
        currentSubPage = SettingsSubPage.NONE
    }

    when (currentSubPage) {
        SettingsSubPage.NONE -> {
            val uiEnabled = when (val state = settingsUiState) {
                is SettingsUiState.Success -> state.settings.uiEnabled
                else -> true
            }
            val webSearchEnabled = when (val state = settingsUiState) {
                is SettingsUiState.Success -> state.settings.webSearchEnabled
                else -> true
            }
            SettingsMenuScreen(
                onBackClick = onBackClick,
                navigator = navigator,
                onBasicSettingsClick = { currentSubPage = SettingsSubPage.BASIC_SETTINGS },
                onProviderConfigClick = { currentSubPage = SettingsSubPage.PROVIDER_CONFIG },
                onSandboxClick = { navigator.navigate(SandboxNavKey) },
                onLicensesClick = { currentSubPage = SettingsSubPage.LICENSES },
                uiEnabled = uiEnabled,
                onUiToggle = { viewModel.updateUiEnabled(it) },
                webSearchEnabled = webSearchEnabled,
                onWebSearchToggle = { viewModel.updateWebSearchEnabled(it) },
            )
        }

        SettingsSubPage.BASIC_SETTINGS -> {
            BasicSettingsScreen(
                settingsUiState = settingsUiState,
                viewModel = viewModel,
                onBackClick = { currentSubPage = SettingsSubPage.NONE },
            )
        }

        SettingsSubPage.PROVIDER_CONFIG -> {
            ProviderConfigScreen(
                viewModel = viewModel,
                onBackClick = { currentSubPage = SettingsSubPage.NONE },
            )
        }

        SettingsSubPage.LICENSES -> {
            LicensesScreen(
                onBackClick = { currentSubPage = SettingsSubPage.NONE },
            )
        }
    }
}

// ==================== 设置入口列表 ====================

@Composable
private fun SettingsMenuScreen(
    onBackClick: () -> Unit,
    navigator: Navigator,
    onBasicSettingsClick: () -> Unit,
    onProviderConfigClick: () -> Unit,
    onSandboxClick: () -> Unit,
    onLicensesClick: () -> Unit,
    uiEnabled: Boolean,
    onUiToggle: (Boolean) -> Unit,
    webSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                titleRes = string.feature_settings_impl_title,
                navigationIcon = JasmineIcons.ArrowBack,
                navigationIconContentDescription = stringResource(id = string.feature_settings_impl_dismiss_dialog_button_text),
                onNavigationClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TrackScreenViewEvent(screenName = "Settings")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsMenuItem(
                        title = stringResource(string.feature_settings_impl_basic_settings),
                        subtitle = stringResource(string.feature_settings_impl_basic_settings_subtitle),
                        onClick = onBasicSettingsClick,
                    )

                    SettingsMenuItem(
                        title = stringResource(string.feature_settings_impl_provider_config),
                        subtitle = stringResource(string.feature_settings_impl_provider_config_subtitle),
                        onClick = onProviderConfigClick,
                    )

                    SettingsMenuItem(
                        title = stringResource(string.feature_settings_impl_linux_sandbox),
                        subtitle = stringResource(string.feature_settings_impl_linux_sandbox_subtitle),
                        onClick = onSandboxClick,
                    )

                    SettingsMenuItem(
                        title = stringResource(string.feature_settings_impl_export_logs),
                        subtitle = stringResource(string.feature_settings_impl_export_logs_subtitle),
                        onClick = { exportLogs(context) },
                    )

                    SettingsToggleItem(
                        title = stringResource(string.feature_settings_impl_dynamic_ui),
                        subtitle = stringResource(string.feature_settings_impl_dynamic_ui_subtitle),
                        checked = uiEnabled,
                        onCheckedChange = onUiToggle,
                    )

                    SettingsToggleItem(
                        title = stringResource(string.feature_settings_impl_web_search),
                        subtitle = if (webSearchEnabled) {
                            stringResource(string.feature_settings_impl_web_search_on)
                        } else {
                            stringResource(string.feature_settings_impl_web_search_off)
                        },
                        checked = webSearchEnabled,
                        onCheckedChange = onWebSearchToggle,
                    )

                    SettingsMenuItem(
                        title = stringResource(string.feature_settings_impl_licenses),
                        subtitle = stringResource(string.feature_settings_impl_licenses_subtitle),
                        onClick = onLicensesClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "›",
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ==================== 基本设置子页面 ====================

@Composable
private fun BasicSettingsScreen(
    settingsUiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                titleRes = string.feature_settings_impl_basic_settings,
                navigationIcon = JasmineIcons.ArrowBack,
                navigationIconContentDescription = stringResource(id = string.feature_settings_impl_dismiss_dialog_button_text),
                onNavigationClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when (val state = settingsUiState) {
                SettingsUiState.Loading -> {
                    Text(
                        text = stringResource(string.feature_settings_impl_loading),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }

                is SettingsUiState.Success -> {
                    BasicSettingsPanel(
                        settings = state.settings,
                        onChangeDarkThemeConfig = viewModel::updateDarkThemeConfig,
                    )
                }
            }
        }
    }
}

// ==================== 设置面板（保留原有逻辑） ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.BasicSettingsPanel(
    settings: UserEditableSettings,
    onChangeDarkThemeConfig: (darkThemeConfig: DarkThemeConfig) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = stringResource(string.feature_settings_impl_dark_mode_preference),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            
            val options = listOf(FOLLOW_SYSTEM, LIGHT, DARK)
            MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, theme ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onCheckedChange = { onChangeDarkThemeConfig(theme) },
                        checked = settings.darkThemeConfig == theme,
                        label = { 
                            Text(
                                when(theme) {
                                    FOLLOW_SYSTEM -> stringResource(string.feature_settings_impl_dark_mode_config_system_default)
                                    LIGHT -> stringResource(string.feature_settings_impl_dark_mode_config_light)
                                    DARK -> stringResource(string.feature_settings_impl_dark_mode_config_dark)
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(onBackClick = { navigator.goBack() }, navigator = navigator)
    }
}

private fun exportLogs(context: Context) {
    try {
        val logContent = FileLogger.getCombinedLogs()
        if (logContent.isEmpty()) {
            Toast.makeText(context, "暂无日志记录", Toast.LENGTH_SHORT).show()
            return
        }

        val logFiles = FileLogger.getLogFiles()
        if (logFiles.isNotEmpty()) {
            val latestLog = logFiles.first()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Jasmine 应用日志")
                putExtra(Intent.EXTRA_TEXT, logContent)
                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    latestLog
                ))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享日志"))
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Jasmine 应用日志")
                putExtra(Intent.EXTRA_TEXT, logContent)
            }
            context.startActivity(Intent.createChooser(intent, "分享日志"))
        }
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val logContent = FileLogger.getCombinedLogs()
        clipboard.setPrimaryClip(ClipData.newPlainText("Jasmine Logs", logContent))
        Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}
