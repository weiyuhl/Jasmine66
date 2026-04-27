package com.lhzkml.jasmine.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.ChatProviderRepository
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.feature.settings.impl.SettingsUiState.Loading
import com.lhzkml.jasmine.feature.settings.impl.SettingsUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val providerRepo: ChatProviderRepository,
    private val clientManager: ChatClientManager,
) : ViewModel() {
    val settingsUiState: StateFlow<SettingsUiState> =
        userDataRepository.userData
            .map { userData ->
                Success(
                    settings = UserEditableSettings(
                        darkThemeConfig = userData.darkThemeConfig,
                        uiEnabled = userData.uiEnabled,
                        webSearchEnabled = userData.webSearchEnabled,
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = WhileSubscribed(5.seconds.inWholeMilliseconds),
                initialValue = Loading,
            )

    val configChangesFlow = providerRepo.configChangesFlow

    // ==================== Dark theme & toggles ====================

    fun updateDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        viewModelScope.launch {
            userDataRepository.setDarkThemeConfig(darkThemeConfig)
        }
    }

    fun updateUiEnabled(uiEnabled: Boolean) {
        viewModelScope.launch {
            userDataRepository.setUiEnabled(uiEnabled)
        }
    }

    fun updateWebSearchEnabled(webSearchEnabled: Boolean) {
        viewModelScope.launch {
            userDataRepository.setWebSearchEnabled(webSearchEnabled)
        }
    }

    // ==================== Provider config delegation ====================

    fun getActiveProviderId(): String? = providerRepo.getActiveProviderId()

    fun setActiveProviderId(id: String) {
        providerRepo.setActiveProviderId(id)
    }

    fun getApiKey(providerId: String): String = providerRepo.getApiKey(providerId)

    fun getBaseUrl(providerId: String): String = providerRepo.getBaseUrl(providerId)

    fun getModel(providerId: String): String = providerRepo.getModel(providerId)

    fun getSystemPrompt(providerId: String): String = providerRepo.getSystemPrompt(providerId)

    fun getTemperature(providerId: String): Double? = providerRepo.getTemperature(providerId)

    fun getTopP(providerId: String): Double? = providerRepo.getTopP(providerId)

    fun getMaxTokens(providerId: String): Int? = providerRepo.getMaxTokens(providerId)

    fun saveProviderConfig(providerId: String, apiKey: String, baseUrl: String, model: String) {
        providerRepo.saveProviderConfig(providerId, apiKey, baseUrl, model)
    }

    fun saveSystemPrompt(providerId: String, prompt: String) {
        providerRepo.setSystemPrompt(providerId, prompt)
    }

    fun saveSamplingParams(
        providerId: String,
        temperature: Double,
        topP: Double,
        maxTokens: Int?,
    ) {
        providerRepo.setTemperature(providerId, temperature)
        providerRepo.setTopP(providerId, topP)
        providerRepo.setMaxTokens(providerId, maxTokens)
    }

    suspend fun listModels(providerId: String, apiKey: String, baseUrl: String): List<String> =
        clientManager.listModelsFor(providerId, apiKey, baseUrl)

    suspend fun getBalance(providerId: String, apiKey: String, baseUrl: String): String? =
        clientManager.getBalanceFor(providerId, apiKey, baseUrl)
}

data class UserEditableSettings(
    val darkThemeConfig: DarkThemeConfig,
    val uiEnabled: Boolean,
    val webSearchEnabled: Boolean,
)

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val settings: UserEditableSettings) : SettingsUiState
}
