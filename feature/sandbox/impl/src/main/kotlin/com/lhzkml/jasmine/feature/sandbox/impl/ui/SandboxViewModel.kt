package com.lhzkml.jasmine.feature.sandbox.impl.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sandbox.SandboxController
import com.android.sandbox.SandboxStatus
import com.android.sandbox.core.LinuxSandboxManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SandboxUiState(
    val sandboxInstalled: Boolean = false,
    val sandboxReady: Boolean = false,
    val sandboxProgress: Float? = null,
    val sandboxStatusText: String = "",
    val sandboxDiskUsageMB: Long = 0,
    val sandboxPackagesInstalled: Boolean = false,
    val isWorking: Boolean = false,
    val hasError: Boolean = false,
    val osInfo: String? = null,
    val kernelInfo: String? = null,
    val kernelCompileTime: String? = null,
    val archInfo: String? = null,
    val packageVersions: List<String> = emptyList(),
)

@HiltViewModel
class SandboxViewModel @Inject constructor(
    private val sandboxController: SandboxController,
    private val sandboxManager: LinuxSandboxManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SandboxUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sandboxController.status.collect { sandboxStatus ->
                _state.update {
                    it.copy(
                        sandboxInstalled = sandboxStatus.installed,
                        sandboxReady = sandboxStatus.ready,
                        sandboxProgress = sandboxStatus.progress,
                        sandboxStatusText = sandboxStatus.statusText,
                        sandboxDiskUsageMB = sandboxStatus.diskUsageMB,
                        sandboxPackagesInstalled = sandboxStatus.packagesInstalled,
                        isWorking = sandboxStatus.working,
                        hasError = sandboxStatus.error,
                    )
                }
                if (sandboxStatus.ready && _state.value.osInfo == null) {
                    fetchSystemInfo()
                }
            }
        }
    }

    private fun fetchSystemInfo() {
        viewModelScope.launch {
            try {
                // Fetch OS info
                val osOutput = sandboxController.executeCommand("cat /etc/os-release | grep PRETTY_NAME | cut -d '=' -f 2 | tr -d '\"'")
                val osInfo = osOutput.trim().takeIf { it.isNotBlank() } ?: "Alpine Linux"
                
                // Fetch Kernel info
                val kernelOutput = sandboxController.executeCommand("uname -r")
                val kernelInfo = kernelOutput.trim().takeIf { it.isNotBlank() }
                
                val kernelTimeOutput = sandboxController.executeCommand("uname -v")
                val kernelCompileTime = kernelTimeOutput.trim().takeIf { it.isNotBlank() }
                
                // Fetch Architecture info
                val archOutput = sandboxController.executeCommand("uname -m")
                val archInfo = archOutput.trim().takeIf { it.isNotBlank() }
                
                // Fetch Package Versions
                val versionsScript = """
                    bash --version 2>/dev/null | head -n 1
                    python3 --version 2>/dev/null
                    git --version 2>/dev/null
                    curl --version 2>/dev/null | head -n 1
                    wget --version 2>/dev/null | head -n 1
                    node --version 2>/dev/null | sed 's/^/Node.js /'
                """.trimIndent()
                val versionsOutput = sandboxController.executeCommand(versionsScript)
                val packageVersions = versionsOutput.lines().map { it.trim() }.filter { it.isNotBlank() }

                _state.update { 
                    it.copy(
                        osInfo = osInfo,
                        kernelInfo = kernelInfo,
                        kernelCompileTime = kernelCompileTime,
                        archInfo = archInfo,
                        packageVersions = packageVersions,
                    )
                }
            } catch (e: Exception) {
                // Ignore errors silently for system info
            }
        }
    }

    fun onSetupSandbox() {
        sandboxController.setup()
    }

    fun onCancelSandbox() {
        sandboxController.cancel()
    }

    fun onResetSandbox() {
        sandboxController.reset()
    }

    fun onInstallPackages() {
        sandboxController.installPackages()
    }

    fun getExecutor() = sandboxManager.createProotExecutor()

    suspend fun executeCommand(command: String): String {
        return sandboxController.executeCommand(command)
    }
}
