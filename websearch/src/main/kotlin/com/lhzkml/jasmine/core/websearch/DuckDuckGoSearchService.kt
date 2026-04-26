package com.lhzkml.jasmine.core.websearch

import android.util.Log
import com.lhzkml.jasmine.core.websearch.model.WebSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        private const val SEARCH_URL = "https://api.duckduckgo.com/"
        private const val HTML_SEARCH_URL = "https://duckduckgo.com/html/"
    }

    private suspend fun searchWithContent(query: String, maxResults: Int = 5, timeFilter: String? = null, region: String? = null): List<WebSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching with content extraction for: $query")

                val extractedUrl = extractUrlFromQuery(query)
                if (extractedUrl != null) {
                    Log.d(TAG, "Detected URL in query, fetching content from: $extractedUrl")
                    val content = fetchPageContent(extractedUrl)
                    if (content.isNotEmpty()) {
                        return@withContext listOf(
                            WebSearchResult(
                                title = "Content from ${extractDomain(extractedUrl)}",
                                snippet = content,
                                url = extractedUrl,
                                source = extractDomain(extractedUrl),
                            ),
                        )
                    }
                    Log.w(TAG, "Failed to fetch content from URL: $extractedUrl")
                }

                val searchUrls = getSearchUrls(query, maxResults, timeFilter, region)
                if (searchUrls.isEmpty()) {
                    Log.w(TAG, "No search URLs found")
                    return@withContext emptyList()
                }

                Log.d(TAG, "Found ${searchUrls.size} URLs to fetch content from")

                val results = mutableListOf<WebSearchResult>()
                for (urlData in searchUrls) {
                    try {
                        val content = fetchPageContent(urlData.url)
                        if (content.isNotEmpty()) {
                            results.add(
                                WebSearchResult(
                                    title = urlData.title,
                                    snippet = content.take(500),
                                    url = urlData.url,
                                    source = extractDomain(urlData.url),
                                ),
                            )
                            Log.d(TAG, "Successfully fetched content from ${urlData.url} (${content.length} chars)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch content from ${urlData.url}: ${e.message}")
                    }

                    if (results.size >= maxResults) break
                }

                Log.d(TAG, "Successfully fetched content from ${results.size} pages")
                return@withContext results

            } catch (e: Exception) {
                Log.e(TAG, "Content search failed for query: $query", e)
                return@withContext emptyList()
            }
        }
    }

    private fun extractUrlFromQuery(query: String): String? {
        val patterns = listOf(
            Regex("""https?://[^\s]+"""),
            Regex("""www\.[^\s]+"""),
            Regex("""[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\s]*"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(query)
            if (match != null) {
                var url = match.value
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                Log.d(TAG, "Extracted URL from query '$query': $url")
                return url
            }
        }

        Log.d(TAG, "No URL found in query: $query")
        return null
    }

    private fun isDirectUrl(query: String): Boolean {
        val trimmedQuery = query.trim()

        if (trimmedQuery.startsWith("http://") || trimmedQuery.startsWith("https://")) {
            return true
        }

        if (trimmedQuery.startsWith("www.")) {
            return true
        }

        if (trimmedQuery.contains(".") && !trimmedQuery.contains(" ") &&
            trimmedQuery.length > 4 && trimmedQuery.length < 100
        ) {
            val domainPattern = Regex("""^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}([/?#].*)?$""")
            if (domainPattern.matches(trimmedQuery)) {
                return true
            }
        }

        return false
    }

    private data class UrlData(val title: String, val url: String)

    private fun buildHtmlSearchUrl(query: String, timeFilter: String?, region: String?): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val sb = StringBuilder("${HTML_SEARCH_URL}?q=$encodedQuery")
        if (!timeFilter.isNullOrBlank()) {
            sb.append("&df=$timeFilter")
        }
        if (!region.isNullOrBlank()) {
            sb.append("&kl=$region")
        }
        return sb.toString()
    }

    private suspend fun getSearchUrls(query: String, maxResults: Int, timeFilter: String?, region: String?): List<UrlData> {
        return try {
            val url = buildHtmlSearchUrl(query, timeFilter, region)

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return emptyList()

            if (!response.isSuccessful) {
                Log.w(TAG, "DuckDuckGo search returned ${response.code}")
                return emptyList()
            }

            extractUrlsFromHtml(html, maxResults)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get search URLs", e)
            return emptyList()
        }
    }

    private fun extractUrlsFromHtml(html: String, maxResults: Int): List<UrlData> {
        val results = mutableListOf<UrlData>()

        val patterns = listOf(
            Regex("""<a[^>]*href="(https?://[^"]*)"[^>]*>([^<]*)</a>"""),
            Regex("""href="(https?://[^"]*)"[^>]*>.*?([^<>]{10,})</a>"""),
            Regex("""<a[^>]*href="(https?://[^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL),
        )

        for (pattern in patterns) {
            val matches = pattern.findAll(html).toList()

            for (match in matches) {
                if (results.size >= maxResults) break

                val url = match.groupValues[1]
                val title = cleanHtml(match.groupValues[2]).trim()

                if (isValidContentUrl(url) && title.length > 5) {
                    results.add(UrlData(title = title.take(100), url = url))
                }
            }

            if (results.size >= maxResults) break
        }

        Log.d(TAG, "Extracted ${results.size} URLs from search results")
        return results.take(maxResults)
    }

    private fun isValidContentUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return !lowerUrl.contains("duckduckgo.com") &&
                !lowerUrl.contains("javascript:") &&
                !lowerUrl.contains("#") &&
                !lowerUrl.contains("privacy") &&
                !lowerUrl.contains("settings") &&
                !lowerUrl.contains("ads") &&
                !lowerUrl.contains("tracking") &&
                lowerUrl.startsWith("http")
    }

    private suspend fun fetchPageContent(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return ""

            if (!response.isSuccessful) {
                return ""
            }

            extractTextFromHtml(html)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch content from $url: ${e.message}")
            ""
        }
    }

    private fun extractTextFromHtml(html: String): String {
        try {
            var cleaned = html.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            cleaned = cleaned.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            cleaned = cleaned.replace(Regex("<[^>]*>"), " ")

            cleaned = cleaned
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val sentences = cleaned.split(Regex("[.!?]+")).filter { sentence ->
                val s = sentence.trim()
                s.length > 20 &&
                        s.length < 500 &&
                        !s.contains("click", ignoreCase = true) &&
                        !s.contains("menu", ignoreCase = true) &&
                        !s.contains("navigation", ignoreCase = true) &&
                        s.split(" ").size > 4
            }

            return sentences.take(5).joinToString(". ").take(1000)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text from HTML: ${e.message}")
            return ""
        }
    }

    override suspend fun search(
        query: String,
        maxResults: Int,
        timeFilter: String?,
        region: String?,
    ): List<WebSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching for: $query (timeFilter=$timeFilter, region=$region)")

                val contentResults = searchWithContent(query, maxResults, timeFilter, region)
                if (contentResults.isNotEmpty()) {
                    Log.d(TAG, "Found ${contentResults.size} content-based results")
                    return@withContext contentResults
                }

                val instantResults = searchInstantAnswer(query)
                if (instantResults.isNotEmpty()) {
                    Log.d(TAG, "Found ${instantResults.size} instant answer results")
                    return@withContext instantResults.take(maxResults)
                }

                val htmlResults = searchHTML(query, maxResults, timeFilter, region)
                Log.d(TAG, "Found ${htmlResults.size} HTML search results")

                // Debug: log the first result if available
                if (htmlResults.isNotEmpty()) {
                    val firstResult = htmlResults[0]
                    Log.d(TAG, "First result: title='${firstResult.title}', snippet='${firstResult.snippet.take(100)}...'")
                }

                return@withContext htmlResults

            } catch (e: Exception) {
                Log.e(TAG, "Search failed for query: $query", e)
                return@withContext listOf(
                    WebSearchResult(
                        title = "Search Error",
                        snippet = "Unable to perform web search: ${e.message}. Please check your internet connection.",
                        url = "",
                        source = "Error",
                    ),
                )
            }
        }
    }

    private suspend fun searchInstantAnswer(query: String): List<WebSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "${SEARCH_URL}?q=${encodedQuery}&format=json&no_html=1&skip_disambig=1&t=jasmine"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Jasmine Android App")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return emptyList()

            if (!response.isSuccessful) {
                Log.w(TAG, "DuckDuckGo API returned ${response.code}")
                return emptyList()
            }

            parseInstantAnswerResponse(responseBody)

        } catch (e: Exception) {
            Log.e(TAG, "Instant Answer search failed", e)
            emptyList()
        }
    }

    private fun parseInstantAnswerResponse(jsonResponse: String): List<WebSearchResult> {
        try {
            if (jsonResponse.trim().startsWith("<!DOCTYPE") || jsonResponse.trim().startsWith("<html")) {
                Log.w(TAG, "Received HTML response instead of JSON from instant answer API")
                return emptyList()
            }

            val json = org.json.JSONObject(jsonResponse)
            val results = mutableListOf<WebSearchResult>()

            val abstract = json.optString("Abstract")
            val abstractSource = json.optString("AbstractSource")
            val abstractUrl = json.optString("AbstractURL")

            if (abstract.isNotEmpty()) {
                results.add(
                    WebSearchResult(
                        title = abstractSource.ifEmpty { "Abstract" },
                        snippet = abstract,
                        url = abstractUrl,
                        source = abstractSource,
                    ),
                )
            }

            val definition = json.optString("Definition")
            val definitionSource = json.optString("DefinitionSource")
            val definitionUrl = json.optString("DefinitionURL")

            if (definition.isNotEmpty()) {
                results.add(
                    WebSearchResult(
                        title = "Definition",
                        snippet = definition,
                        url = definitionUrl,
                        source = definitionSource,
                    ),
                )
            }

            val relatedTopics = json.optJSONArray("RelatedTopics")
            relatedTopics?.let { topics ->
                for (i in 0 until minOf(topics.length(), 3)) {
                    val topic = topics.getJSONObject(i)
                    val text = topic.optString("Text")
                    val firstURL = topic.optString("FirstURL")

                    if (text.isNotEmpty()) {
                        results.add(
                            WebSearchResult(
                                title = "Related Topic",
                                snippet = text,
                                url = firstURL,
                                source = "DuckDuckGo",
                            ),
                        )
                    }
                }
            }

            return results

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse instant answer response", e)
            return emptyList()
        }
    }

    private suspend fun searchHTML(query: String, maxResults: Int, timeFilter: String?, region: String?): List<WebSearchResult> {
        return try {
            val url = buildHtmlSearchUrl(query, timeFilter, region)

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return emptyList()

            if (!response.isSuccessful) {
                Log.w(TAG, "DuckDuckGo HTML search returned ${response.code}")
                return emptyList()
            }

            parseHTMLResults(html, maxResults, query)

        } catch (e: Exception) {
            Log.e(TAG, "HTML search failed", e)
            emptyList()
        }
    }

    private fun parseHTMLResults(html: String, maxResults: Int, query: String): List<WebSearchResult> {
        try {
            val results = mutableListOf<WebSearchResult>()

            val newFormatRegex = Regex("""<h2[^>]*>.*?<a[^>]*href="([^"]*)"[^>]*>(.*?)</a>.*?</h2>.*?<a[^>]*class="[^"]*result[^"]*snippet[^"]*"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val newFormatMatches = newFormatRegex.findAll(html).toList()

            if (newFormatMatches.isNotEmpty()) {
                Log.d(TAG, "Using new format parsing, found ${newFormatMatches.size} matches")
                for (match in newFormatMatches.take(maxResults)) {
                    val url = match.groupValues[1]
                    val title = cleanHtml(match.groupValues[2])
                    val snippet = cleanHtml(match.groupValues[3])

                    if (title.isNotEmpty() && snippet.isNotEmpty()) {
                        results.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = url,
                                source = extractDomain(url),
                            ),
                        )
                    }
                }
            }

            if (results.isEmpty()) {
                val titlePattern = Regex("""<a class="result__a"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                val snippetPattern = Regex("""<a class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                val urlPattern = Regex("""<a class="result__url"[^>]*href="([^"]*)"[^>]*>""")

                val titleMatches = titlePattern.findAll(html).toList()
                val snippetMatches = snippetPattern.findAll(html).toList()
                val urlMatches = urlPattern.findAll(html).toList()

                val count = minOf(titleMatches.size, snippetMatches.size, urlMatches.size, maxResults)

                Log.d(TAG, "Using original format parsing, found $count matches")

                for (i in 0 until count) {
                    val title = cleanHtml(titleMatches[i].groupValues[1])
                    val snippet = cleanHtml(snippetMatches[i].groupValues[1])
                    val url = urlMatches[i].groupValues[1]

                    if (title.isNotEmpty() && snippet.isNotEmpty()) {
                        results.add(
                            WebSearchResult(
                                title = title,
                                snippet = snippet,
                                url = url,
                                source = extractDomain(url),
                            ),
                        )
                    }
                }
            }

            if (results.isEmpty()) {
                Log.d(TAG, "Trying generic content extraction")

                val linkPattern = Regex("""<a[^>]*href="(https?://[^"]*)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                val linkMatches = linkPattern.findAll(html).toList()

                var extractedResults = 0
                for (match in linkMatches) {
                    if (extractedResults >= maxResults) break

                    val url = match.groupValues[1]
                    val linkText = cleanHtml(match.groupValues[2])

                    if (linkText.length < 10 ||
                        url.contains("duckduckgo.com") ||
                        url.contains("privacy") ||
                        url.contains("settings") ||
                        linkText.lowercase().contains("more results")
                    ) {
                        continue
                    }

                    val linkIndex = html.indexOf(match.value)
                    val contextStart = maxOf(0, linkIndex - 200)
                    val contextEnd = minOf(html.length, linkIndex + match.value.length + 200)
                    val context = html.substring(contextStart, contextEnd)

                    val snippet = extractSnippetFromContext(context, linkText)

                    if (snippet.isNotEmpty() && snippet.length > 20) {
                        results.add(
                            WebSearchResult(
                                title = linkText.ifEmpty { "Search Result" },
                                snippet = snippet,
                                url = url,
                                source = extractDomain(url),
                            ),
                        )
                        extractedResults++
                    }
                }
            }

            if (results.isEmpty()) {
                Log.w(TAG, "Could not parse any results from HTML for query: $query")
                results.add(
                    WebSearchResult(
                        title = "Search Error",
                        snippet = "无法解析搜索结果。DuckDuckGo 页面结构已变更，请尝试更换搜索关键词。",
                        url = "",
                        source = "DuckDuckGo",
                    ),
                )
            }

            Log.d(TAG, "Successfully parsed ${results.size} HTML results")
            return results

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse HTML results for query: $query", e)
            return listOf(
                WebSearchResult(
                    title = "Search Error",
                    snippet = "搜索处理失败: ${e.message}. 请重试或更换搜索关键词。",
                    url = "",
                    source = "Error",
                ),
            )
        }
    }

    private fun extractSnippetFromContext(context: String, linkText: String): String {
        val cleanContext = cleanHtml(context)

        val sentences = cleanContext.split(Regex("[.!?]+")).map { it.trim() }

        val goodSentences = sentences.filter { sentence ->
            sentence.length > 30 &&
                    sentence.length < 200 &&
                    !sentence.contains(linkText, ignoreCase = true) &&
                    !sentence.contains("click", ignoreCase = true) &&
                    !sentence.contains("more", ignoreCase = true) &&
                    sentence.split(" ").size > 5
        }

        return goodSentences.firstOrNull()?.take(150) ?: ""
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = if (url.startsWith("http")) url else "https://$url"
            val domain = java.net.URL(cleanUrl).host
            domain.removePrefix("www.")
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
