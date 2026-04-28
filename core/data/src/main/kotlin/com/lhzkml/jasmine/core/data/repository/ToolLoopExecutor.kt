package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.data.model.ToolCallInfo
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.ContextManager
import com.lhzkml.jasmine.core.prompt.llm.StreamResumeHelper
import com.lhzkml.jasmine.core.prompt.llm.chatStreamWithUsageAndThinking
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import com.lhzkml.jasmine.core.prompt.model.ToolCall
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class ToolLoopExecutor @Inject constructor(
    private val toolManager: ChatToolManager,
) {
    private val streamResumeHelper = StreamResumeHelper(maxResumes = 3)

    suspend fun executeToolLoop(
        client: ChatClient,
        messages: List<ChatMessage>,
        model: String,
        samplingParams: ResolvedSamplingParams,
        tools: List<ToolDescriptor>,
        contextManager: ContextManager,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit,
        onResumeAttempt: suspend (Int) -> Unit,
        onToolCallStart: suspend (String, String) -> Unit,
        onToolCallResult: suspend (String, String) -> Unit,
    ): StreamChatResult {
        val mutableMessages = messages.toMutableList()
        val allToolCalls = mutableListOf<ToolCallInfo>()
        var finalContent = ""
        var finalThinking = ""
        var finalFinishReason: String? = null
        var iterations = 0
        val maxIterations = 15
        val recentToolSignatures = mutableListOf<String>()
        val maxRepeatedCalls = 3

        while (iterations < maxIterations) {
            iterations++

            val trimmedMessages = contextManager.trimMessages(mutableMessages.toList())

            val result = streamResumeHelper.streamWithResume(
                client = client,
                messages = trimmedMessages,
                model = model,
                maxTokens = samplingParams.maxTokens,
                samplingParams = samplingParams.coreSamplingParams,
                tools = tools,
                onChunk = onChunk,
                onThinking = onThinking,
                onResumeAttempt = onResumeAttempt,
                contextManager = contextManager,
            )

            finalFinishReason = result.finishReason
            if (result.thinking.isNullOrBlank().not()) {
                finalThinking = result.thinking ?: finalThinking
            }

            if (!result.hasToolCalls) {
                if (result.content.isNotEmpty()) {
                    finalContent = result.content
                }
                break
            }

            val signatures = result.toolCalls.map { "${it.name}:${it.arguments.hashCode()}" }
            if (isRepeatingToolCalls(recentToolSignatures, signatures, maxRepeatedCalls)) {
                finalContent = result.content
                finalFinishReason = "repeated_tool_calls"
                break
            }
            recentToolSignatures.addAll(signatures)
            while (recentToolSignatures.size > maxRepeatedCalls * 2) {
                recentToolSignatures.removeAt(0)
            }

            allToolCalls.addAll(toolManager.toToolCallInfoList(result.toolCalls))

            val toolResults = executeToolCallsInParallel(
                toolCalls = result.toolCalls,
                onToolCallStart = onToolCallStart,
                onToolCallResult = onToolCallResult,
            )

            mutableMessages.add(
                ChatMessage.assistantWithToolCalls(
                    toolCalls = result.toolCalls,
                    content = result.content,
                )
            )
            for (toolResult in toolResults) {
                mutableMessages.add(ChatMessage.toolResult(toolResult))
            }

            if (result.content.isNotEmpty()) {
                finalContent = result.content
            }
        }

        if (iterations >= maxIterations && finalContent.isEmpty()) {
            mutableMessages.add(ChatMessage.user("You have made $maxIterations rounds of tool calls. Based on the information gathered, provide a summary response. Do not make any more tool calls."))
            val finalResult = client.chatStreamWithUsageAndThinking(
                messages = contextManager.trimMessages(mutableMessages),
                model = model,
                maxTokens = samplingParams.maxTokens,
                samplingParams = samplingParams.coreSamplingParams,
                tools = emptyList(),
                onChunk = onChunk,
                onThinking = onThinking,
            )
            finalContent = finalResult.content
            finalFinishReason = "max_iterations"
        }

        return StreamChatResult(
            content = finalContent,
            finishReason = finalFinishReason,
            thinking = finalThinking,
            toolCalls = allToolCalls,
        )
    }

    private fun isRepeatingToolCalls(
        recentSignatures: List<String>,
        currentSignatures: List<String>,
        maxRepeatedCalls: Int,
    ): Boolean {
        if (recentSignatures.isEmpty()) return false
        val repeatedCount = currentSignatures.count { it in recentSignatures }
        return repeatedCount >= maxRepeatedCalls
    }

    private suspend fun executeToolCallsInParallel(
        toolCalls: List<ToolCall>,
        onToolCallStart: suspend (String, String) -> Unit,
        onToolCallResult: suspend (String, String) -> Unit,
    ): List<ToolResult> = supervisorScope {
        toolCalls.map { call ->
            async {
                onToolCallStart(call.name, call.arguments)
                val result = try {
                    withContext(Dispatchers.IO) {
                        withTimeout(120.seconds) {
                            toolManager.execute(call)
                        }
                    }
                } catch (e: Exception) {
                    // 脱敏：不泄露内部堆栈和路径给 LLM
                    val safeMessage = e.message?.take(200)?.replace(Regex("[A-Za-z]:\\\\[\\S]+"), "[path]")
                        ?.replace(Regex("/[a-z]+/[\\w/]+"), "[path]") ?: "Unknown error"
                    ToolResult(
                        callId = call.id,
                        name = call.name,
                        content = "Error: $safeMessage",
                    )
                }
                onToolCallResult(call.name, result.content)
                result
            }
        }.mapIndexed { index, deferred ->
            val call = toolCalls[index]
            runCatching { deferred.await() }.getOrDefault(
                ToolResult(
                    callId = call.id,
                    name = call.name,
                    content = "Tool execution failed",
                )
            )
        }
    }

    suspend fun streamWithoutTools(
        client: ChatClient,
        messages: List<ChatMessage>,
        model: String,
        samplingParams: ResolvedSamplingParams,
        contextManager: ContextManager,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit,
        onResumeAttempt: suspend (Int) -> Unit,
    ): StreamChatResult {
        val trimmedMessages = contextManager.trimMessages(messages)
        val result = streamResumeHelper.streamWithResume(
            client = client,
            messages = trimmedMessages,
            model = model,
            maxTokens = samplingParams.maxTokens,
            samplingParams = samplingParams.coreSamplingParams,
            onChunk = onChunk,
            onThinking = onThinking,
            onResumeAttempt = onResumeAttempt,
            contextManager = contextManager,
        )
        return StreamChatResult(
            content = result.content,
            finishReason = result.finishReason,
            thinking = result.thinking,
            toolCalls = toolManager.toToolCallInfoList(result.toolCalls),
        )
    }
}

data class ResolvedSamplingParams(
    val coreSamplingParams: SamplingParams? = null,
    val maxTokens: Int? = null,
)
