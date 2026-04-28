package com.lhzkml.jasmine.core.websearch

import android.util.Log
import com.lhzkml.jasmine.core.websearch.model.DuckDuckGoResponse
import com.lhzkml.jasmine.core.websearch.model.WebSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuckDuckGoSearchService(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) : WebSearchService {

    companion object {
        const val DEFAULT_BASE_URL = "https://api.duckduckgo.com/"
        private const val TAG = "DuckDuckGoSearch"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun search(
        query: String,
        maxResults: Int,
        timeFilter: String?,
        region: String?,
    ): List<WebSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "API search: $query (timeFilter=$timeFilter, region=$region)")

                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = buildString {
                    append("${baseUrl}?q=$encodedQuery")
                    append("&format=json&no_html=1&skip_disambig=1&t=jasmine")
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Jasmine Android App")
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "API returned ${resp.code}")
                        return@withContext emptyList()
                    }

                    val body = resp.body?.string() ?: return@withContext emptyList()

                    if (body.trim().startsWith("<!DOCTYPE") || body.trim().startsWith("<html")) {
                        Log.w(TAG, "Received HTML instead of JSON")
                        return@withContext emptyList()
                    }

                    val duckResponse = json.decodeFromString<DuckDuckGoResponse>(body)
                    val results = parseResponse(duckResponse, maxResults)
                    Log.d(TAG, "Found ${results.size} results")
                    results
                }

            } catch (e: Exception) {
                Log.e(TAG, "Search failed: $query", e)
                listOf(
                    WebSearchResult(
                        title = "Search Error",
                        snippet = "搜索失败: ${e.message}",
                        url = "",
                        source = "Error",
                    ),
                )
            }
        }
    }

    private fun parseResponse(response: DuckDuckGoResponse, maxResults: Int): List<WebSearchResult> {
        val results = mutableListOf<WebSearchResult>()

        // 1. Abstract (Wikipedia-style topic summary)
        if (response.AbstractText.isNotBlank()) {
            results.add(
                WebSearchResult(
                    title = response.AbstractSource.ifBlank { response.Heading.ifBlank { "Topic" } },
                    snippet = response.AbstractText,
                    url = response.AbstractURL,
                    source = "Wikipedia",
                ),
            )
        }

        // 2. Instant Answer (calculations, color codes, IP info, etc.)
        if (response.Answer.isNotBlank()) {
            results.add(
                WebSearchResult(
                    title = "Instant Answer (${response.AnswerType})",
                    snippet = response.Answer,
                    url = "",
                    source = "DuckDuckGo",
                ),
            )
        }

        // 3. Definition (dictionary)
        if (response.Definition.isNotBlank()) {
            results.add(
                WebSearchResult(
                    title = "Definition",
                    snippet = response.Definition,
                    url = response.DefinitionURL,
                    source = response.DefinitionSource.ifBlank { "Dictionary" },
                ),
            )
        }

        // 4. External Results (web links)
        for (item in response.Results.take(maxResults)) {
            val iconUrl = item.Icon?.URL ?: ""
            if (item.Text.isNotBlank()) {
                results.add(
                    WebSearchResult(
                        title = item.Text.take(80).replace("<b>", "").replace("</b>", ""),
                        snippet = item.Text,
                        url = item.FirstURL,
                        source = "DuckDuckGo",
                    ),
                )
            }
        }

        // 5. Related Topics
        for (topic in response.RelatedTopics) {
            if (results.size >= maxResults) break

            if (topic.Topics.isNotEmpty()) {
                for (sub in topic.Topics) {
                    if (results.size >= maxResults) break
                    if (sub.Text.isNotBlank()) {
                        results.add(
                            WebSearchResult(
                                title = sub.Text.take(80).replace("<b>", "").replace("</b>", ""),
                                snippet = sub.Text,
                                url = sub.FirstURL,
                                source = "DuckDuckGo",
                            ),
                        )
                    }
                }
            } else if (topic.Text.isNotBlank()) {
                results.add(
                    WebSearchResult(
                        title = topic.Text.take(80).replace("<b>", "").replace("</b>", ""),
                        snippet = topic.Text,
                        url = topic.FirstURL,
                        source = "DuckDuckGo",
                    ),
                )
            }
        }

        return results
    }
}
