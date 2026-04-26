package com.android.sandbox

import com.android.sandbox.core.DistroStatus
import com.android.sandbox.core.LinuxDistro
import kotlinx.coroutines.flow.StateFlow

data class SandboxStatus(
    val installed: Boolean = false,
    val ready: Boolean = false,
    val working: Boolean = false,
    val progress: Float? = null,
    val statusText: String = "",
    val diskUsageMB: Long = 0,
    val packagesInstalled: Boolean = false,
    val error: Boolean = false,
    val activeDistro: LinuxDistro? = null,
)

interface SandboxController {
    val status: StateFlow<SandboxStatus>
    fun setup()
    fun cancel()
    fun reset()
    fun resetAll()
    fun installPackages()
    suspend fun executeCommand(command: String): String
    fun setActiveDistro(distro: LinuxDistro)
    fun getActiveDistro(): LinuxDistro
    fun getInstalledDistros(): List<LinuxDistro>
    fun getDistroStatus(distro: LinuxDistro): DistroStatus
    fun getAvailableDistros(): List<LinuxDistro>
}
