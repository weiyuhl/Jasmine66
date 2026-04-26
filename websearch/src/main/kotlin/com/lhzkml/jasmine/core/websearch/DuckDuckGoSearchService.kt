package com.lhzkml.jasmine.core.websearch

import android.util.Log
import com.lhzkml.jasmine.core.websearch.model.WebSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuckDuckGoSearchService @Inject constructor() : WebSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "DuckDuckGoSearch"
        private const val API_URL = "https://api.duckduckgo.com/"
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
                // The Instant Answer API does not support time/region filters directly,
                // but we pass them as query modifiers to improve results.
                val url = buildString {
                    append("${API_URL}?q=$encodedQuery")
                    append("&format=json&no_html=1&skip_disambig=1&t=jasmine")
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Jasmine Android App (jasmine.lhzkml.com)")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()

                if (!response.isSuccessful) {
                    Log.w(TAG, "API returned ${response.code}")
                    return@withContext emptyList()
                }

                if (body.trim().startsWith("<!DOCTYPE") || body.trim().startsWith("<html")) {
                    Log.w(TAG, "Received HTML instead of JSON")
                    return@withContext emptyList()
                }

                val results = parseResponse(body, maxResults)
                Log.d(TAG, "Found ${results.size} results")
                results

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

    private fun parseResponse(jsonStr: String, maxResults: Int): List<WebSearchResult> {
        val results = mutableListOf<WebSearchResult>()

        try {
            val json = JSONObject(jsonStr)

            // 1. Abstract (Wikipedia-style topic summary) — highest quality
            val abstractText = json.optString("AbstractText")
            val abstractSource = json.optString("AbstractSource")
            val abstractUrl = json.optString("AbstractURL")
            if (abstractText.isNotBlank()) {
                results.add(
                    WebSearchResult(
                        title = abstractSource.ifBlank { json.optString("Heading", "Topic") },
                        snippet = abstractText,
                        url = abstractUrl,
                        source = "Wikipedia",
                    ),
                )
            }

            // 2. Instant Answer (calculations, color codes, IP info, etc.)
            val answer = json.optString("Answer")
            val answerType = json.optString("AnswerType")
            if (answer.isNotBlank()) {
                results.add(
                    WebSearchResult(
                        title = "Instant Answer ($answerType)",
                        snippet = answer,
                        url = "",
                        source = "DuckDuckGo",
                    ),
                )
            }

            // 3. Definition (dictionary)
            val definition = json.optString("Definition")
            val definitionSource = json.optString("DefinitionSource")
            val definitionUrl = json.optString("DefinitionURL")
            if (definition.isNotBlank()) {
                results.add(
                    WebSearchResult(
                        title = "Definition",
                        snippet = definition,
                        url = definitionUrl,
                        source = definitionSource.ifBlank { "Dictionary" },
                    ),
                )
            }

            // 4. External Results (web links) — most important for general search
            val resultsArray = json.optJSONArray("Results")
            if (resultsArray != null) {
                for (i in 0 until minOf(resultsArray.length(), maxResults)) {
                    val item = resultsArray.getJSONObject(i)
                    val text = item.optString("Text", "")
                    val firstUrl = item.optString("FirstURL", "")
                    val icon = item.optJSONObject("Icon")
                    val iconUrl = icon?.optString("URL", "") ?: ""

                    if (text.isNotBlank()) {
                        results.add(
                            WebSearchResult(
                                title = text.take(80).replace("<b>", "").replace("</b>", ""),
                                snippet = text,
                                url = firstUrl,
                                source = "DuckDuckGo",
                            ),
                        )
                    }
                }
            }

            // 5. Related Topics (internal links, Wikipedia sub-topics)
            val relatedTopics = json.optJSONArray("RelatedTopics")
            if (relatedTopics != null) {
                for (i in 0 until minOf(relatedTopics.length(), maxResults)) {
                    if (results.size >= maxResults) break

                    val item = relatedTopics.optJSONObject(i) ?: continue

                    // Disambiguation groups have "Name" + "Topics"
                    val name = item.optString("Name")
                    val topics = item.optJSONArray("Topics")
                    if (topics != null) {
                        for (j in 0 until minOf(topics.length(), maxResults)) {
                            if (results.size >= maxResults) break
                            val sub = topics.getJSONObject(j)
                            val text = sub.optString("Text")
                            val firstUrl = sub.optString("FirstURL")
                            if (text.isNotBlank()) {
                                results.add(
                                    WebSearchResult(
                                        title = text.take(80).replace("<b>", "").replace("</b>", ""),
                                        snippet = text,
                                        url = firstUrl,
                                        source = "DuckDuckGo",
                                    ),
                                )
                            }
                        }
                    } else {
                        val text = item.optString("Text")
                        val firstUrl = item.optString("FirstURL")
                        if (text.isNotBlank()) {
                            results.add(
                                WebSearchResult(
                                    title = text.take(80).replace("<b>", "").replace("</b>", ""),
                                    snippet = text,
                                    url = firstUrl,
                                    source = "DuckDuckGo",
                                ),
                            )
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse API response", e)
        }

        return results
    }
}
