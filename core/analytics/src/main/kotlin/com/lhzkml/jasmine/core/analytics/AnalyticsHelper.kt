
package com.lhzkml.jasmine.core.analytics

/**
 * Interface for logging analytics events. See `FirebaseJasmineAnalyticsHelper` and
 * `StubJasmineAnalyticsHelper` for implementations.
 */
interface JasmineAnalyticsHelper {
    fun logEvent(event: AnalyticsEvent)
}

