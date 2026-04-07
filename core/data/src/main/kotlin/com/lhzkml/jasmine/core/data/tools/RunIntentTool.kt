package com.lhzkml.jasmine.core.data.tools

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunIntentTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool() {

    companion object {
        const val TOOL_NAME = "run_intent"
        private const val TAG = "RunIntentTool"
    }

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Run an Android intent to interact with the app to perform actions like sending email or SMS.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "intent",
                "The intent action to run (e.g. 'send_email', 'send_sms').",
                ToolParameterType.StringType
            ),
            ToolParameterDescriptor(
                "parameters",
                "A JSON string containing the parameter values required for the intent.",
                ToolParameterType.StringType
            )
        ),
        optionalParameters = emptyList()
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val json = Json.parseToJsonElement(arguments).jsonObject
            val action = json["intent"]?.jsonPrimitive?.content ?: ""
            val parameters = json["parameters"]?.jsonPrimitive?.content ?: ""

            Log.d(TAG, "Run intent called: action='$action', parameters='$parameters'")

            val paramsJson = Json.parseToJsonElement(parameters).jsonObject

            when (action) {
                "send_email" -> {
                    val email = paramsJson["extra_email"]?.jsonPrimitive?.content ?: ""
                    val subject = paramsJson["extra_subject"]?.jsonPrimitive?.content ?: ""
                    val text = paramsJson["extra_text"]?.jsonPrimitive?.content ?: ""

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        data = "mailto:".toUri()
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "✅ Action send_email succeeded"
                }

                "send_sms" -> {
                    val phone = paramsJson["phone_number"]?.jsonPrimitive?.content ?: ""
                    val body = paramsJson["sms_body"]?.jsonPrimitive?.content ?: ""

                    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri()).apply {
                        putExtra("sms_body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "✅ Action send_sms succeeded"
                }

                else -> "❌ Error: Unknown action '$action'"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute run_intent", e)
            "❌ Error executing intent: ${e.message}"
        }
    }
}
