package com.lhzkml.jasmine.feature.settings.impl

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.lhzkml.jasmine.core.designsystem.component.JasmineTextButton
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.designsystem.theme.supportsDynamicTheming
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

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                titleRes = string.feature_settings_impl_title,
                navigationIcon = JasmineIcons.ArrowBack,
                navigationIconContentDescription = stringResource(id = string.feature_settings_impl_dismiss_dialog_button_text),
                onNavigationClick = onBackClick,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            when (val state = settingsUiState) {
                SettingsUiState.Loading -> {
                    Text(
                        text = stringResource(string.feature_settings_impl_loading),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }

                is SettingsUiState.Success -> {
                    SettingsPanel(
                        settings = state.settings,
                        supportDynamicColor = supportsDynamicTheming(),
                        onChangeThemeBrand = viewModel::updateThemeBrand,
                        onChangeDynamicColorPreference = viewModel::updateDynamicColorPreference,
                        onChangeDarkThemeConfig = viewModel::updateDarkThemeConfig,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            LinksPanel()
            TrackScreenViewEvent(screenName = "Settings")
        }
    }
}

// [ColumnScope] is used for using the [ColumnScope.AnimatedVisibility] extension overload composable.
@Composable
internal fun ColumnScope.SettingsPanel(
    settings: UserEditableSettings,
    supportDynamicColor: Boolean,
    onChangeThemeBrand: (themeBrand: ThemeBrand) -> Unit,
    onChangeDynamicColorPreference: (useDynamicColor: Boolean) -> Unit,
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
    AnimatedVisibility(visible = settings.brand == DEFAULT && supportDynamicColor) {
        Column {
            SettingsDialogSectionTitle(text = stringResource(string.feature_settings_impl_dynamic_color_preference))
            Column(Modifier.selectableGroup()) {
                SettingsDialogThemeChooserRow(
                    text = stringResource(string.feature_settings_impl_dynamic_color_yes),
                    selected = settings.useDynamicColor,
                    onClick = { onChangeDynamicColorPreference(true) },
                )
                SettingsDialogThemeChooserRow(
                    text = stringResource(string.feature_settings_impl_dynamic_color_no),
                    selected = !settings.useDynamicColor,
                    onClick = { onChangeDynamicColorPreference(false) },
                )
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LinksPanel() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val context = LocalContext.current
        JasmineTextButton(
            onClick = {
                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            },
        ) {
            Text(text = stringResource(string.feature_settings_impl_licenses))
        }
    }
}

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(onBackClick = { navigator.goBack() })
    }
}
