package com.lhzkml.jasmine.feature.settings.impl

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.DARK
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.FOLLOW_SYSTEM
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig.LIGHT
import com.lhzkml.jasmine.core.model.data.ThemeBrand
import com.lhzkml.jasmine.core.model.data.ThemeBrand.ANDROID
import com.lhzkml.jasmine.core.model.data.ThemeBrand.DEFAULT
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.core.navigation.SettingsNavKey
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent
import com.lhzkml.jasmine.feature.settings.impl.R.string

/**
 * 设置页面的子页面枚举
 */
private enum class SettingsSubPage {
    NONE,
    BASIC_SETTINGS,
    PROVIDER_CONFIG,
}

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.NONE) }

    BackHandler(enabled = currentSubPage != SettingsSubPage.NONE) {
        currentSubPage = SettingsSubPage.NONE
    }

    when (currentSubPage) {
        SettingsSubPage.NONE -> {
            // 设置入口列表
            SettingsMenuScreen(
                onBackClick = onBackClick,
                onBasicSettingsClick = { currentSubPage = SettingsSubPage.BASIC_SETTINGS },
                onProviderConfigClick = { currentSubPage = SettingsSubPage.PROVIDER_CONFIG },
            )
        }
        SettingsSubPage.BASIC_SETTINGS -> {
            // 基本设置子页面
            BasicSettingsScreen(
                settingsUiState = settingsUiState,
                viewModel = viewModel,
                onBackClick = { currentSubPage = SettingsSubPage.NONE },
            )
        }
        SettingsSubPage.PROVIDER_CONFIG -> {
            // 大模型供应商配置子页面
            ProviderConfigScreen(
                providerRepo = viewModel.providerRepo,
                onBackClick = { currentSubPage = SettingsSubPage.NONE },
            )
        }
    }
}

// ==================== 设置入口列表 ====================

@Composable
private fun SettingsMenuScreen(
    onBackClick: () -> Unit,
    onBasicSettingsClick: () -> Unit,
    onProviderConfigClick: () -> Unit,
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

            // 基本设置
            SettingsMenuItem(
                title = "基本设置",
                subtitle = "主题、动态色彩、深色模式",
                onClick = onBasicSettingsClick,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 大模型配置
            SettingsMenuItem(
                title = stringResource(string.feature_settings_impl_provider_config),
                subtitle = "API Key 与供应商选择",
                onClick = onProviderConfigClick,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 开源许可
            SettingsMenuItem(
                title = stringResource(string.feature_settings_impl_licenses),
                subtitle = "查看第三方开源许可证",
                onClick = {
                    context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                },
            )
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
                        onChangeThemeBrand = viewModel::updateThemeBrand,
                        onChangeDarkThemeConfig = viewModel::updateDarkThemeConfig,
                    )
                }
            }
        }
    }
}

// ==================== 设置面板（保留原有逻辑） ====================

@Composable
internal fun ColumnScope.BasicSettingsPanel(
    settings: UserEditableSettings,
    onChangeThemeBrand: (themeBrand: ThemeBrand) -> Unit,
    onChangeDarkThemeConfig: (darkThemeConfig: DarkThemeConfig) -> Unit,
) {
    SettingsDialogSectionTitle(text = stringResource(string.feature_settings_impl_theme))
    Column(Modifier.selectableGroup()) {
        SettingsDialogThemeChooserRow(
            text = stringResource(string.feature_settings_impl_brand_default),
            selected = settings.brand == DEFAULT,
            onClick = { onChangeThemeBrand(DEFAULT) },
        )
        SettingsDialogThemeChooserRow(
            text = stringResource(string.feature_settings_impl_brand_android),
            selected = settings.brand == ANDROID,
            onClick = { onChangeThemeBrand(ANDROID) },
        )
    }
    SettingsDialogSectionTitle(text = stringResource(string.feature_settings_impl_dark_mode_preference))
    Column(Modifier.selectableGroup()) {
        SettingsDialogThemeChooserRow(
            text = stringResource(string.feature_settings_impl_dark_mode_config_system_default),
            selected = settings.darkThemeConfig == FOLLOW_SYSTEM,
            onClick = { onChangeDarkThemeConfig(FOLLOW_SYSTEM) },
        )
        SettingsDialogThemeChooserRow(
            text = stringResource(string.feature_settings_impl_dark_mode_config_light),
            selected = settings.darkThemeConfig == LIGHT,
            onClick = { onChangeDarkThemeConfig(LIGHT) },
        )
        SettingsDialogThemeChooserRow(
            text = stringResource(string.feature_settings_impl_dark_mode_config_dark),
            selected = settings.darkThemeConfig == DARK,
            onClick = { onChangeDarkThemeConfig(DARK) },
        )
    }
}

@Composable
internal fun SettingsDialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsDialogThemeChooserRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(onBackClick = { navigator.goBack() })
    }
}
