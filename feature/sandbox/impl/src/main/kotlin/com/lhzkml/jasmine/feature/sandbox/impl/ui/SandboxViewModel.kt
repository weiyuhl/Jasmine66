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
