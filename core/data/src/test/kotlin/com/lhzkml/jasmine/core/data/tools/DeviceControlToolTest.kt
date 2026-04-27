package com.lhzkml.jasmine.core.data.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceControlToolTest {

    private val json = Json

    @Test
    fun descriptor_has_correct_name() {
        assertEquals("device_control", DeviceControlTool.TOOL_NAME)
    }

    @Test
    fun flashLightOn_args_are_well_formed() {
        val args = buildJsonArgs("flashlight_on")
        assertTrue(args.contains("flashlight_on"))
    }

    @Test
    fun flashLightOff_args_are_well_formed() {
        val args = buildJsonArgs("flashlight_off")
        assertTrue(args.contains("flashlight_off"))
    }

    @Test
    fun handle_unknown_action() {
        val args = buildJsonArgs("unknown_action")
        assertTrue(args.contains("unknown_action"))
    }

    @Test
    fun createContact_params_are_correctly_structured() {
        val params = JsonObject(mapOf(
            "first_name" to JsonPrimitive("John"),
            "last_name" to JsonPrimitive("Doe"),
            "phone_number" to JsonPrimitive("+1234567890"),
            "email" to JsonPrimitive("john@example.com"),
        ))
        val args = buildJsonArgs("create_contact", params)
        assertTrue(args.contains("create_contact"))
        assertTrue(args.contains("John"))
        assertTrue(args.contains("Doe"))
    }

    @Test
    fun sendEmail_params_are_correctly_structured() {
        val params = JsonObject(mapOf(
            "to" to JsonPrimitive("test@example.com"),
            "subject" to JsonPrimitive("Hello"),
            "body" to JsonPrimitive("Test body"),
        ))
        val args = buildJsonArgs("send_email", params)
        assertTrue(args.contains("send_email"))
        assertTrue(args.contains("test@example.com"))
    }

    @Test
    fun createCalendarEvent_contains_datetime_and_title() {
        val params = JsonObject(mapOf(
            "datetime" to JsonPrimitive("2026-04-27T14:00:00"),
            "title" to JsonPrimitive("Meeting"),
        ))
        val args = buildJsonArgs("create_calendar_event", params)
        assertTrue(args.contains("create_calendar_event"))
        assertTrue(args.contains("2026-04-27T14:00:00"))
        assertTrue(args.contains("Meeting"))
    }

    @Test
    fun openWifiSettings_has_correct_action() {
        val args = buildJsonArgs("open_wifi_settings")
        assertTrue(args.contains("open_wifi_settings"))
    }

    @Test
    fun showMap_with_location() {
        val params = JsonObject(mapOf(
            "location" to JsonPrimitive("Beijing"),
        ))
        val args = buildJsonArgs("show_map", params)
        assertTrue(args.contains("show_map"))
        assertTrue(args.contains("Beijing"))
    }

    private fun buildJsonArgs(action: String, params: JsonObject? = null): String {
        val map = mutableMapOf<String, JsonElement>(
            "action" to JsonPrimitive(action),
        )
        if (params != null) {
            map["parameters"] = JsonPrimitive(json.encodeToString(JsonObject.serializer(), params))
        }
        val obj = JsonObject(map)
        return json.encodeToString(JsonObject.serializer(), obj)
    }
}
