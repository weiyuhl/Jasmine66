package com.lhzkml.jasmine.core.websearch

import com.lhzkml.jasmine.core.websearch.model.WebSearchResult

interface WebSearchService {
    suspend fun search(query: String, maxResults: Int = 5): List<WebSearchResult>
}
