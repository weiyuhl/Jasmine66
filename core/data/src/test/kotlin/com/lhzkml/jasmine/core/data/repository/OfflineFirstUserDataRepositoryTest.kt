
package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.analytics.NoOpJasmineAnalyticsHelper
import com.lhzkml.jasmine.core.datastore.JasminePreferencesDataSource
import com.lhzkml.jasmine.core.datastore.UserPreferences
import com.lhzkml.jasmine.core.datastore.test.InMemoryDataStore
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.ThemeBrand
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineFirstUserDataRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var subject: OfflineFirstUserDataRepository

    private lateinit var JasminePreferencesDataSource: JasminePreferencesDataSource

    private val JasmineAnalyticsHelper = NoOpJasmineAnalyticsHelper()

    @Before
    fun setup() {
        JasminePreferencesDataSource = JasminePreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))

        subject = OfflineFirstUserDataRepository(
            JasminePreferencesDataSource = JasminePreferencesDataSource,
            JasmineAnalyticsHelper,
        )
    }

    @Test
    fun offlineFirstUserDataRepository_default_user_data_is_correct() =
        testScope.runTest {
            assertEquals(
                UserData(
                    followedTopics = emptySet(),
                    themeBrand = ThemeBrand.DEFAULT,
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    useDynamicColor = false,
                    shouldHideOnboarding = false,
                ),
                subject.userData.first(),
            )
        }

    @Test
    fun offlineFirstUserDataRepository_toggle_followed_topics_logic_delegates_to__preferences() =
        testScope.runTest {
            subject.setTopicIdFollowed(followedTopicId = "0", followed = true)

            assertEquals(
                setOf("0"),
                subject.userData
                    .map { it.followedTopics }
                    .first(),
            )

            subject.setTopicIdFollowed(followedTopicId = "1", followed = true)

            assertEquals(
                setOf("0", "1"),
                subject.userData
                    .map { it.followedTopics }
                    .first(),
            )

            assertEquals(
                JasminePreferencesDataSource.userData
                    .map { it.followedTopics }
                    .first(),
                subject.userData
                    .map { it.followedTopics }
                    .first(),
            )
        }

    @Test
    fun offlineFirstUserDataRepository_set_followed_topics_logic_delegates_to__preferences() =
        testScope.runTest {
            subject.setFollowedTopicIds(followedTopicIds = setOf("1", "2"))

            assertEquals(
                setOf("1", "2"),
                subject.userData
                    .map { it.followedTopics }
                    .first(),
            )

            assertEquals(
                JasminePreferencesDataSource.userData
                    .map { it.followedTopics }
                    .first(),
                subject.userData
                    .map { it.followedTopics }
                    .first(),
            )
        }


    @Test
    fun offlineFirstUserDataRepository_set_theme_brand_delegates_to__preferences() =
        testScope.runTest {
            subject.setThemeBrand(ThemeBrand.ANDROID)

            assertEquals(
                ThemeBrand.ANDROID,
                subject.userData
                    .map { it.themeBrand }
                    .first(),
            )
            assertEquals(
                ThemeBrand.ANDROID,
                JasminePreferencesDataSource
                    .userData
                    .map { it.themeBrand }
                    .first(),
            )
        }

    @Test
    fun offlineFirstUserDataRepository_set_dynamic_color_delegates_to__preferences() =
        testScope.runTest {
            subject.setDynamicColorPreference(true)

            assertEquals(
                true,
                subject.userData
                    .map { it.useDynamicColor }
                    .first(),
            )
            assertEquals(
                true,
                JasminePreferencesDataSource
                    .userData
                    .map { it.useDynamicColor }
                    .first(),
            )
        }

    @Test
    fun offlineFirstUserDataRepository_set_dark_theme_config_delegates_to__preferences() =
        testScope.runTest {
            subject.setDarkThemeConfig(DarkThemeConfig.DARK)

            assertEquals(
                DarkThemeConfig.DARK,
                subject.userData
                    .map { it.darkThemeConfig }
                    .first(),
            )
            assertEquals(
                DarkThemeConfig.DARK,
                JasminePreferencesDataSource
                    .userData
                    .map { it.darkThemeConfig }
                    .first(),
            )
        }

    @Test
    fun whenUserCompletesOnboarding_thenRemovesAllInterests_shouldHideOnboardingIsFalse() =
        testScope.runTest {
            subject.setFollowedTopicIds(setOf("1"))
            subject.setShouldHideOnboarding(true)
            assertTrue(subject.userData.first().shouldHideOnboarding)

            subject.setFollowedTopicIds(emptySet())
            assertFalse(subject.userData.first().shouldHideOnboarding)
        }
}


