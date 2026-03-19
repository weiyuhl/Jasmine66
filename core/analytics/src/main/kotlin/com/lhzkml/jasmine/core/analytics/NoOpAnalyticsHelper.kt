
package com.lhzkml.jasmine.core.analytics

/**
 * Implementation of JasmineAnalyticsHelper which does nothing. Useful for tests and previews.
 */
class NoOpJasmineAnalyticsHelper : JasmineAnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) = Unit
}

