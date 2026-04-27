package com.lhzkml.jasmine.core.data.sandbox

import com.lhzkml.jasmine.core.data.log.FileLogger
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

/**
 * Secure WebView-based JavaScript sandbox for executing skill scripts.
 *
 * Security measures:
 * - Only HTTPS URLs are allowed
 * - File access is disabled
 * - JS interface exposes only the result callback
 * - 30-second timeout for execution
 */
class SkillJsSandbox {

    companion object {
        private const val TAG = "SkillJsSandbox"
        private const val JS_INTERFACE_NAME = "AiEdgeGallery"
    }

    /**
     * Creates a secured WebView instance.
     * Caller is responsible for adding the returned view to the view hierarchy.
     */
    fun createWebView(webView: WebView): WebView {
        return webView.apply {
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                databaseEnabled = false
                domStorageEnabled = false
            }
        }
    }

    /**
     * Executes a skill script in the given WebView.
     *
     * @param webView The WebView to execute the script in
     * @param url The URL to load (must be HTTPS)
     * @param data Skill data to pass to the JS function
     * @param secret Skill secret to pass to the JS function
     * @param timeoutMs Timeout in milliseconds (default 30 seconds)
     * @return The result string from the JS callback
     */
    suspend fun executeSkill(
        webView: WebView,
        url: String,
        data: String,
        secret: String,
        timeoutMs: Long = 30_000L,
    ): String {
        validateUrl(url)

        return withTimeout(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                webView.removeJavascriptInterface(JS_INTERFACE_NAME)

                webView.addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onResultReady(result: String) {
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                }, JS_INTERFACE_NAME)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val script = buildJsPollScript(data, secret)
                        webView.evaluateJavascript(script, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?,
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                SkillExecutionException("WebView error ($errorCode): $description")
                            )
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    webView.stopLoading()
                }

                webView.loadUrl(url)
            }
        }
    }

    private fun buildJsPollScript(data: String, secret: String): String {
        val escapedData = data.replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
        val escapedSecret = secret.replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")

        return """
            (async function() {
                var startTs = Date.now();
                while(true) {
                    if (typeof ai_edge_gallery_get_result === 'function') {
                        break;
                    }
                    await new Promise(function(r) { setTimeout(r, 100); });
                    if (Date.now() - startTs > 10000) {
                        $JS_INTERFACE_NAME.onResultReady('');
                        return;
                    }
                }
                try {
                    var result = await ai_edge_gallery_get_result(`$escapedData`, `$escapedSecret`);
                    $JS_INTERFACE_NAME.onResultReady(String(result));
                } catch(e) {
                    $JS_INTERFACE_NAME.onResultReady('');
                }
            })()
        """.trimIndent()
    }

    private fun validateUrl(url: String) {
        try {
            val uri = URI(url)
            when (uri.scheme) {
                "https" -> {
                    require(uri.host != null && uri.host.isNotBlank()) {
                        "URL must have a valid host: $url"
                    }
                }
                "file" -> {
                    require(uri.authority == "android_asset") {
                        "Only file:///android_asset/ URLs are allowed, got: $url"
                    }
                    val decodedPath = java.net.URLDecoder.decode(uri.path, "UTF-8")
                    require(!decodedPath.contains("..")) {
                        "Path traversal not allowed: $url"
                    }
                }
                else -> throw IllegalArgumentException(
                    "Only HTTPS or android_asset URLs are allowed for skill execution, got: $url"
                )
            }
        } catch (e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw IllegalArgumentException("Invalid URL for skill execution: $url", e)
        }
    }

    class SkillExecutionException(message: String) : Exception(message)
}
