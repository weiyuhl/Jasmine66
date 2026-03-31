package com.lhzkml.jasmine.core.data.repository

import android.content.Context
import android.content.SharedPreferences
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
    val apiTypeString: String,
    val defaultModel: String,
)

/**
 * 轻量级供应商配置仓库（位于 core:data，被各个 feature 共享）
 * 使用 SharedPreferences 持久化供应商选择、API Key、Base URL 和模型名称。
 * 注意：此处不依赖任何 jasmine-core 的具体模型类型，只存储字符串配置。
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
        private const val KEY_SYSTEM_PROMPT_PREFIX = "system_prompt_"
        private const val KEY_TEMPERATURE_PREFIX = "temperature_"
        private const val KEY_TOP_P_PREFIX = "top_p_"
        private const val KEY_MAX_TOKENS_PREFIX = "max_tokens_"

        /** 内置供应商列表 */
        val PRESETS = listOf(
            ProviderPreset("deepseek", "DeepSeek", "https://api.deepseek.com", "OPENAI", "deepseek-chat"),
            ProviderPreset("openai", "OpenAI", "https://api.openai.com", "OPENAI", "gpt-4o"),
            ProviderPreset("claude", "Claude", "https://api.anthropic.com", "CLAUDE", "claude-sonnet-4-20250514"),
            ProviderPreset("gemini", "Gemini", "https://generativelanguage.googleapis.com", "GEMINI", "gemini-2.5-flash"),
            ProviderPreset("siliconflow", "硅基流动", "https://api.siliconflow.cn", "OPENAI", "deepseek-ai/DeepSeek-V3"),
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

    /** 保存完整供应商配置 */
    fun saveProviderConfig(providerId: String, apiKey: String, baseUrl: String, model: String) {
        prefs.edit()
            .putString(KEY_API_KEY_PREFIX + providerId, apiKey)
            .putString(KEY_BASE_URL_PREFIX + providerId, baseUrl)
            .putString(KEY_MODEL_PREFIX + providerId, model)
            .apply()
    }

    // ==================== System Prompt ====================

    fun getSystemPrompt(providerId: String): String =
        prefs.getString(KEY_SYSTEM_PROMPT_PREFIX + providerId, "") ?: ""

    fun setSystemPrompt(providerId: String, prompt: String) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT_PREFIX + providerId, prompt).apply()
    }

    // ==================== Sampling Params ====================

    fun getTemperature(providerId: String): Double? {
        val v = prefs.getString(KEY_TEMPERATURE_PREFIX + providerId, null) ?: return null
        return v.toDoubleOrNull()
    }

    fun setTemperature(providerId: String, value: Double?) {
        if (value == null) {
            prefs.edit().remove(KEY_TEMPERATURE_PREFIX + providerId).apply()
        } else {
            prefs.edit().putString(KEY_TEMPERATURE_PREFIX + providerId, value.toString()).apply()
        }
    }

    fun getTopP(providerId: String): Double? {
        val v = prefs.getString(KEY_TOP_P_PREFIX + providerId, null) ?: return null
        return v.toDoubleOrNull()
    }

    fun setTopP(providerId: String, value: Double?) {
        if (value == null) {
            prefs.edit().remove(KEY_TOP_P_PREFIX + providerId).apply()
        } else {
            prefs.edit().putString(KEY_TOP_P_PREFIX + providerId, value.toString()).apply()
        }
    }

    fun getMaxTokens(providerId: String): Int? {
        val v = prefs.getString(KEY_MAX_TOKENS_PREFIX + providerId, null) ?: return null
        return v.toIntOrNull()
    }

    fun setMaxTokens(providerId: String, value: Int?) {
        if (value == null) {
            prefs.edit().remove(KEY_MAX_TOKENS_PREFIX + providerId).apply()
        } else {
            prefs.edit().putString(KEY_MAX_TOKENS_PREFIX + providerId, value.toString()).apply()
        }
    }

    private val _configChangesFlow = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())

    /** 提供 Flow 供 ViewModel 监听配置变更 */
    val configChangesFlow: kotlinx.coroutines.flow.Flow<Long> = _configChangesFlow

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_PROVIDER_ID || (key?.startsWith(KEY_API_KEY_PREFIX) == true)) {
            _configChangesFlow.value = System.currentTimeMillis()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }
}
