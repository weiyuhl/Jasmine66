package com.lhzkml.jasmine.feature.sandbox.impl.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sandbox.SandboxController
import com.android.sandbox.SandboxStatus
import com.android.sandbox.core.DistroStatus
import com.android.sandbox.core.LinuxDistro
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
    val activeDistro: LinuxDistro = LinuxDistro.DEFAULT,
    val availableDistros: List<LinuxDistro> = LinuxDistro.ALL,
    val distroStatuses: Map<String, DistroStatus> = emptyMap(),
)

object SandboxCache {
    var osInfo: String? = null
    var kernelInfo: String? = null
    var kernelCompileTime: String? = null
    var archInfo: String? = null
    var packageVersions: List<String>? = null
}

@HiltViewModel
class SandboxViewModel @Inject constructor(
    private val sandboxController: SandboxController,
    private val sandboxManager: LinuxSandboxManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SandboxUiState(
        osInfo = SandboxCache.osInfo,
        kernelInfo = SandboxCache.kernelInfo,
        kernelCompileTime = SandboxCache.kernelCompileTime,
        archInfo = SandboxCache.archInfo,
        packageVersions = SandboxCache.packageVersions ?: emptyList()
    ))
    val state = _state.asStateFlow()

    init {
        refreshDistroStatuses()
        viewModelScope.launch {
            sandboxController.status.collect { sandboxStatus ->
                _state.update {
                    val updated = it.copy(
                        sandboxInstalled = sandboxStatus.installed,
                        sandboxReady = sandboxStatus.ready,
                        sandboxProgress = sandboxStatus.progress,
                        sandboxStatusText = sandboxStatus.statusText,
                        sandboxDiskUsageMB = sandboxStatus.diskUsageMB,
                        sandboxPackagesInstalled = sandboxStatus.packagesInstalled,
                        isWorking = sandboxStatus.working,
                        hasError = sandboxStatus.error,
                        activeDistro = sandboxStatus.activeDistro ?: LinuxDistro.DEFAULT,
                    )
                    if (!sandboxStatus.ready) {
                        SandboxCache.osInfo = null
                        SandboxCache.kernelInfo = null
                        SandboxCache.kernelCompileTime = null
                        SandboxCache.archInfo = null
                        SandboxCache.packageVersions = null
                        updated.copy(
                            osInfo = null,
                            kernelInfo = null,
                            kernelCompileTime = null,
                            archInfo = null,
                            packageVersions = emptyList(),
                        )
                    } else {
                        refreshDistroStatuses()
                        updated
                    }
                }
            }
        }
    }

    fun setActiveDistro(distro: LinuxDistro) {
        sandboxController.setActiveDistro(distro)
        refreshDistroStatuses()
        _state.update {
            it.copy(activeDistro = distro)
        }
    }

    fun refreshDistroStatuses() {
        val statuses = sandboxController.getAvailableDistros().associate { distro ->
            distro.id to sandboxController.getDistroStatus(distro)
        }
        _state.update {
            it.copy(
                availableDistros = sandboxController.getAvailableDistros(),
                distroStatuses = statuses,
            )
        }
    }

    fun fetchSystemInfo() {
        viewModelScope.launch {
            try {
                val distroName = sandboxController.getActiveDistro().name

                val osOutput = sandboxController.executeCommand("cat /etc/os-release | grep PRETTY_NAME | cut -d '=' -f 2 | tr -d '\"'")
                val osInfo = osOutput.trim().takeIf { it.isNotBlank() } ?: distroName

                val kernelOutput = sandboxController.executeCommand("uname -r")
                val kernelInfo = kernelOutput.trim().takeIf { it.isNotBlank() }

                val kernelTimeOutput = sandboxController.executeCommand("uname -v")
                val kernelCompileTime = kernelTimeOutput.trim().takeIf { it.isNotBlank() }

                val archOutput = sandboxController.executeCommand("uname -m")
                val archInfo = archOutput.trim().takeIf { it.isNotBlank() }

                val versionsScript = buildVersionScript()
                val versionsOutput = sandboxController.executeCommand(versionsScript)
                val packageVersions = versionsOutput.lines().map { it.trim() }.filter { it.isNotBlank() }

                SandboxCache.osInfo = osInfo
                SandboxCache.kernelInfo = kernelInfo
                SandboxCache.kernelCompileTime = kernelCompileTime
                SandboxCache.archInfo = archInfo
                SandboxCache.packageVersions = packageVersions

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
            }
        }
    }

    private fun buildVersionScript(): String {
        return """
                    bash --version 2>/dev/null | head -n 1
                    python3 --version 2>/dev/null
                    git --version 2>/dev/null
                    curl --version 2>/dev/null | head -n 1
                    wget --version 2>/dev/null | head -n 1
                    node --version 2>/dev/null | sed 's/^/Node.js /'
                """.trimIndent()
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
