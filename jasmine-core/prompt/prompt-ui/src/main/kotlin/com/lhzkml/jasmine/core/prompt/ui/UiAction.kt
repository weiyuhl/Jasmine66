package com.lhzkml.jasmine.core.prompt.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UiAction

@Serializable
@SerialName("callback")
data class CallbackAction(
    val event: String = "",
    val collectFrom: List<String> = emptyList(),
) : UiAction

@Serializable
@SerialName("toggle")
data class ToggleAction(
    val targetId: String = "",
) : UiAction

@Serializable
@SerialName("open_url")
data class OpenUrlAction(
    val url: String = "",
) : UiAction
