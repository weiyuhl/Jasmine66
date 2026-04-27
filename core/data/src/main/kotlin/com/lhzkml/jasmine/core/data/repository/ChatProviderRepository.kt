package com.lhzkml.jasmine.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ProviderPreset(
    val id: String,
    val name: String,
    val defaultBaseUrl: String,
    val apiTypeString: String,
    val defaultModel: String,
)

@Singleton
class ChatProviderRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "jasmine_provider_encrypted",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        // Migrate data from old plain SharedPreferences if present
        val oldPrefs = context.getSharedPreferences("jasmine_provider", Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty() && encryptedPrefs.all.isEmpty()) {
            val editor = encryptedPrefs.edit()
            for ((key, value) in oldPrefs.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value as Int)
                    is Long -> editor.putLong(key, value as Long)
                    is Float -> editor.putFloat(key, value as Float)
                    is Boolean -> editor.putBoolean(key, value as Boolean)
                }
            }
            editor.apply()
            // Wipe old plaintext data after successful migration
            oldPrefs.edit().clear().apply()
        }
        encryptedPrefs
    }

    companion object {
        private const val KEY_PROVIDER_ID = "active_provider_id"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_BASE_URL_PREFIX = "base_url_"
        private const val KEY_MODEL_PREFIX = "model_"
        private const val KEY_SYSTEM_PROMPT_PREFIX = "system_prompt_"
        private const val KEY_TEMPERATURE_PREFIX = "temperature_"
        private const val KEY_TOP_P_PREFIX = "top_p_"
        private const val KEY_MAX_TOKENS_PREFIX = "max_tokens_"

        val PRESETS = listOf(
            ProviderPreset("deepseek", "DeepSeek", "https://api.deepseek.com", "OPENAI", "deepseek-chat"),
            ProviderPreset("openai", "OpenAI", "https://api.openai.com", "OPENAI", "gpt-4o"),
            ProviderPreset("claude", "Claude", "https://api.anthropic.com", "CLAUDE", "claude-sonnet-4-20250514"),
            ProviderPreset("gemini", "Gemini", "https://generativelanguage.googleapis.com", "GEMINI", "gemini-2.5-flash"),
            ProviderPreset("siliconflow", "硅基流动", "https://api.siliconflow.cn", "OPENAI", "deepseek-ai/DeepSeek-V3"),
            ProviderPreset("openrouter", "OpenRouter", "https://openrouter.ai/api", "OPENAI", "anthropic/claude-3-haiku"),
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

    val configChangesFlow: kotlinx.coroutines.flow.Flow<Long> = _configChangesFlow

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null) return@OnSharedPreferenceChangeListener
        if (key == KEY_PROVIDER_ID ||
            key.startsWith(KEY_API_KEY_PREFIX) ||
            key.startsWith(KEY_BASE_URL_PREFIX) ||
            key.startsWith(KEY_MODEL_PREFIX) ||
            key.startsWith(KEY_SYSTEM_PROMPT_PREFIX) ||
            key.startsWith(KEY_TEMPERATURE_PREFIX) ||
            key.startsWith(KEY_TOP_P_PREFIX) ||
            key.startsWith(KEY_MAX_TOKENS_PREFIX)
        ) {
            _configChangesFlow.value = System.currentTimeMillis()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }
}
