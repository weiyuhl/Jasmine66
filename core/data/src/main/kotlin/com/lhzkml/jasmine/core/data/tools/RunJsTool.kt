package com.lhzkml.jasmine.core.data.tools

import com.lhzkml.jasmine.core.data.log.FileLogger
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RunJsTool @Inject constructor(
    private val agentEventBus: AgentEventBus,
    private val skillManager: SkillManager,
) : Tool() {

    companion object {
        const val TOOL_NAME = "run_js"
        private const val TAG = "RunJsTool"
    }

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Runs JS script inside a secure UI sandbox. Used to render Interactive items, extract data, map routes, etc.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "skillName",
                "The name of the skill currently being executed.",
                ToolParameterType.StringType
            ),
            ToolParameterDescriptor(
                "scriptName",
                "The script name to run. Use 'index.html' if not provided by user.",
                ToolParameterType.StringType
            ),
            ToolParameterDescriptor(
                "data",
                "The JSON data to pass to the script. Use empty string if not provided by user.",
                ToolParameterType.StringType
            )
        ),
        optionalParameters = emptyList()
    )

    override suspend fun execute(arguments: String): String = withContext(Dispatchers.IO) {
        val json = Json.parseToJsonElement(arguments).jsonObject
        val skillName = json["skillName"]?.jsonPrimitive?.content?.trim() ?: ""
        val scriptName = json["scriptName"]?.jsonPrimitive?.content?.trim() ?: "index.html"
        val data = json["data"]?.jsonPrimitive?.content?.trim()?.ifEmpty { "{}" } ?: "{}"

        // Only pass secret if the skill's metadata declares it requires one
        val skill = skillManager.getSkillByName(skillName)
        val secret = if (skill?.requireSecret == true) skillManager.getSecret(skillName) else ""

        // Calculate the local asset path for the skill
        val url = "file:///android_asset/skills/${skillName}/scripts/${scriptName}"

        FileLogger.log(TAG, "RunJsTool executed. Preparing to evaluate $url with data: $data")

        try {
            // We use a safe timeout so a badly formed JS script won't hang the LLM generation loop forever 
            val result = withTimeoutOrNull(60_000L) {
                suspendCancellableCoroutine<String> { continuation ->
                    val event = CallJsEvent(url = url, data = data, secret = secret, continuation = continuation)
                    agentEventBus.emitJsEvent(event)
                }
            }
            
            if (result == null) {
                // Timeout occurred
                return@withContext "{\"error\": \"Skill execution timed out. The Javascript engine didn't respond in 60s.\"}"
            }
            
            return@withContext result
        } catch (e: Exception) {
            FileLogger.logError(TAG, "Exception during JS execution request flow", e)
            return@withContext "{\"error\": \"${e.message}\"}"
        }
    }
}
