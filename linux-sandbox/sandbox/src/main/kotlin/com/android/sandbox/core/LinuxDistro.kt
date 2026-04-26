package com.android.sandbox.core

sealed class LinuxDistro(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val icon: String,
) {
    abstract fun getDownloadUrl(arch: String): String
    abstract fun getPackageManagerName(): String
    abstract fun getInstallCommand(packages: List<String>): String
    abstract fun getUpdateCommand(): String
    abstract fun getDefaultShell(): String
    abstract fun getPostExtractCommands(): List<String>
    abstract fun getDefaultPackages(): List<String>

    data object Alpine : LinuxDistro(
        id = "alpine",
        name = "Alpine Linux",
        description = "轻量级 musl-based 发行版 (~5MB)",
        version = "3.23.3",
        icon = "🌄",
    ) {
        override fun getDownloadUrl(arch: String): String {
            val branch = version.substringBeforeLast('.')
            return "https://dl-cdn.alpinelinux.org/alpine/v$branch/releases/$arch/alpine-minirootfs-$version-$arch.tar.gz"
        }

        override fun getPackageManagerName() = "apk"
        override fun getInstallCommand(packages: List<String>) = "apk add --no-cache ${packages.joinToString(" ")}"
        override fun getUpdateCommand() = "apk update"
        override fun getDefaultShell() = "/bin/sh"

        override fun getPostExtractCommands(): List<String> = listOf(
            "echo 'nameserver 8.8.8.8' > /etc/resolv.conf",
            "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf",
        )

        override fun getDefaultPackages() = listOf(
            "bash", "curl", "wget", "git", "jq", "python3", "py3-pip", "nodejs"
        )
    }

    data object Ubuntu : LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu",
        description = "最流行的发行版，海量软件包 (~30MB)",
        version = "24.04",
        icon = "🟠",
    ) {
        override fun getDownloadUrl(arch: String): String {
            val ubuntuArch = when (arch) {
                "aarch64" -> "arm64"
                "armhf" -> "armhf"
                "x86_64" -> "amd64"
                "x86" -> "i386"
                else -> "arm64"
            }
            return "https://cdimage.ubuntu.com/ubuntu-base/releases/$version/release/ubuntu-base-$version-base-$ubuntuArch.tar.gz"
        }

        override fun getPackageManagerName() = "apt"
        override fun getInstallCommand(packages: List<String>) = "apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        override fun getUpdateCommand() = "apt-get update"
        override fun getDefaultShell() = "/bin/bash"

        override fun getPostExtractCommands(): List<String> = listOf(
            "echo 'nameserver 8.8.8.8' > /etc/resolv.conf",
            "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf",
            "apt-get update",
        )

        override fun getDefaultPackages() = listOf(
            "curl", "wget", "git", "jq", "python3", "python3-pip", "nodejs", "npm", "ca-certificates"
        )
    }

    data object Debian : LinuxDistro(
        id = "debian",
        name = "Debian",
        description = "稳定可靠的社区发行版 (~30MB)",
        version = "12",
        icon = "🔴",
    ) {
        override fun getDownloadUrl(arch: String): String {
            val debianArch = when (arch) {
                "aarch64" -> "arm64"
                "armhf" -> "armhf"
                "x86_64" -> "amd64"
                "x86" -> "i386"
                else -> "arm64"
            }
            val codename = "bookworm"
            return "https://github.com/termux/proot-distro/releases/download/v4.18.0/debian-$codename-$debianArch.tar.xz"
        }

        override fun getPackageManagerName() = "apt"
        override fun getInstallCommand(packages: List<String>) = "apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        override fun getUpdateCommand() = "apt-get update"
        override fun getDefaultShell() = "/bin/bash"

        override fun getPostExtractCommands(): List<String> = listOf(
            "echo 'nameserver 8.8.8.8' > /etc/resolv.conf",
            "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf",
            "apt-get update",
        )

        override fun getDefaultPackages() = listOf(
            "curl", "wget", "git", "jq", "python3", "python3-pip", "nodejs", "npm", "ca-certificates"
        )
    }

    companion object {
        val ALL: List<LinuxDistro> = listOf(Alpine, Ubuntu, Debian)

        val DEFAULT: LinuxDistro = Alpine

        fun fromId(id: String): LinuxDistro? = ALL.find { it.id == id }

        fun getToolDescription(distro: LinuxDistro): String {
            val pkgManager = distro.getPackageManagerName()
            val shell = distro.getDefaultShell()
            return "Execute a shell command in a ${distro.name} sandbox and return stdout, stderr, and exit code. " +
                "The environment is a full ${distro.name} system running via proot with:\n" +
                "- Shell: $shell\n" +
                "- Package manager: $pkgManager\n" +
                "- Default working directory: /root\n" +
                "- Network access available (curl, wget)\n" +
                "- Persistent home directory at /root across commands\n" +
                "Each command runs in a fresh shell — use \"cd dir && command\" for directory changes.\n" +
                "Output is limited to 15000 characters per stream; for large output, pipe through head/tail.\n" +
                "Default timeout: 30s, max: 60s.\n" +
                "Set background=true to run long-lived processes. Use the manage_process tool to check on them."
        }
    }
}
