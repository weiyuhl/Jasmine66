package com.lhzkml.jasmine.core.data.model

/**
 * core:data 层的采样参数抽象。
 * 与 jasmine-core 的 SamplingParams 完全解耦。
 */
data class ChatSamplingParams(
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,
)
