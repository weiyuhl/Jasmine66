package com.android.sandbox.adapter

import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.tools.ProcessManager
import com.android.sandbox.tools.ParameterSchema
import com.android.sandbox.tools.ToolSchema
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private fun ToolSchema.toDescriptor(): ToolDescriptor {
    val required = parameters.filterValues { it.required }
    val optional = parameters.filterValues { !it.required }
    return ToolDescriptor(
        name = name,
        description = description,
        requiredParameters = required.map { (key, param) -> param.toParamDescriptor(key) },
        optionalParameters = optional.map { (key, param) -> param.toParamDescriptor(key) },
    )
}

private fun ParameterSchema.toParamDescriptor(name: String): ToolParameterDescriptor {
    val jasmineType = when (type) {
        "string" -> ToolParameterType.StringType
        "integer" -> ToolParameterType.IntegerType
        "number" -> ToolParameterType.FloatType
        "boolean" -> ToolParameterType.BooleanType
        "object" -> ToolParameterType.ObjectType(emptyList())
        "array" -> ToolParameterType.ListType(ToolParameterType.StringType)
        else -> ToolParameterType.StringType
    }
    return ToolParameterDescriptor(
        name = name,
        description = description,
        type = jasmineType,
    )
}

private fun parseArgs(arguments: String): Map<String, Any> {
    val json = Json { ignoreUnknownKeys = true }
    val element = json.parseToJsonElement(arguments)
    if (element !is JsonObject) return emptyMap()
    return element.mapValues { (_, v) ->
        when {
            v is JsonPrimitive && v.isString -> v.content
            v is JsonPrimitive && (v.content == "true" || v.content == "false") -> v.content.toBoolean()
            v is JsonPrimitive -> v.content.toIntOrNull() ?: v.content.toDoubleOrNull() ?: v.content
            v is JsonObject -> v
            else -> v.toString()
        }
    }
}

class ExecuteShellCommandTool(
    private val sandboxManager: LinuxSandboxManager,
    private val processManager: ProcessManager,
) : Tool() {

    private val sandboxTool = com.android.sandbox.tools.ShellCommandTool(sandboxManager, processManager)

    override val descriptor: ToolDescriptor = sandboxTool.schema.toDescriptor()

    override suspend fun execute(arguments: String): String {
        val args = parseArgs(arguments)
        val result = sandboxTool.execute(args)
        return formatResult(result)
    }

    private fun formatResult(result: Any): String {
        @Suppress("UNCHECKED_CAST")
        val map = result as? Map<String, Any> ?: return result.toString()
        val success = map["success"] as? Boolean ?: false
        val error = map["error"] as? String
        val stdout = map["stdout"] as? String ?: ""
        val stderr = map["stderr"] as? String ?: ""
        val exitCode = map["exit_code"] as? Int
        val timedOut = map["timed_out"] as? Boolean
        val sessionId = map["session_id"] as? String
        val status = map["status"] as? String
        val message = map["message"] as? String

        return buildString {
            if (!success && error != null) {
                appendLine("Error: $error")
            }
            if (sessionId != null) {
                appendLine("Session ID: $sessionId")
            }
            if (status != null) {
                appendLine("Status: $status")
            }
            if (message != null) {
                appendLine(message)
            }
            if (stdout.isNotEmpty()) {
                appendLine("--- stdout ---")
                appendLine(stdout)
            }
            if (stderr.isNotEmpty()) {
                appendLine("--- stderr ---")
                appendLine(stderr)
            }
            if (exitCode != null) {
                appendLine("Exit code: $exitCode")
            }
            if (timedOut == true) {
                appendLine("Timed out: true")
            }
        }.trimEnd()
    }
}

class ManageProcessTool(
    private val sandboxManager: LinuxSandboxManager,
) : Tool() {

    private val processManager by lazy { ProcessManager(sandboxManager) }
    private val sandboxTool = com.android.sandbox.tools.ProcessManagerTool(sandboxManager)

    override val descriptor: ToolDescriptor = sandboxTool.schema.toDescriptor()

    override suspend fun execute(arguments: String): String {
        val args = parseArgs(arguments)
        val result = sandboxTool.execute(args)
        return formatResult(result)
    }

    private fun formatResult(result: Any): String {
        @Suppress("UNCHECKED_CAST")
        val map = result as? Map<String, Any> ?: return result.toString()
        val success = map["success"] as? Boolean ?: false
        val error = map["error"] as? String
        val message = map["message"] as? String

        return buildString {
            if (!success && error != null) {
                appendLine("Error: $error")
            }
            if (message != null) {
                appendLine(message)
            }

            val running = map["running"] as? List<*>
            val finished = map["finished"] as? List<*>
            val total = map["total"] as? Int

            if (total != null) {
                appendLine("Total sessions: $total")
            }
            if (!running.isNullOrEmpty()) {
                appendLine("--- Running ---")
                @Suppress("UNCHECKED_CAST")
                running.forEach { session ->
                    val info = session as? Map<String, Any> ?: return@forEach
                    appendLine("  [${info["session_id"]}] ${info["command"]} (${info["status"]}, exit=${info["exit_code"]})")
                }
            }
            if (!finished.isNullOrEmpty()) {
                appendLine("--- Finished ---")
                @Suppress("UNCHECKED_CAST")
                finished.forEach { session ->
                    val info = session as? Map<String, Any> ?: return@forEach
                    appendLine("  [${info["session_id"]}] ${info["command"]} (${info["status"]}, exit=${info["exit_code"]})")
                }
            }

            val sessionId = map["session_id"] as? String
            val sessionStatus = map["status"] as? String
            val exitCode = map["exit_code"] as? Int
            val stdout = map["stdout"] as? String
            val stderr = map["stderr"] as? String
            val totalLines = map["total_stdout_lines"] as? Int

            if (sessionId != null) {
                appendLine("Session: $sessionId")
                appendLine("Status: $sessionStatus")
                appendLine("Exit code: $exitCode")
                if (totalLines != null) {
                    appendLine("Total stdout lines: $totalLines")
                }
                if (!stdout.isNullOrEmpty()) {
                    appendLine("--- stdout ---")
                    appendLine(stdout)
                }
                if (!stderr.isNullOrEmpty()) {
                    appendLine("--- stderr ---")
                    appendLine(stderr)
                }
            }
        }.trimEnd()
    }
}
