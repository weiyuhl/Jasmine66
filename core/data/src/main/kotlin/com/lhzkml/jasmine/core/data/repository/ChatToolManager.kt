package com.lhzkml.jasmine.core.data.repository

import android.content.Context
import com.android.sandbox.adapter.ExecuteShellCommandTool
import com.android.sandbox.adapter.ManageProcessTool
import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.tools.ProcessManager
import com.lhzkml.jasmine.core.agent.tools.AgentEventListener
import com.lhzkml.jasmine.core.agent.tools.CalculatorTool
import com.lhzkml.jasmine.core.agent.tools.GetCurrentTimeTool
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.agent.tools.ToolRegistry
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.data.tools.SkillsTool
import com.lhzkml.jasmine.core.data.tools.RunJsTool
import com.lhzkml.jasmine.core.data.tools.RunIntentTool
import com.lhzkml.jasmine.core.data.tools.DeviceControlTool
import com.lhzkml.jasmine.core.data.tools.WebSearchTool
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.core.prompt.model.ToolCall
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatToolManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSearchTool: WebSearchTool,
    private val skillsTool: SkillsTool,
    private val runJsTool: RunJsTool,
    private val runIntentTool: RunIntentTool,
    private val deviceControlTool: DeviceControlTool,
    private val skillManager: SkillManager,
) {

    private val toolRegistry = ToolRegistry()
    private val sandboxManager: LinuxSandboxManager? by lazy {
        try {
            LinuxSandboxManager(context)
        } catch (e: Exception) {
            null
        }
    }
    private val processManager: ProcessManager? by lazy {
        sandboxManager?.let { ProcessManager(it) }
    }

    init {
        registerDefaultTools()
    }

    fun descriptors(): List<ToolDescriptor> = toolRegistry.descriptors()

    fun hasTools(): Boolean = toolRegistry.descriptors().isNotEmpty()

    suspend fun execute(call: ToolCall): ToolResult = toolRegistry.execute(call)

    suspend fun executeAll(calls: List<ToolCall>): List<ToolResult> = toolRegistry.executeAll(calls)

    fun toToolCallInfoList(toolCalls: List<com.lhzkml.jasmine.core.prompt.model.ToolCall>): List<ToolCallInfo> {
        return toolCalls.map { ToolCallInfo(id = it.id, name = it.name, arguments = it.arguments) }
    }

    private fun registerDefaultTools() {
        CalculatorTool.allTools().forEach { registerTool(it) }
        registerTool(GetCurrentTimeTool)
        registerTool(webSearchTool)
        registerTool(skillsTool)
        registerTool(runJsTool)
        registerTool(runIntentTool)
        registerTool(deviceControlTool)
        registerSandboxTools()
    }

    private fun registerSandboxTools() {
        val sm = sandboxManager ?: return
        val pm = processManager ?: return
        registerTool(ExecuteShellCommandTool(sm, pm))
        registerTool(ManageProcessTool(sm, pm))
    }

    private fun registerTool(tool: Tool) {
        toolRegistry.register(tool)
    }

    fun buildEventListener(onToolCallStart: suspend (String, String) -> Unit = { _, _ -> },
                           onToolCallResult: suspend (String, String) -> Unit = { _, _ -> },
                           onThinking: suspend (String) -> Unit = {},
                           onCompletion: suspend (String, String?) -> Unit = { _, _ -> },
                           onCompression: suspend (String) -> Unit = {}): AgentEventListener {
        return object : AgentEventListener {
            override suspend fun onToolCallStart(toolName: String, arguments: String) = onToolCallStart(toolName, arguments)
            override suspend fun onToolCallResult(toolName: String, result: String) = onToolCallResult(toolName, result)
            override suspend fun onThinking(content: String) = onThinking(content)
            override suspend fun onCompletion(result: String, command: String?) = onCompletion(result, command)
            override suspend fun onCompression(message: String) = onCompression(message)
        }
    }
}
