package com.android.sandbox.core

/**
 * A [LinuxDistro] backed by a PRoot-distro plugin definition.
 * Allows loading distro definitions from .sh plugin files.
 */
class ProotPluginDistro(
    private val plugin: ProotDistroPlugin,
) : LinuxDistro(
    id = plugin.id,
    name = plugin.name,
    description = plugin.comment,
    version = "plugin", // plugins don't encode version explicitly
    icon = "📦",
) {
    override fun getDownloadUrl(arch: String): String {
        return plugin.tarballUrls[arch]
            ?: plugin.tarballUrls["aarch64"]
            ?: throw IllegalArgumentException("No tarball URL for arch $arch in plugin $id")
    }

    override fun getPackageManagerName(): String {
        // Guess from name/URL patterns
        val lower = plugin.name.lowercase()
        return when {
            "alpine" in lower -> "apk"
            "arch" in lower -> "pacman"
            "fedora" in lower -> "dnf"
            "opensuse" in lower || "suse" in lower -> "zypper"
            "gentoo" in lower -> "emerge"
            else -> "apt"
        }
    }

    override fun getInstallCommand(packages: List<String>): String {
        return when (getPackageManagerName()) {
            "apk" -> "apk add --no-cache ${packages.joinToString(" ")}"
            "pacman" -> "pacman -Sy --noconfirm ${packages.joinToString(" ")}"
            "dnf" -> "dnf install -y ${packages.joinToString(" ")}"
            "zypper" -> "zypper install -y ${packages.joinToString(" ")}"
            "emerge" -> "emerge ${packages.joinToString(" ")}"
            else -> "apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        }
    }

    override fun getUpdateCommand(): String {
        return when (getPackageManagerName()) {
            "apk" -> "apk update"
            "pacman" -> "pacman -Sy"
            "dnf" -> "dnf check-update"
            "zypper" -> "zypper refresh"
            "emerge" -> "emaint sync"
            else -> "apt-get update"
        }
    }

    override fun getDefaultShell(): String {
        return when (getPackageManagerName()) {
            "apk" -> "/bin/sh"
            else -> "/bin/bash"
        }
    }

    override fun getPostExtractCommands(): List<String> {
        val cmds = mutableListOf(
            "echo 'nameserver 8.8.8.8' > /etc/resolv.conf",
            "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf",
        )
        cmds.addAll(plugin.setupCommands)
        return cmds
    }

    override fun getDefaultPackages(): List<String> {
        return plugin.recommendedPackages.ifEmpty {
            listOf("bash", "curl", "wget", "git", "ca-certificates")
        }
    }

    override fun getInitFiles() = defaultInitFiles()
}
