package com.lhzkml.jasmine.core.data.model

/**
 * 流式聊天结果。
 * 封装 jasmine-core StreamResult 的关键字段，暴露给 feature 层使用。
 */
data class StreamChatResult(
    val content: String,
    val finishReason: String? = null,
)
