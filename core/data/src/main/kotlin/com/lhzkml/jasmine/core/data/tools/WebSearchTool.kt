package com.lhzkml.jasmine.core.data.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import com.lhzkml.jasmine.core.websearch.WebSearchService
import com.lhzkml.jasmine.core.websearch.model.WebSearchResult
import com.lhzkml.jasmine.core.websearch.util.SearchIntentDetector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 联网搜索工具 - 允许 Agent 主动进行网络搜索获取最新信息
 * 
 * 功能：
 * - 支持自定义搜索查询和结果数量
 * - 自动检测是否需要联网搜索
 * - 返回结构化的搜索结果
 */
@Singleton
class WebSearchTool @Inject constructor(
    private val webSearchService: WebSearchService
) : Tool() {

    companion object {
        const val TOOL_NAME = "web_search"
        const val DEFAULT_MAX_RESULTS = 5
    }

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Search the web for current information on any topic. Use this when you need up-to-date information, " +
            "latest news, current events, weather, stock prices, or any information that may have changed since your training. " +
            "The search will return structured results with titles, snippets, and URLs. " +
            "Use time_filter to narrow results by recency (e.g., 'day' for news from today, 'year' for annual summaries). " +
            "Use region to get locale-specific results (e.g., 'cn-zh' for Chinese results, 'us-en' for US results).",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "query",
                "Search query - what you want to search for. Be specific and include relevant keywords.",
                ToolParameterType.StringType
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "max_results",
                "Maximum number of results to return (default: $DEFAULT_MAX_RESULTS, max: 10)",
                ToolParameterType.IntegerType
            ),
            ToolParameterDescriptor(
                "time_filter",
                "Filter results by time: 'day' (past 24h), 'week' (past week), 'month' (past month), 'year' (past year). Omit for no time filter.",
                ToolParameterType.StringType
            ),
            ToolParameterDescriptor(
                "region",
                "Region/language code for localized results. Format: language-country. Examples: 'cn-zh' (Chinese), 'us-en' (US English), 'jp-jp' (Japanese), 'de-de' (German). Omit for default.",
                ToolParameterType.StringType
            ),
        )
    )

    private fun mapTimeFilter(raw: String?): String? = when (raw?.lowercase()?.trim()) {
        "day", "d" -> "d"
        "week", "w" -> "w"
        "month", "m" -> "m"
        "year", "y" -> "y"
        else -> null
    }

    override suspend fun execute(arguments: String): String {
        val obj = try {
            Json.parseToJsonElement(arguments).jsonObject
        } catch (e: Exception) {
            return "Error: Invalid JSON arguments: ${e.message}"
        }

        val rawQuery = obj["query"]?.jsonPrimitive?.content
            ?: return "Error: Missing required parameter 'query'"

        val maxResults = obj["max_results"]?.jsonPrimitive?.int?.coerceIn(1, 10)
            ?: DEFAULT_MAX_RESULTS

        val timeFilter = mapTimeFilter(obj["time_filter"]?.jsonPrimitive?.content)
        val region = obj["region"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

        // Clean the query using intent detector (strips prefixes like "search for", "查找", etc.)
        val query = SearchIntentDetector.extractSearchQuery(rawQuery) ?: rawQuery

        return try {
            val results = webSearchService.search(query, maxResults, timeFilter, region)

            if (results.isEmpty()) {
                "No search results found for: $query"
            } else {
                formatResults(results, query, timeFilter, region)
            }
        } catch (e: Exception) {
            "Search failed: ${e.message}"
        }
    }

    private fun formatResults(
        results: List<WebSearchResult>,
        query: String,
        timeFilter: String?,
        region: String?,
    ): String {
        val sb = StringBuilder()
        val filterDesc = buildString {
            if (timeFilter != null) append(" (time: $timeFilter)")
            if (region != null) append(" (region: $region)")
        }
        sb.appendLine("## Web Search Results for: $query$filterDesc")
        sb.appendLine()

        results.forEachIndexed { index, result ->
            sb.appendLine("### Result ${index + 1}")
            sb.appendLine("**Title:** ${result.title}")
            sb.appendLine("**Source:** ${result.source}")
            sb.appendLine("**URL:** ${result.url}")
            sb.appendLine("**Content:** ${result.snippet}")
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("Total results: ${results.size}")

        return sb.toString()
    }
}
