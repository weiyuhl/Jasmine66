
package com.lhzkml.jasmine.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StubJasmineAnalyticsHelper"

/**
 * An implementation of JasmineAnalyticsHelper just writes the events to logcat. Used in builds where no
 * analytics events should be sent to a backend.
 */
@Singleton
internal class StubJasmineAnalyticsHelper @Inject constructor() : JasmineAnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) {
        Log.d(TAG, "Received analytics event: $event")
    }
}

