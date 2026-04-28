package com.lhzkml.jasmine.core.data.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.lhzkml.jasmine.core.data.log.FileLogger
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

            FileLogger.log(TAG, "Run intent called: action='$action', parameters='$parameters'")

            val paramsJson = Json.parseToJsonElement(parameters).jsonObject

            when (action) {
                "send_email" -> {
                    val email = paramsJson["extra_email"]?.jsonPrimitive?.content ?: ""
                    val subject = paramsJson["extra_subject"]?.jsonPrimitive?.content ?: ""
                    val text = paramsJson["extra_text"]?.jsonPrimitive?.content ?: ""

                    // 校验邮箱格式
                    if (email.isBlank() || !email.contains("@") || email.length > 254) {
                        return "❌ Error: Invalid email address"
                    }
                    // 校验主题和正文长度
                    if (subject.length > 500) {
                        return "❌ Error: Subject too long (max 500 chars)"
                    }
                    if (text.length > 10000) {
                        return "❌ Error: Body too long (max 10000 chars)"
                    }

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        data = "mailto:".toUri()
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        "✅ Action send_email succeeded"
                    } catch (e: ActivityNotFoundException) {
                        "❌ Error: No email app found on device"
                    }
                }

                "send_sms" -> {
                    val phone = paramsJson["phone_number"]?.jsonPrimitive?.content ?: ""
                    val body = paramsJson["sms_body"]?.jsonPrimitive?.content ?: ""

                    // 校验手机号格式（只允许数字、+、-、空格、括号）
                    val sanitizedPhone = phone.replace(Regex("[^0-9+\\-() ]"), "")
                    if (sanitizedPhone.isBlank() || sanitizedPhone.length > 20) {
                        return "❌ Error: Invalid phone number"
                    }
                    // 校验短信正文长度
                    if (body.length > 5000) {
                        return "❌ Error: SMS body too long (max 5000 chars)"
                    }

                    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$sanitizedPhone".toUri()).apply {
                        putExtra("sms_body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        "✅ Action send_sms succeeded"
                    } catch (e: ActivityNotFoundException) {
                        "❌ Error: No SMS app found on device"
                    }
                }

                else -> "❌ Error: Unknown action '$action'"
            }
        } catch (e: Exception) {
            FileLogger.logError(TAG, "Failed to execute run_intent", e)
            "❌ Error executing intent: ${e.message}"
        }
    }
}
