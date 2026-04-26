package com.android.sandbox.tools

import com.android.sandbox.core.LinuxDistro
import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.core.SandboxState
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ShellCommandTool(
    private val sandboxManager: LinuxSandboxManager,
    private val processManager: ProcessManager,
) : Tool {

    override val timeout = 60.seconds

    override val schema: ToolSchema
        get() {
            val distro = sandboxManager.activeDistro
            return ToolSchema(
                name = "execute_shell_command",
                description = LinuxDistro.getToolDescription(distro),
                parameters = mapOf(
                    "command" to ParameterSchema("string", "The shell command to execute", true),
                    "timeout" to ParameterSchema("integer", "Timeout in seconds (default 30, max 60)", false),
                    "working_dir" to ParameterSchema("string", "Working directory for the command (default: /root)", false),
                    "env" to ParameterSchema("object", "Environment variables to set (key-value pairs)", false),
                    "background" to ParameterSchema("boolean", "Run in background and return immediately with a session_id. Use manage_process tool to check status.", false),
                ),
            )
        }

    private fun isSandboxReady(): Boolean {
        if (sandboxManager.state.value is SandboxState.Ready) return true
        val rootfs = File(sandboxManager.rootfsPath)
        val proot = File(sandboxManager.prootPath)
        return rootfs.isDirectory && proot.exists() && proot.canExecute()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(args: Map<String, Any>): Any {
        val command = args["command"] as? String
            ?: return mapOf("success" to false, "error" to "Command is required")

        if (!isSandboxReady()) {
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
