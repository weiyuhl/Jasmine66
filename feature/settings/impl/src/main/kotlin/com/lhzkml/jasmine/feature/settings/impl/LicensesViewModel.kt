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
import javax.inject.Inject

data class LicenseInfo(
    val name: String,
    val licenseName: String = "",
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
        val licenseIds = context.resources.getIdentifier(
            "third_party_licenses", "raw", context.packageName
        )
        val metadataIds = context.resources.getIdentifier(
            "third_party_license_metadata", "raw", context.packageName
        )

        if (licenseIds == 0 || metadataIds == 0) {
            throw IllegalStateException("许可证信息暂不可用")
        }

        // Read as raw bytes because metadata uses byte-offsets, not character indices
        val licenseBytes = context.resources.openRawResource(licenseIds).readBytes()
        val totalBytes = licenseBytes.size

        // Parse library entries from metadata file
        // Format: "offset:length library_name" (offset/length are byte-based)
        val results = mutableListOf<LicenseInfo>()
        context.resources.openRawResource(metadataIds)
            .bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    val parts = line.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        val headerParts = parts[0].split(":")
                        val offset = headerParts.getOrNull(0)?.toIntOrNull() ?: 0
                        val length = headerParts.getOrNull(1)?.toIntOrNull() ?: 0
                        val libName = parts[1].trim()
                        val licenseText = if (offset >= 0 && length > 0 && offset + length <= totalBytes) {
                            val slice = licenseBytes.copyOfRange(offset, offset + length)
                            String(slice, Charsets.UTF_8).trim()
                        } else ""
                        val licenseName = extractLicenseName(licenseText)
                        results.add(LicenseInfo(name = libName, licenseName = licenseName))
                    }
                }
            }

        return results
    }

    private fun extractLicenseName(url: String): String {
        return when {
            "apache" in url.lowercase() -> "Apache License 2.0"
            "mit" in url.lowercase() -> "MIT License"
            "android.com/studio/terms" in url -> "Android SDK License"
            "opensource.org/licenses/BSD" in url -> "BSD License"
            "gnu.org" in url.lowercase() || "gpl" in url.lowercase() -> "GPL License"
            "mozilla.org" in url.lowercase() -> "Mozilla Public License"
            url.endsWith(".txt") -> url.substringAfterLast('/').removeSuffix(".txt")
            else -> url.substringAfterLast('/')
        }
    }
}
