package com.lhzkml.jasmine.core.data.tools

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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
 *
 * 使用 Channel 替代 SharedFlow：
 * - Channel 提供背压（缓冲区满时挂起而非丢弃）
 * - 事件按序处理，避免 CancellableContinuation 跨线程竞态
 * - 缓冲区容量 64，足够应对突发流量
 */
@Singleton
class AgentEventBus @Inject constructor() {
    private val _jsEvents = Channel<CallJsEvent>(capacity = 64)
    val jsEvents = _jsEvents.receiveAsFlow()

    fun emitJsEvent(event: CallJsEvent) {
        // trySend 非挂起版本，缓冲区满时返回失败并 resume continuation
        val result = _jsEvents.trySend(event)
        if (result.isFailure) {
            android.util.Log.w("AgentEventBus", "JS event buffer full, url=${event.url}")
            if (event.continuation.isActive) {
                event.continuation.resumeWith(
                    Result.failure(IllegalStateException("JS event buffer full, cannot process skill: ${event.url}"))
                )
            }
        }
    }
}
