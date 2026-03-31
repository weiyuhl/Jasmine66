package com.lhzkml.jasmine.core.data.model

/**
 * core:data 层的聊天消息抽象。
 * 与 jasmine-core 的 ChatMessage 完全解耦，feature 层只使用此类。
 */
data class SimpleChatMessage(
    val role: String,
    val content: String,
)
