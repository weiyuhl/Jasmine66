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
            "The search will return structured results with titles, snippets, and URLs.",
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
            )
        )
    )

    override suspend fun execute(arguments: String): String {
        val obj = try {
            Json.parseToJsonElement(arguments).jsonObject
        } catch (e: Exception) {
            return "Error: Invalid JSON arguments: ${e.message}"
        }

        val query = obj["query"]?.jsonPrimitive?.content
            ?: return "Error: Missing required parameter 'query'"
        
        val maxResults = obj["max_results"]?.jsonPrimitive?.int?.coerceIn(1, 10)
            ?: DEFAULT_MAX_RESULTS

        // 检测是否需要联网搜索（可选的预检查）
        if (!SearchIntentDetector.needsWebSearch(query)) {
            // 即使检测器认为不需要，也执行搜索（因为 Agent 主动调用）
            // 这里只是记录日志
        }

        return try {
            val results = webSearchService.search(query, maxResults)
            
            if (results.isEmpty()) {
                "No search results found for: $query"
            } else {
                formatResults(results, query)
            }
        } catch (e: Exception) {
            "Search failed: ${e.message}"
        }
    }

    /**
     * 格式化搜索结果为可读文本
     */
    private fun formatResults(results: List<WebSearchResult>, query: String): String {
        val sb = StringBuilder()
        sb.appendLine("## Web Search Results for: $query")
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
