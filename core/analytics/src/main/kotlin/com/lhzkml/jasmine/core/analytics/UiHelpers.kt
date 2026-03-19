
package com.lhzkml.jasmine.core.analytics

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Global key used to obtain access to the JasmineAnalyticsHelper through a CompositionLocal.
 */
val LocalJasmineAnalyticsHelper = staticCompositionLocalOf<JasmineAnalyticsHelper> {
    // Provide a default JasmineAnalyticsHelper which does nothing. This is so that tests and previews
    // do not have to provide one. For real app builds provide a different implementation.
    NoOpJasmineAnalyticsHelper()
}

