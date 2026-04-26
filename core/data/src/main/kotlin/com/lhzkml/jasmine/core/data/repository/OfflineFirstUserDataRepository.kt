package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper
import com.lhzkml.jasmine.core.datastore.JasminePreferencesDataSource
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.ThemeBrand
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class OfflineFirstUserDataRepository @Inject constructor(
    private val JasminePreferencesDataSource: JasminePreferencesDataSource,
    private val JasmineAnalyticsHelper: JasmineAnalyticsHelper,
) : UserDataRepository {

    override val userData: Flow<UserData> =
        JasminePreferencesDataSource.userData

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        JasminePreferencesDataSource.setDarkThemeConfig(darkThemeConfig)
        JasmineAnalyticsHelper.logDarkThemeConfigChanged(darkThemeConfig.name)
    }

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        JasminePreferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
        JasmineAnalyticsHelper.logOnboardingStateChanged(shouldHideOnboarding)
    }

    override suspend fun setUiEnabled(uiEnabled: Boolean) {
        JasminePreferencesDataSource.setUiEnabled(uiEnabled)
    }

    override suspend fun setWebSearchEnabled(webSearchEnabled: Boolean) {
        JasminePreferencesDataSource.setWebSearchEnabled(webSearchEnabled)
        JasmineAnalyticsHelper.logWebSearchEnabledChanged(webSearchEnabled)
    }
}
