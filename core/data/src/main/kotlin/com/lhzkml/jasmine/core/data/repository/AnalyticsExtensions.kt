
package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.analytics.AnalyticsEvent
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Param
import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper

internal fun JasmineAnalyticsHelper.logTopicFollowToggled(followedTopicId: String, isFollowed: Boolean) {
    val eventType = if (isFollowed) "topic_followed" else "topic_unfollowed"
    val paramKey = if (isFollowed) "followed_topic_id" else "unfollowed_topic_id"
    logEvent(
        AnalyticsEvent(
            type = eventType,
            extras = listOf(
                Param(key = paramKey, value = followedTopicId),
            ),
        ),
    )
}

internal fun JasmineAnalyticsHelper.logThemeChanged(themeName: String) =
    logEvent(
        AnalyticsEvent(
            type = "theme_changed",
            extras = listOf(
                Param(key = "theme_name", value = themeName),
            ),
        ),
    )

internal fun JasmineAnalyticsHelper.logDarkThemeConfigChanged(darkThemeConfigName: String) =
    logEvent(
        AnalyticsEvent(
            type = "dark_theme_config_changed",
            extras = listOf(
                Param(key = "dark_theme_config", value = darkThemeConfigName),
            ),
        ),
    )

internal fun JasmineAnalyticsHelper.logDynamicColorPreferenceChanged(useDynamicColor: Boolean) =
    logEvent(
        AnalyticsEvent(
            type = "dynamic_color_preference_changed",
            extras = listOf(
                Param(key = "dynamic_color_preference", value = useDynamicColor.toString()),
            ),
        ),
    )

internal fun JasmineAnalyticsHelper.logOnboardingStateChanged(shouldHideOnboarding: Boolean) {
    val eventType = if (shouldHideOnboarding) "onboarding_complete" else "onboarding_reset"
    logEvent(
        AnalyticsEvent(type = eventType),
    )
}

