package com.lhzkml.jasmine.core.data.model

data class StreamChatResult(
    val content: String,
    val finishReason: String? = null,
    val thinking: String? = null,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)
