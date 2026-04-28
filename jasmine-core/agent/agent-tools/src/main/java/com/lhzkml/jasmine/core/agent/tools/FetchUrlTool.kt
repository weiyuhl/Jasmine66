package com.lhzkml.jasmine.core.agent.tools

import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType.StringType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 网页抓取工具集
 * 移植自 AetherLink @aether/fetch
 * 直接 HTTP GET 请求，返回 HTML / 纯文本 / JSON / Markdown 四种格式
 * 不依赖 BrightData，适用于无反爬保护的 URL
 */
class FetchUrlTool : AutoCloseable {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        // 自定义 DNS 解析器：验证解析后的 IP 不是私有地址（防止 DNS 重绑定）
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (hostname in BLOCKED_HOSTS) {
                    throw java.net.UnknownHostException("Blocked host: $hostname")
                }
                val addresses = Dns.SYSTEM.lookup(hostname)
                for (addr in addresses) {
                    if (addr.isSiteLocalAddress || addr.isLoopbackAddress ||
                        addr.isLinkLocalAddress || addr.isMulticastAddress) {
                        throw java.net.UnknownHostException(
                            "Resolved address ${addr.hostAddress} for '$hostname' is a private/internal address"
                        )
                    }
                }
                return addresses
            }
        })
        // Network interceptor 作为第二道防线（重定向后的 host 检查）
        .addNetworkInterceptor { chain ->
            val host = chain.request().url.host
            if (host in BLOCKED_HOSTS) {
                throw java.io.IOException("Access to blocked host '$host'")
            }
            chain.proceed(chain.request())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private suspend fun fetch(arguments: String): Pair<String, String> {
        val obj = Json.parseToJsonElement(arguments).jsonObject
        val url = obj["url"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing parameter 'url'")
        val headers = obj["headers"]?.jsonObject

        // SSRF protection: validate URL before fetching
        validateUrl(url)

        val requestBuilder = Request.Builder().url(url)
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value.jsonPrimitive.content)
        }

        return withContext(Dispatchers.IO) {
            val response = httpClient.newCall(requestBuilder.build()).execute()

            response.use { resp ->
                val status = resp.code
                if (status !in 200..299) {
                    throw RuntimeException("HTTP $status ${resp.message}")
                }

                url to (resp.body?.string() ?: "")
            }
        }
    }

    val fetchHtml = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "fetch_url_as_html",
            description = "Fetches a URL and returns the raw HTML content.",
            requiredParameters = listOf(
                ToolParameterDescriptor("url", "URL to fetch", StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("headers", "Optional HTTP request headers (JSON object)", StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            return try {
                val (_, body) = fetch(arguments)
                body
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    val fetchText = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "fetch_url_as_text",
            description = "Fetches a URL and returns the content as plain text.",
            requiredParameters = listOf(
                ToolParameterDescriptor("url", "URL to fetch", StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("headers", "Optional HTTP request headers (JSON object)", StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            return try {
                val (_, body) = fetch(arguments)
                body.replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), "")
                    .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    val fetchJson = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "fetch_url_as_json",
            description = "Fetches a URL and returns the content parsed as formatted JSON.",
            requiredParameters = listOf(
                ToolParameterDescriptor("url", "URL to fetch", StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("headers", "Optional HTTP request headers (JSON object)", StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            return try {
                val (url, body) = fetch(arguments)
                val parsed = jsonParser.parseToJsonElement(body)
                jsonParser.encodeToString(JsonElement.serializer(), parsed)
            } catch (e: kotlinx.serialization.SerializationException) {
                "Error: Response is not valid JSON"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    /**
     * Markdown 模式（对标 Cursor WebFetch）
     * 将 HTML 内容转换为可读的 Markdown 格式
     */
    val fetchMarkdown = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "fetch_url_as_markdown",
            description = "Fetch content from a URL and return it in a readable markdown format. " +
                "Converts HTML to clean markdown with headings, links, lists, code blocks, etc. " +
                "Use this for reading web pages, documentation, articles, etc. " +
                "The URL must be a fully-formed, valid URL. This tool is read-only.",
            requiredParameters = listOf(
                ToolParameterDescriptor("url", "The URL to fetch. Content will be converted to readable markdown", StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("headers", "Optional HTTP request headers (JSON object)", StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            return try {
                val (url, body) = fetch(arguments)
                htmlToMarkdown(body)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    fun allTools(): List<Tool> = listOf(fetchHtml, fetchText, fetchJson, fetchMarkdown)

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    companion object {
        private val BLOCKED_HOSTS = setOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "[::1]",
            "metadata.google.internal",
        )

        private fun isPrivateAddress(host: String): Boolean {
            return try {
                val addr = InetAddress.getByName(host)
                addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
                    || addr.isMulticastAddress
            } catch (_: Exception) {
                false
            }
        }

        private fun validateUrl(url: String) {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
                ?: throw IllegalArgumentException("Invalid URL: missing scheme")
            if (scheme != "http" && scheme != "https") {
                throw IllegalArgumentException("URL scheme '$scheme' is not allowed")
            }
            val host = uri.host?.lowercase()
                ?: throw IllegalArgumentException("Invalid URL: missing host")
            if (host in BLOCKED_HOSTS) {
                throw IllegalArgumentException("Access to '$host' is blocked")
            }
            if (isPrivateAddress(host)) {
                throw IllegalArgumentException("Access to private/internal address '$host' is blocked")
            }
        }

        /**
         * HTML → Markdown 转换
         * 基于正则的轻量实现，覆盖常见 HTML 元素
         */
        fun htmlToMarkdown(html: String): String {
            var md = html

            // 移除 script 和 style
            md = md.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            md = md.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            md = md.replace(Regex("<nav[^>]*>[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
            md = md.replace(Regex("<footer[^>]*>[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
            md = md.replace(Regex("<!--[\\s\\S]*?-->"), "")

            // 标题 h1-h6
            md = md.replace(Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n# ${cleanInlineHtml(it.groupValues[1])}\n" }
            md = md.replace(Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n## ${cleanInlineHtml(it.groupValues[1])}\n" }
            md = md.replace(Regex("<h3[^>]*>(.*?)</h3>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n### ${cleanInlineHtml(it.groupValues[1])}\n" }
            md = md.replace(Regex("<h4[^>]*>(.*?)</h4>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n#### ${cleanInlineHtml(it.groupValues[1])}\n" }
            md = md.replace(Regex("<h5[^>]*>(.*?)</h5>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n##### ${cleanInlineHtml(it.groupValues[1])}\n" }
            md = md.replace(Regex("<h6[^>]*>(.*?)</h6>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "\n###### ${cleanInlineHtml(it.groupValues[1])}\n" }

            // 代码块 <pre><code>
            md = md.replace(Regex("<pre[^>]*>\\s*<code[^>]*(?:class=\"[^\"]*language-(\\w+)[^\"]*\")?[^>]*>(.*?)</code>\\s*</pre>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                val lang = it.groupValues[1]
                val code = decodeHtmlEntities(it.groupValues[2])
                "\n```$lang\n$code\n```\n"
            }
            md = md.replace(Regex("<pre[^>]*>(.*?)</pre>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                "\n```\n${decodeHtmlEntities(it.groupValues[1])}\n```\n"
            }

            // 内联代码
            md = md.replace(Regex("<code[^>]*>(.*?)</code>", RegexOption.IGNORE_CASE)) {
                "`${decodeHtmlEntities(it.groupValues[1])}`"
            }

            // 粗体和斜体
            md = md.replace(Regex("<(strong|b)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "**${it.groupValues[2]}**" }
            md = md.replace(Regex("<(em|i)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { "*${it.groupValues[2]}*" }

            // 链接
            md = md.replace(Regex("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                val href = it.groupValues[1]
                val text = cleanInlineHtml(it.groupValues[2])
                if (text.isBlank() || text == href) href else "[$text]($href)"
            }

            // 图片
            md = md.replace(Regex("<img[^>]*alt=\"([^\"]*)\"[^>]*src=\"([^\"]*)\"[^>]*/?>", RegexOption.IGNORE_CASE)) {
                "![${it.groupValues[1]}](${it.groupValues[2]})"
            }
            md = md.replace(Regex("<img[^>]*src=\"([^\"]*)\"[^>]*/?>", RegexOption.IGNORE_CASE)) {
                "![](${it.groupValues[1]})"
            }

            // 列表
            md = md.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                "- ${cleanInlineHtml(it.groupValues[1]).trim()}\n"
            }
            md = md.replace(Regex("</?[ou]l[^>]*>", RegexOption.IGNORE_CASE), "\n")

            // 段落和换行
            md = md.replace(Regex("<p[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                "\n${it.groupValues[1].trim()}\n"
            }
            md = md.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            md = md.replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "\n---\n")

            // 引用块
            md = md.replace(Regex("<blockquote[^>]*>(.*?)</blockquote>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) {
                it.groupValues[1].trim().lines().joinToString("\n") { line -> "> ${line.trim()}" } + "\n"
            }

            // 表格（使用非回溯模式避免 ReDoS）
            md = md.replace(Regex("<table[^>]*>[\\s\\S]*?</table>", RegexOption.IGNORE_CASE)) {
                convertTableToMarkdown(it.value)
            }

            // 移除剩余 HTML 标签
            md = md.replace(Regex("<[^>]+>"), "")

            // 解码 HTML 实体
            md = decodeHtmlEntities(md)

            // 清理多余空行
            md = md.replace(Regex("\n{3,}"), "\n\n")

            return md.trim()
        }

        private fun cleanInlineHtml(text: String): String {
            return text.replace(Regex("<[^>]+>"), "").trim()
        }

        private fun decodeHtmlEntities(text: String): String {
            return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .replace("&hellip;", "...")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&laquo;", "«")
                .replace("&raquo;", "»")
                .replace("&copy;", "©")
                .replace("&reg;", "®")
                .replace("&trade;", "™")
                .replace(Regex("&#(\\d+);")) { Char(it.groupValues[1].toInt()).toString() }
                .replace(Regex("&#x([0-9a-fA-F]+);")) { Char(it.groupValues[1].toInt(16)).toString() }
        }

        private fun convertTableToMarkdown(tableHtml: String): String {
            val rows = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .findAll(tableHtml).toList()
            if (rows.isEmpty()) return ""

            val result = StringBuilder("\n")
            var headerDone = false

            for (row in rows) {
                val cells = Regex("<t[hd][^>]*>(.*?)</t[hd]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(row.groupValues[1]).map { cleanInlineHtml(it.groupValues[1]).trim() }.toList()
                if (cells.isEmpty()) continue

                result.appendLine("| ${cells.joinToString(" | ")} |")

                if (!headerDone) {
                    result.appendLine("| ${cells.joinToString(" | ") { "---" }} |")
                    headerDone = true
                }
            }
            result.appendLine()
            return result.toString()
        }
    }
}
