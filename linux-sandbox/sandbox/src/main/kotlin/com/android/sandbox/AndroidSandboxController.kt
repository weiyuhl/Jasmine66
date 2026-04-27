package com.android.sandbox

import android.content.Context
import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.core.SandboxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AndroidSandboxController(context: Context) : SandboxController {

    private val sandboxManager = LinuxSandboxManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var cachedDiskUsageMB = 0L
    private val _status = MutableStateFlow(SandboxStatus())
    override val status: StateFlow<SandboxStatus> = _status

    init {
        scope.launch {
            sandboxManager.state.collect { state ->
                _status.value = mapState(state)
            }
        }
    }

    private fun mapState(state: SandboxState): SandboxStatus = when (state) {
        is SandboxState.NotInstalled -> SandboxStatus(statusText = "Not installed")
        is SandboxState.Downloading -> SandboxStatus(working = true, progress = state.progress, statusText = "Downloading rootfs...")
        is SandboxState.Extracting -> SandboxStatus(working = true, statusText = "Extracting...")
        is SandboxState.Installing -> {
            val rootfsExists = java.io.File(sandboxManager.rootfsPath).isDirectory
            SandboxStatus(installed = rootfsExists, working = true, statusText = state.detail.ifEmpty { "Installing..." }, diskUsageMB = cachedDiskUsageMB)
        }
        is SandboxState.Ready -> {
            cachedDiskUsageMB = sandboxManager.getDiskUsageMB()
            SandboxStatus(installed = true, ready = true, statusText = "Ready", diskUsageMB = cachedDiskUsageMB, packagesInstalled = sandboxManager.arePackagesInstalled())
        }
        is SandboxState.Error -> {
            val rootfsExists = java.io.File(sandboxManager.rootfsPath).isDirectory
            SandboxStatus(installed = rootfsExists, ready = rootfsExists, error = true, statusText = "Error: ${state.message}", diskUsageMB = cachedDiskUsageMB, packagesInstalled = if (rootfsExists) sandboxManager.arePackagesInstalled() else false)
        }
    }

    override fun setup() = sandboxManager.setup()
    override fun cancel() = sandboxManager.cancel()
    override fun reset() = sandboxManager.reset()
    override fun installPackages() = sandboxManager.installPackages()

    override suspend fun executeCommand(command: String): String {
        val state = sandboxManager.state.value
        if (state !is SandboxState.Ready) return "Sandbox is not ready"

        val executor = sandboxManager.createProotExecutor()
        val result = executor.execute(command, timeoutSeconds = 30)

        val stdout = result["stdout"] as? String ?: ""
        val stderr = result["stderr"] as? String ?: ""
        val exitCode = result["exit_code"] as? Int
        val error = result["error"] as? String

        return buildString {
            if (stdout.isNotEmpty()) append(stdout)
            if (stderr.isNotEmpty()) { if (isNotEmpty()) append("\n"); append(stderr) }
            if (error != null) { if (isNotEmpty()) append("\n"); append(error) }
            if (exitCode != null && exitCode != 0 && isEmpty()) append("Exit code: $exitCode")
        }
    }

    fun destroy() {
        scope.cancel()
        sandboxManager.destroy()
    }
}
