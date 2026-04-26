package com.android.sandbox.core

sealed interface SandboxState {
    data object NotInstalled : SandboxState
    data class Downloading(val progress: Float) : SandboxState
    data object Extracting : SandboxState
    data class Installing(val distro: LinuxDistro, val detail: String = "") : SandboxState
    data class Ready(val distro: LinuxDistro) : SandboxState
    data class Error(val distro: LinuxDistro, val message: String) : SandboxState
}
