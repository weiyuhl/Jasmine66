package com.lhzkml.jasmine.core.data.tools

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CancellableContinuation
import javax.inject.Inject
import javax.inject.Singleton

data class CallJsEvent(
    val url: String,
    val data: String,
    val secret: String,
    val continuation: CancellableContinuation<String>
)

/**
 * Event bus bridging the background Agent Tools execution layer with the UI.
 * This is used specifically to fire Javascript Evaluation commands to a frontend WebView Sandbox.
 */
@Singleton
class AgentEventBus @Inject constructor() {
    private val _jsEvents = MutableSharedFlow<CallJsEvent>(extraBufferCapacity = 64)
    val jsEvents: SharedFlow<CallJsEvent> = _jsEvents.asSharedFlow()

    fun emitJsEvent(event: CallJsEvent) {
        val accepted = _jsEvents.tryEmit(event)
        if (!accepted) {
            android.util.Log.w("AgentEventBus", "Dropping JS event - buffer full (64). url=${event.url}")
            if (event.continuation.isActive) {
                event.continuation.resumeWith(
                    Result.failure(IllegalStateException("JS event buffer full, cannot process skill: ${event.url}"))
                )
            }
        }
    }
}
