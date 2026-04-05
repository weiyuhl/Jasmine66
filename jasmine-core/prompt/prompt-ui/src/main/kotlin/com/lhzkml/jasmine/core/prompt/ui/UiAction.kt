package com.lhzkml.jasmine.core.prompt.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
sealed interface UiAction

@Serializable
@SerialName("callback")
data class CallbackAction(
    val event: String = "",
    val data: Map<String, JsonPrimitive>? = null,
    val collectFrom: List<String>? = null,
) : UiAction {
    val dataAsStrings: Map<String, String>?
        get() = data?.mapValues { (_, v) ->
            if (v.isString) v.content else v.toString()
        }
}

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
