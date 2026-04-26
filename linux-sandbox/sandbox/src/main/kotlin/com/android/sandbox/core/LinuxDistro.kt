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

        override fun getInitFiles() = defaultInitFiles()
    }

    data object Ubuntu : LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu",
        description = "最流行的发行版，海量软件包 (~28MB)",
        version = "24.04.3",
        icon = "🟠",
    ) {
        // Ubuntu cdimage URL pattern:
        // dir:  releases/{point_release}/release/
        // file: ubuntu-base-{patch_version}-base-{arch}.tar.gz
        // Verified at: https://cdimage.ubuntu.com/ubuntu-base/releases/
        override fun getDownloadUrl(arch: String): String {
            val ubuntuArch = when (arch) {
                "aarch64" -> "arm64"
                "armhf" -> "armhf"
                "x86_64" -> "amd64"
                "x86" -> "i386"
                else -> "arm64"
            }
            // 24.04.1 point-release directory hosts the 24.04.3 patch tarball
            return "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04.1/release/ubuntu-base-$version-base-$ubuntuArch.tar.gz"
        }

        override fun getPackageManagerName() = "apt"
        override fun getInstallCommand(packages: List<String>) = "apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        override fun getUpdateCommand() = "apt-get update"
        override fun getDefaultShell() = "/bin/bash"

        override fun getPostExtractCommands(): List<String> = listOf(
            // resolv.conf is already fixed natively by fixResolvConf() before these run.
            // ARM ports: replace archive.ubuntu.com → ports.ubuntu.com if needed
            "if [ -f /etc/apt/sources.list ]; then sed -i 's|http://archive.ubuntu.com|http://ports.ubuntu.com|g' /etc/apt/sources.list; else echo 'deb http://ports.ubuntu.com/ubuntu-ports noble main universe' > /etc/apt/sources.list; fi",
            "apt-get update -qq",
        )

        override fun getDefaultPackages() = listOf(
            "curl", "wget", "git", "jq", "python3", "python3-pip", "nodejs", "npm", "ca-certificates"
        )

        override fun getInitFiles() = defaultInitFiles()
    }

    data object Debian : LinuxDistro(
        id = "debian",
        name = "Debian",
        description = "稳定可靠的社区发行版 (~30MB)",
        version = "12",
        icon = "🔴",
    ) {
        // Proot-distro releases (Debian Bookworm = v12, codename "bookworm").
        // v4.17.3 is the last release with Bookworm (v4.18+ moved to Trixie/13).
        // Filename: debian-{codename}-{arch}-pd-v{version}.tar.xz
        // Verified at: https://github.com/termux/proot-distro/releases/tag/v4.17.3
        override fun getDownloadUrl(arch: String): String {
            val debianArch = when (arch) {
                "aarch64" -> "aarch64"
                "armhf" -> "arm"
                "x86_64" -> "x86_64"
                "x86" -> "i686"
                else -> "aarch64"
            }
            val pdistroVersion = "4.17.3"
            val codename = "bookworm"
            return "https://github.com/termux/proot-distro/releases/download/v$pdistroVersion/debian-$codename-$debianArch-pd-v$pdistroVersion.tar.xz"
        }

        override fun getPackageManagerName() = "apt"
        override fun getInstallCommand(packages: List<String>) = "apt-get update && apt-get install -y ${packages.joinToString(" ")}"
        override fun getUpdateCommand() = "apt-get update"
        override fun getDefaultShell() = "/bin/bash"

        override fun getPostExtractCommands(): List<String> = listOf(
            // resolv.conf already fixed natively. Ensure apt sources are set.
            "apt-get update -qq",
        )

        override fun getDefaultPackages() = listOf(
            "curl", "wget", "git", "jq", "python3", "python3-pip", "nodejs", "npm", "ca-certificates"
        )

        override fun getInitFiles() = defaultInitFiles()
    }

    abstract fun getInitFiles(): Map<String, String>

    companion object {
        private val BUILT_IN: List<LinuxDistro> = listOf(Alpine, Ubuntu, Debian)
        private val pluginDistros = mutableListOf<LinuxDistro>()

        val ALL: List<LinuxDistro> get() = BUILT_IN + pluginDistros
        val DEFAULT: LinuxDistro = Alpine

        fun fromId(id: String): LinuxDistro? = ALL.find { it.id == id }

        fun registerPlugins(plugins: List<ProotDistroPlugin>) {
            pluginDistros.clear()
            pluginDistros.addAll(plugins.map { it.toLinuxDistro() })
        }

        fun defaultInitFiles(): Map<String, String> = mapOf(
            "etc/hosts" to "127.0.0.1 localhost\n::1 localhost ip6-localhost ip6-loopback\n",
            "etc/hostname" to "jasmine-sandbox\n",
            "etc/profile.d/jasmine.sh" to """
                |# Jasmine Linux Sandbox profile
                |# Sources on login shells (-l flag)
                |export LS_COLORS="di=1;34:ln=1;36:so=1;35:pi=1;33:ex=1;32:bd=1;33:cd=1;33:su=1;31:sg=1;31:tw=1;34:ow=1;31"
                |alias ll='ls -alF --color=auto'
                |alias la='ls -A --color=auto'
                |alias l='ls -CF --color=auto'
                |alias grep='grep --color=auto'
                |export PS1='\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
                |echo "Jasmine Linux Sandbox — $(cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d= -f2 | tr -d '\"')"
                |
            """.trimMargin().trimStart(),
        )

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
