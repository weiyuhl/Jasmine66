package com.lhzkml.jasmine.core.data.model

data class SimpleChatMessage(
    val role: String,
    val content: String,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolCalls: List<ToolCallInfo>? = null,
)

data class ToolCallInfo(
    val id: String,
    val name: String,
    val arguments: String,
)
