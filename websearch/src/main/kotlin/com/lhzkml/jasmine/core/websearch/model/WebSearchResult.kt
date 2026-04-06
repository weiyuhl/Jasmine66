package com.lhzkml.jasmine.core.websearch.model

data class WebSearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String = "",
)
