package com.android.sandbox

import android.content.Context
import com.android.sandbox.core.DistroStatus
import com.android.sandbox.core.LinuxDistro
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
    private var previousState: SandboxState? = null
    private val _status = MutableStateFlow(SandboxStatus())
    override val status: StateFlow<SandboxStatus> = _status

    init {
        scope.launch {
            sandboxManager.state.collect { state ->
                _status.value = mapState(state)
                previousState = state
            }
        }
    }

    private fun mapState(state: SandboxState): SandboxStatus = when (state) {
        is SandboxState.NotInstalled -> SandboxStatus(
            statusText = "Not installed",
            activeDistro = sandboxManager.activeDistro,
        )

        is SandboxState.Downloading -> SandboxStatus(
            working = true,
            progress = state.progress,
            statusText = "Downloading rootfs...",
            activeDistro = sandboxManager.activeDistro,
        )

        is SandboxState.Extracting -> SandboxStatus(
            working = true,
            statusText = "Extracting...",
            activeDistro = sandboxManager.activeDistro,
        )

        is SandboxState.Installing -> {
            val rootfsExists = java.io.File(sandboxManager.rootfsPath).isDirectory
            SandboxStatus(
                installed = rootfsExists,
                working = true,
                statusText = state.detail.ifEmpty { "Installing..." },
                diskUsageMB = cachedDiskUsageMB,
                activeDistro = state.distro,
            )
        }

        is SandboxState.Ready -> {
            if (previousState !is SandboxState.Ready) {
                cachedDiskUsageMB = sandboxManager.getDiskUsageMB()
            }
            SandboxStatus(
                installed = true,
                ready = true,
                statusText = "Ready",
                diskUsageMB = cachedDiskUsageMB,
                packagesInstalled = sandboxManager.arePackagesInstalled(),
                activeDistro = state.distro,
            )
        }

        is SandboxState.Error -> {
            val rootfsExists = java.io.File(sandboxManager.rootfsPath).isDirectory
            SandboxStatus(
                installed = rootfsExists,
                ready = rootfsExists,
                error = true,
                statusText = "Error: ${state.message}",
                diskUsageMB = cachedDiskUsageMB,
                packagesInstalled = if (rootfsExists) sandboxManager.arePackagesInstalled() else false,
                activeDistro = state.distro,
            )
        }
    }

    override fun setup() {
        sandboxManager.setup()
    }

    override fun cancel() {
        sandboxManager.cancel()
    }

    override fun reset() {
        sandboxManager.reset()
    }

    override fun resetAll() {
        sandboxManager.resetAll()
    }

    override fun installPackages() {
        sandboxManager.installPackages()
    }

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
            if (stderr.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(stderr)
            }
            if (error != null) {
                if (isNotEmpty()) append("\n")
                append(error)
            }
            if (exitCode != null && exitCode != 0 && isEmpty()) {
                append("Exit code: $exitCode")
            }
        }
    }

    override fun setActiveDistro(distro: LinuxDistro) {
        sandboxManager.setActiveDistro(distro)
    }

    override fun getActiveDistro(): LinuxDistro = sandboxManager.activeDistro

    override fun getInstalledDistros(): List<LinuxDistro> = sandboxManager.getInstalledDistros()

    override fun getDistroStatus(distro: LinuxDistro): DistroStatus = sandboxManager.getDistroStatus(distro)

    override fun getAvailableDistros(): List<LinuxDistro> = LinuxDistro.ALL
}
