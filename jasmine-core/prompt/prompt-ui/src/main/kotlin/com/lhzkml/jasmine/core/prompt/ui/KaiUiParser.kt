package com.lhzkml.jasmine.core.prompt.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object KaiUiParser {

    private val kaiUiBlockRegex = Regex("```kai-ui\\s*\\n?([\\s\\S]*?)\\n?```")

    sealed interface MessageSegment

    data class MarkdownSegment(val content: String) : MessageSegment

    data class UiSegment(val node: KaiUiNode, val rawJson: String) : MessageSegment

    data class ErrorSegment(val rawJson: String) : MessageSegment

    fun containsUiBlocks(message: String): Boolean = kaiUiBlockRegex.containsMatchIn(message)

    private val brokenKeySyntax = Regex(""""(\w+)=([{\[])""")
    private fun fixJsonSyntax(raw: String): String = brokenKeySyntax.replace(raw) { "\"${it.groupValues[1]}\":${it.groupValues[2]}" }

    private fun sanitizeJson(raw: String): String {
        if (raw.isEmpty()) return raw
        if (raw[0] != '{' && raw[0] != '[') return raw
        val stack = mutableListOf<Char>()
        val result = StringBuilder()
        var inString = false
        var escaped = false
        for (i in raw.indices) {
            val c = raw[i]
            if (escaped) {
                escaped = false
                result.append(c)
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                result.append(c)
                continue
            }
            if (c == '"') {
                inString = !inString
                result.append(c)
                continue
            }
            if (inString) {
                result.append(c)
                continue
            }
            when (c) {
                '{', '[' -> {
                    stack.add(c)
                    result.append(c)
                }
                '}' -> if (stack.isNotEmpty() && stack.last() == '{') {
                    stack.removeAt(stack.lastIndex)
                    result.append(c)
                }
                ']' -> if (stack.isNotEmpty() && stack.last() == '[') {
                    stack.removeAt(stack.lastIndex)
                    result.append(c)
                }
                else -> result.append(c)
            }
            if (stack.isEmpty()) return result.toString()
        }
        val trimmed = trimTrailingIncomplete(result.toString(), inString)
        val sb = StringBuilder(trimmed)
        for (i in stack.indices.reversed()) {
            sb.append(if (stack[i] == '{') '}' else ']')
        }
        return sb.toString()
    }

    private fun trimTrailingIncomplete(json: String, inString: Boolean): String {
        var s = json
        if (inString) {
            val lastQuote = s.lastIndexOf('"')
            if (lastQuote >= 0) {
                s = s.substring(0, lastQuote)
            }
        }
        s = s.trimEnd()
        while (s.isNotEmpty()) {
            val last = s.last()
            if (last == ',' || last == ':') {
                s = s.dropLast(1).trimEnd()
            } else if (last == '"') {
                val openQuote = s.lastIndexOf('"', s.lastIndex - 1)
                if (openQuote >= 0) {
                    val before = s.substring(0, openQuote).trimEnd()
                    if (before.isEmpty() || before.last() == ',' || before.last() == '{' || before.last() == '[') {
                        s = before
                    } else {
                        break
                    }
                } else {
                    break
                }
            } else {
                break
            }
        }
        return s
    }

    private fun tryParseLine(line: String): KaiUiNode? {
        try { return parseSingleNode(line) } catch (_: Exception) {}
        try { return parseSingleNode(sanitizeJson(line)) } catch (_: Exception) {}
        return null
    }

    private val helperObjectFields = setOf("value", "children", "action")
    private val knownCompositeFields = setOf("children", "items", "chips", "tabs", "options", "headers", "rows", "collectFrom", "action", "data")
    private val nodeListFields = setOf("children", "items")

    private val knownNodeTypes: Set<String> by lazy {
        KaiUiNode.serializer().descriptor.getElementDescriptor(1).let { desc ->
            (0 until desc.elementsCount).map { desc.getElementName(it) }.toSet()
        }
    }

    private fun flattenToString(arr: JsonArray): JsonPrimitive = JsonPrimitive(arr.joinToString(", ") { if (it is JsonPrimitive) it.content else it.toString() })

    private fun fixMissingTypes(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> JsonArray(element.map { fixMissingTypes(it) })
        is JsonObject -> {
            val fixed = JsonObject(
                element.mapValues { (key, value) ->
                    val processed = fixMissingTypes(value)
                    when {
                        key in nodeListFields && processed is JsonArray ->
                            JsonArray(processed.map { item ->
                                if (item is JsonPrimitive && item.isString) {
                                    JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to item))
                                } else item
                            })
                        key !in knownCompositeFields && processed is JsonArray -> flattenToString(processed)
                        else -> processed
                    }
                }
            )
            if ("type" in fixed) fixed else inferMissingType(fixed)
        }
        else -> element
    }

    private fun stripUnknownNodes(element: JsonElement): JsonElement? = when (element) {
        is JsonObject -> {
            val type = element["type"]?.jsonPrimitive?.contentOrNull
            if (type != null && type !in knownNodeTypes) null
            else JsonObject(element.mapValues { (key, value) ->
                if (key in nodeListFields && value is JsonArray) {
                    JsonArray(value.mapNotNull { stripUnknownNodes(it) })
                } else stripUnknownNodes(value) ?: value
            })
        }
        is JsonArray -> JsonArray(element.map { stripUnknownNodes(it) ?: it })
        else -> element
    }

    private fun inferMissingType(obj: JsonObject): JsonObject {
        if (obj.keys.any { it in helperObjectFields }) return obj
        if ("content" in obj) return JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["content"]?.jsonPrimitive ?: JsonPrimitive(""))))
        if ("title" in obj && "subtitle" in obj) {
            return JsonObject(mapOf("type" to JsonPrimitive("column"), "children" to JsonArray(listOf(
                JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["title"]?.jsonPrimitive ?: JsonPrimitive("")), "style" to JsonPrimitive("title"))),
                JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["subtitle"]?.jsonPrimitive ?: JsonPrimitive("")), "style" to JsonPrimitive("caption"))),
            ))))
        }
        if ("text" in obj) return JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["text"]?.jsonPrimitive ?: JsonPrimitive(""))))
        if ("title" in obj) return JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["title"]?.jsonPrimitive ?: JsonPrimitive("")), "style" to JsonPrimitive("title")))
        if ("label" in obj) return JsonObject(mapOf("type" to JsonPrimitive("text"), "value" to (obj["label"]?.jsonPrimitive ?: JsonPrimitive(""))))
        return obj
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parseSingleNode(jsonStr: String): KaiUiNode? {
        val jsonElement = fixMissingTypes(json.parseToJsonElement(jsonStr))
        val filtered = stripUnknownNodes(jsonElement) ?: return null
        val jsonObject = filtered.jsonObject
        return if ("type" in jsonObject) {
            json.decodeFromJsonElement(KaiUiNode.serializer(), jsonObject)
        } else {
            val wrapped = JsonObject(jsonObject + ("type" to JsonPrimitive("column")))
            json.decodeFromJsonElement(KaiUiNode.serializer(), wrapped)
        }
    }

    fun stripUiBlocks(message: String): String = kaiUiBlockRegex.replace(message, "").trim()

    fun parse(message: String): List<MessageSegment> {
        val segments = mutableListOf<MessageSegment>()
        var lastIndex = 0

        for (match in kaiUiBlockRegex.findAll(message)) {
            val before = message.substring(lastIndex, match.range.first)
            if (before.isNotBlank()) segments.add(MarkdownSegment(before))

            val rawBlock = fixJsonSyntax(match.groupValues[1].trim())
            val lines = rawBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }

            if (lines.size > 1 && lines.all { it.startsWith("{") }) {
                val children = mutableListOf<KaiUiNode>()
                for (line in lines) {
                    val node = tryParseLine(line)
                    if (node != null) children.add(node)
                }
                if (children.isNotEmpty()) {
                    segments.add(UiSegment(ColumnNode(children = children), rawBlock))
                } else {
                    segments.add(ErrorSegment(rawBlock))
                }
            } else {
                val sanitized = sanitizeJson(rawBlock)
                try {
                    val node = parseSingleNode(sanitized)
                    if (node != null) segments.add(UiSegment(node, sanitized))
                } catch (e: Exception) {
                    segments.add(ErrorSegment(sanitized))
                }
            }

            lastIndex = match.range.last + 1
        }

        val remaining = message.substring(lastIndex)
        if (remaining.isNotBlank()) segments.add(MarkdownSegment(remaining))

        return segments
    }
}
