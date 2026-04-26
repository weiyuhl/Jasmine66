package com.android.sandbox.core

/**
 * Parsed PRoot-distro plugin file.
 *
 * PRoot-distro defines distros via shell scripts with this format:
 * ```
 * DISTRO_NAME="Ubuntu"
 * DISTRO_COMMENT="A popular Linux distribution."
 * TARBALL_URL['aarch64']="https://..."
 * TARBALL_SHA256['aarch64']="abc123..."
 * ```
 *
 * Optional hook functions:
 * - `distro_setup()` — runs after extraction, before first login
 * - `get_distro_packages()` — returns recommended packages
 */
data class ProotDistroPlugin(
    val id: String,
    val name: String,
    val comment: String,
    val tarballUrls: Map<String, String>,
    val tarballSha256: Map<String, String>,
    val setupCommands: List<String> = emptyList(),
    val recommendedPackages: List<String> = emptyList(),
) {

    fun toLinuxDistro(): LinuxDistro {
        return ProotPluginDistro(
            plugin = this,
        )
    }
}
