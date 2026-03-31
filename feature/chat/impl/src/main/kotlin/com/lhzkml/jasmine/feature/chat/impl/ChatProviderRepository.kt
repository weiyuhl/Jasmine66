package com.lhzkml.jasmine.feature.chat.impl

import android.content.Context
import android.content.SharedPreferences
import com.lhzkml.jasmine.core.prompt.executor.ApiType
import com.lhzkml.jasmine.core.prompt.executor.ChatClientConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 供应商预设
 */
data class ProviderPreset(
    val id: String,
    val name: String,
    val defaultBaseUrl: String,
    val apiType: ApiType,
    val defaultModel: String,
)

/**
 * 轻量级供应商配置仓库
 * 使用 SharedPreferences 持久化供应商选择、API Key、Base URL 和模型名称。
 */
@Singleton
class ChatProviderRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jasmine_provider", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROVIDER_ID = "active_provider_id"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_BASE_URL_PREFIX = "base_url_"
        private const val KEY_MODEL_PREFIX = "model_"

        /** 内置供应商列表 */
        val PRESETS = listOf(
            ProviderPreset("deepseek", "DeepSeek", "https://api.deepseek.com", ApiType.OPENAI, "deepseek-chat"),
            ProviderPreset("openai", "OpenAI", "https://api.openai.com", ApiType.OPENAI, "gpt-4o"),
            ProviderPreset("claude", "Claude", "https://api.anthropic.com", ApiType.CLAUDE, "claude-sonnet-4-20250514"),
            ProviderPreset("gemini", "Gemini", "https://generativelanguage.googleapis.com", ApiType.GEMINI, "gemini-2.5-flash"),
            ProviderPreset("siliconflow", "硅基流动", "https://api.siliconflow.cn", ApiType.OPENAI, "deepseek-ai/DeepSeek-V3"),
        )
    }

    fun getActiveProviderId(): String? = prefs.getString(KEY_PROVIDER_ID, null)

    fun setActiveProviderId(id: String) {
        prefs.edit().putString(KEY_PROVIDER_ID, id).apply()
    }

    fun getApiKey(providerId: String): String =
        prefs.getString(KEY_API_KEY_PREFIX + providerId, "") ?: ""

    fun setApiKey(providerId: String, key: String) {
        prefs.edit().putString(KEY_API_KEY_PREFIX + providerId, key).apply()
    }

    fun getBaseUrl(providerId: String): String {
        val saved = prefs.getString(KEY_BASE_URL_PREFIX + providerId, "") ?: ""
        if (saved.isNotEmpty()) return saved
        return PRESETS.find { it.id == providerId }?.defaultBaseUrl ?: ""
    }

    fun setBaseUrl(providerId: String, url: String) {
        prefs.edit().putString(KEY_BASE_URL_PREFIX + providerId, url).apply()
    }

    fun getModel(providerId: String): String {
        val saved = prefs.getString(KEY_MODEL_PREFIX + providerId, "") ?: ""
        if (saved.isNotEmpty()) return saved
        return PRESETS.find { it.id == providerId }?.defaultModel ?: ""
    }

    fun setModel(providerId: String, model: String) {
        prefs.edit().putString(KEY_MODEL_PREFIX + providerId, model).apply()
    }

    /**
     * 构建当前激活供应商的 ChatClientConfig，
     * 如果未配置供应商或 API Key 为空则返回 null。
     */
    fun getActiveConfig(): ChatClientConfig? {
        val id = getActiveProviderId() ?: return null
        val preset = PRESETS.find { it.id == id } ?: return null
        val apiKey = getApiKey(id)
        if (apiKey.isBlank()) return null

        return ChatClientConfig(
            providerId = id,
            providerName = preset.name,
            apiKey = apiKey,
            baseUrl = getBaseUrl(id),
            apiType = preset.apiType,
        )
    }

    /** 保存完整供应商配置 */
    fun saveProviderConfig(providerId: String, apiKey: String, baseUrl: String, model: String) {
        prefs.edit()
            .putString(KEY_PROVIDER_ID, providerId)
            .putString(KEY_API_KEY_PREFIX + providerId, apiKey)
            .putString(KEY_BASE_URL_PREFIX + providerId, baseUrl)
            .putString(KEY_MODEL_PREFIX + providerId, model)
            .apply()
    }
}
