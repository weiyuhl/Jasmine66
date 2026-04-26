package com.lhzkml.jasmine.feature.settings.impl

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

data class LicenseInfo(
    val name: String,
    val version: String = "",
    val license: String = "",
    val licenseContent: String = "",
    val url: String = "",
)

data class LicensesUiState(
    val loading: Boolean = true,
    val licenses: List<LicenseInfo> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(LicensesUiState())
    val state: StateFlow<LicensesUiState> = _state.asStateFlow()

    init {
        loadLicenses()
    }

    private fun loadLicenses() {
        viewModelScope.launch {
            _state.value = LicensesUiState(loading = true)
            try {
                val licenses = withContext(Dispatchers.IO) {
                    parseLicenses()
                }
                _state.value = LicensesUiState(loading = false, licenses = licenses)
            } catch (e: Exception) {
                _state.value = LicensesUiState(loading = false, error = e.message)
            }
        }
    }

    private fun parseLicenses(): List<LicenseInfo> {
        val results = mutableListOf<LicenseInfo>()

        try {
            val jsonText = context.resources.openRawResource(
                context.resources.getIdentifier(
                    "third_party_licenses", "raw", context.packageName
                )
            ).bufferedReader().use { it.readText() }

            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                results.add(
                    LicenseInfo(
                        name = obj.optString("moduleName", obj.optString("libraryName", "Unknown")),
                        version = obj.optString("moduleVersion", obj.optString("libraryVersion", "")),
                        license = obj.optString("moduleLicense", obj.optString("libraryLicense", "")),
                        licenseContent = obj.optString("moduleLicenseContent", ""),
                        url = obj.optString("moduleLicenseUrl", ""),
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback: try reading the combined text
            try {
                val text = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        "third_party_licenses", "raw", context.packageName
                    )
                ).bufferedReader().use { it.readText() }

                // Split by license separator
                val parts = text.split("================================================================")
                    .filter { it.isNotBlank() }
                for (part in parts) {
                    val lines = part.trim().lines()
                    val name = lines.firstOrNull()?.trim() ?: "Unknown"
                    results.add(
                        LicenseInfo(
                            name = name,
                            licenseContent = part.trim(),
                        )
                    )
                }
            } catch (_: Exception) {
                throw IllegalStateException("无法加载开源许可证信息")
            }
        }

        return results
    }
}
