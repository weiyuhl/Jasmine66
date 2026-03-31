package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.analytics.AnalyticsEvent
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Param
import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper

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

internal fun JasmineAnalyticsHelper.logOnboardingStateChanged(shouldHideOnboarding: Boolean) {
    val eventType = if (shouldHideOnboarding) "onboarding_complete" else "onboarding_reset"
    logEvent(
        AnalyticsEvent(type = eventType),
    )
}
