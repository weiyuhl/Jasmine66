package com.android.sandbox.tools

import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.core.SandboxState
import kotlin.time.Duration.Companion.seconds

private const val TOOL_DESCRIPTION = """Execute a shell command in an Alpine Linux sandbox and return stdout, stderr, and exit code. The environment is a full Alpine Linux system running via proot with:
- Shell: /bin/sh (busybox), bash available if installed
- Package manager: apk (e.g. "apk add <package>")
- Default working directory: /root
- Network access available (curl, wget)
- Persistent home directory at /root across commands
Each command runs in a fresh shell — use "cd dir && command" for directory changes.
Output is limited to 15000 characters per stream; for large output, pipe through head/tail.
Default timeout: 30s, max: 60s.
Set background=true to run long-lived processes. Use the manage_process tool to check on them.
Install packages with: apk add <package>
Common packages: python3, py3-pip, nodejs, git, curl, wget, jq, bash, gcc, make"""

class ShellCommandTool(
    private val sandboxManager: LinuxSandboxManager,
    private val processManager: ProcessManager,
) : Tool {

    override val timeout = 60.seconds

    override val schema = ToolSchema(
        name = "execute_shell_command",
        description = TOOL_DESCRIPTION,
        parameters = mapOf(
            "command" to ParameterSchema("string", "The shell command to execute", true),
            "timeout" to ParameterSchema("integer", "Timeout in seconds (default 30, max 60)", false),
            "working_dir" to ParameterSchema("string", "Working directory for the command (default: /root)", false),
            "env" to ParameterSchema("object", "Environment variables to set (key-value pairs)", false),
            "background" to ParameterSchema("boolean", "Run in background and return immediately with a session_id. Use manage_process tool to check status.", false),
        ),
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(args: Map<String, Any>): Any {
        val command = args["command"] as? String
            ?: return mapOf("success" to false, "error" to "Command is required")

        if (sandboxManager.state.value !is SandboxState.Ready) {
            return mapOf("success" to false, "error" to "Linux sandbox is not installed. Set it up first.")
        }

        val timeoutSeconds = ((args["timeout"] as? Number)?.toLong() ?: 30L)
            .coerceIn(1, 60L)
        val workingDir = args["working_dir"] as? String ?: "/root"

        val envMap = (args["env"] as? Map<String, Any>)
            ?.mapValues { it.value.toString() }
            ?: emptyMap()

        val background = args["background"] as? Boolean ?: false
        if (background) {
            return processManager.startBackground(command, timeoutSeconds, workingDir, envMap)
        }

        val executor = sandboxManager.createProotExecutor()
        return executor.execute(command, timeoutSeconds, workingDir, envMap)
    }
}
