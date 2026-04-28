package com.android.sandbox.core

data object AlpineInfo {
    const val ID = "alpine"
    const val NAME = "Alpine Linux"
    const val DESCRIPTION = "轻量级 musl-based 发行版 (~5MB)"
    const val VERSION = "3.23.3"
    const val ICON = "🌄"

    fun getDownloadUrl(arch: String): String {
        val branch = VERSION.substringBeforeLast('.')
        return "https://dl-cdn.alpinelinux.org/alpine/v$branch/releases/$arch/alpine-minirootfs-$VERSION-$arch.tar.gz"
    }

    fun getChecksumUrl(arch: String): String {
        val branch = VERSION.substringBeforeLast('.')
        return "https://dl-cdn.alpinelinux.org/alpine/v$branch/releases/$arch/alpine-minirootfs-$VERSION-$arch.tar.gz.sha256"
    }

    const val PACKAGE_MANAGER = "apk"
    const val DEFAULT_SHELL = "/bin/sh"

    fun getUpdateCommand() = "apk update"

    fun getInstallCommand(packages: List<String>) = "apk add --no-cache ${packages.joinToString(" ")}"

    val POST_EXTRACT_COMMANDS = listOf(
        "echo 'nameserver 8.8.8.8' > /etc/resolv.conf",
        "echo 'nameserver 8.8.4.4' >> /etc/resolv.conf",
    )

    val DEFAULT_PACKAGES = listOf("bash", "curl", "wget", "git", "jq", "python3", "py3-pip", "nodejs")

    val INIT_FILES: Map<String, String> = mapOf(
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

    const val TOOL_DESCRIPTION =
        "Execute a shell command in an Alpine Linux sandbox and return stdout, stderr, and exit code. " +
        "The environment is a full Alpine Linux system running via proot with:\n" +
        "- Shell: /bin/sh\n" +
        "- Package manager: apk\n" +
        "- Default working directory: /root\n" +
        "- Network access available (curl, wget)\n" +
        "- Persistent home directory at /root across commands\n" +
        "Each command runs in a fresh shell — use \"cd dir && command\" for directory changes.\n" +
        "Output is limited to 15000 characters per stream; for large output, pipe through head/tail.\n" +
        "Default timeout: 30s, max: 60s.\n" +
        "Set background=true to run long-lived processes. Use the manage_process tool to check on them."
}
