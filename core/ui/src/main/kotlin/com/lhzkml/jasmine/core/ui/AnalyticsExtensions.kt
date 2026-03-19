
package com.lhzkml.jasmine.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Param
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.ParamKeys
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Types
import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper
import com.lhzkml.jasmine.core.analytics.LocalJasmineAnalyticsHelper

/**
 * Classes and functions associated with analytics events for the UI.
 */
fun JasmineAnalyticsHelper.logScreenView(screenName: String) {
    logEvent(
        AnalyticsEvent(
            type = Types.SCREEN_VIEW,
            extras = listOf(
                Param(ParamKeys.SCREEN_NAME, screenName),
            ),
        ),
    )
}

/**
 * A side-effect which records a screen view event.
 */
@Composable
fun TrackScreenViewEvent(
    screenName: String,
    JasmineAnalyticsHelper: JasmineAnalyticsHelper = LocalJasmineAnalyticsHelper.current,
) = DisposableEffect(Unit) {
    JasmineAnalyticsHelper.logScreenView(screenName)
    onDispose {}
}

