package com.lhzkml.jasmine.core.data.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.lhzkml.jasmine.core.data.log.FileLogger
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceControlTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool() {

    companion object {
        const val TOOL_NAME = "device_control"
        private const val TAG = "DeviceControlTool"
    }

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Controls device hardware and system functions. " +
                "Supported actions: flashlight_on, flashlight_off, create_contact, " +
                "show_map, open_wifi_settings, create_calendar_event.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "action",
                "The device action: 'flashlight_on', 'flashlight_off', 'create_contact', " +
                        "'show_map', 'open_wifi_settings', 'create_calendar_event'.",
                ToolParameterType.StringType
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "parameters",
                "A JSON string with action-specific fields. " +
                        "create_contact: {first_name, last_name, phone_number, email}. " +
                        "show_map: {location}. " +
                        "create_calendar_event: {datetime (YYYY-MM-DDTHH:MM:SS), title}.",
                ToolParameterType.StringType
            )
        )
    )

    override suspend fun execute(arguments: String): String {
        return try {
            val json = Json.parseToJsonElement(arguments).jsonObject
            val action = json["action"]?.jsonPrimitive?.content?.trim() ?: ""
            val paramsStr = json["parameters"]?.jsonPrimitive?.content ?: "{}"
            val params = try { Json.parseToJsonElement(paramsStr).jsonObject } catch (_: Exception) { null }

            FileLogger.log(TAG, "Action: $action, Parameters: $paramsStr")

            when (action) {
                "flashlight_on" -> setFlashlight(true)
                "flashlight_off" -> setFlashlight(false)

                "create_contact" -> {
                    val firstName = params?.get("first_name")?.jsonPrimitive?.content ?: ""
                    val lastName = params?.get("last_name")?.jsonPrimitive?.content ?: ""
                    val phone = params?.get("phone_number")?.jsonPrimitive?.content ?: ""
                    val email = params?.get("email")?.jsonPrimitive?.content ?: ""
                    createContact(firstName, lastName, phone, email)
                }

                "show_map" -> {
                    val location = params?.get("location")?.jsonPrimitive?.content ?: ""
                    showLocationOnMap(location)
                }

                "open_wifi_settings" -> openWifiSettings()

                "create_calendar_event" -> {
                    val datetime = params?.get("datetime")?.jsonPrimitive?.content ?: ""
                    val title = params?.get("title")?.jsonPrimitive?.content ?: ""
                    createCalendarEvent(datetime, title)
                }

                else -> "❌ Unknown device action: '$action'"
            }
        } catch (e: Exception) {
            FileLogger.logError(TAG, "DeviceControlTool error", e)
            "❌ Error: ${e.message}"
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun permissionDeniedMessage(action: String, permission: String): String =
        "⚠️ Cannot execute '$action': $permission permission is not granted. " +
                "Please grant this permission in device Settings > Apps > Jasmine > Permissions."

    // ── Flashlight ──────────────────────────────────────────
    private fun setFlashlight(enabled: Boolean): String {
        // Android 13+ (T+) doesn't require CAMERA permission for flashlight
        val needsCameraPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (needsCameraPermission && !hasPermission(android.Manifest.permission.CAMERA)) {
            return permissionDeniedMessage("flashlight", "CAMERA")
        }
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return "Camera service not available on this device"
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    cameraManager.setTorchMode(id, enabled)
                    return "✅ Flashlight ${if (enabled) "ON" else "OFF"}"
                }
            }
            return "⚠️ No flash unit found on this device."
        } catch (e: Exception) {
            FileLogger.logError(TAG, "Flashlight error", e)
            return "❌ Flashlight error: ${e.message}"
        }
    }

    // ── Create Contact ──────────────────────────────────────
    private fun createContact(firstName: String, lastName: String, phone: String, email: String): String {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, "$firstName $lastName")
            putExtra(ContactsContract.Intents.Insert.EMAIL, email)
            putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return "❌ Error: No contacts app found on device"
        }
        return "✅ Creating contact: $firstName $lastName"
    }

    // ── Show Map ────────────────────────────────────────────
    private fun showLocationOnMap(location: String): String {
        val encoded = URLEncoder.encode(location, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$encoded".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return "❌ Error: No maps app found on device"
        }
        return "✅ Showing '$location' on map"
    }

    // ── WiFi Settings ───────────────────────────────────────
    private fun openWifiSettings(): String {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "✅ Opened WiFi settings"
    }

    // ── Calendar Event ──────────────────────────────────────
    private fun createCalendarEvent(datetime: String, title: String): String {
        var ms = System.currentTimeMillis()
        try {
            val ldt = LocalDateTime.parse(datetime)
            ms = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            FileLogger.log(TAG, "Failed to parse datetime '$datetime', using current time", com.lhzkml.jasmine.core.data.log.LogLevel.WARN)
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + 3600000)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return "❌ Error: No calendar app found on device"
        }
        return "✅ Created calendar event: '$title'"
    }
}
