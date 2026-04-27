package com.lhzkml.jasmine.core.websearch.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DuckDuckGoResponse(
    val AbstractText: String = "",
    val AbstractSource: String = "",
    val AbstractURL: String = "",
    val Heading: String = "",
    val Answer: String = "",
    val AnswerType: String = "",
    val Definition: String = "",
    val DefinitionSource: String = "",
    val DefinitionURL: String = "",
    val Results: List<DuckDuckGoResultItem> = emptyList(),
    val RelatedTopics: List<DuckDuckGoRelatedTopic> = emptyList(),
)

@Serializable
data class DuckDuckGoResultItem(
    val Text: String = "",
    val FirstURL: String = "",
    val Icon: DuckDuckGoIcon? = null,
)

@Serializable
data class DuckDuckGoIcon(
    val URL: String = "",
)

@Serializable
data class DuckDuckGoRelatedTopic(
    val Text: String = "",
    val FirstURL: String = "",
    val Name: String = "",
    val Topics: List<DuckDuckGoResultItem> = emptyList(),
)
