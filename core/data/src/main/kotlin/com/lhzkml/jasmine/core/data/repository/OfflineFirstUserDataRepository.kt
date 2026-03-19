/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhzkml.jasmine.core.data.repository

import androidx.annotation.VisibleForTesting
import com.lhzkml.jasmine.core.analytics.AnalyticsHelper
import com.lhzkml.jasmine.core.datastore.JasminePreferencesDataSource
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.ThemeBrand
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class OfflineFirstUserDataRepository @Inject constructor(
    private val JasminePreferencesDataSource: JasminePreferencesDataSource,
    private val analyticsHelper: AnalyticsHelper,
) : UserDataRepository {

    override val userData: Flow<UserData> =
        JasminePreferencesDataSource.userData

    @VisibleForTesting
    override suspend fun setFollowedTopicIds(followedTopicIds: Set<String>) =
        JasminePreferencesDataSource.setFollowedTopicIds(followedTopicIds)

    override suspend fun setTopicIdFollowed(followedTopicId: String, followed: Boolean) {
        JasminePreferencesDataSource.setTopicIdFollowed(followedTopicId, followed)
        analyticsHelper.logTopicFollowToggled(followedTopicId, followed)
    }

    override suspend fun setThemeBrand(themeBrand: ThemeBrand) {
        JasminePreferencesDataSource.setThemeBrand(themeBrand)
        analyticsHelper.logThemeChanged(themeBrand.name)
    }

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        JasminePreferencesDataSource.setDarkThemeConfig(darkThemeConfig)
        analyticsHelper.logDarkThemeConfigChanged(darkThemeConfig.name)
    }

    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        JasminePreferencesDataSource.setDynamicColorPreference(useDynamicColor)
        analyticsHelper.logDynamicColorPreferenceChanged(useDynamicColor)
    }

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        JasminePreferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
        analyticsHelper.logOnboardingStateChanged(shouldHideOnboarding)
    }
}

