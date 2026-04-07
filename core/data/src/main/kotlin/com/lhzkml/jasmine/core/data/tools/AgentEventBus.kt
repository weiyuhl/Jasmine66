package com.lhzkml.jasmine.core.data.tools

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation

data class CallJsEvent(
    val url: String,
    val data: String,
    val secret: String,
    val continuation: Continuation<String>
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
        _jsEvents.tryEmit(event)
    }
}
