package com.android.sandbox.core

import java.io.File

/**
 * Parses PRoot-distro bash plugin files into [ProotDistroPlugin] objects.
 *
 * Format reference: https://github.com/termux/proot-distro
 *
 * Example plugin:
 * ```
 * DISTRO_NAME="Ubuntu"
 * DISTRO_COMMENT="A popular Linux distribution (24.04 Noble)."
 * TARBALL_URL['aarch64']="https://..."
 * TARBALL_URL['arm']="https://..."
 * TARBALL_SHA256['aarch64']="abc123..."
 * ```
 */
object ProotDistroPluginParser {

    private val nameRegex = Regex("""DISTRO_NAME\s*=\s*"(.*)""")
    private val commentRegex = Regex("""DISTRO_COMMENT\s*=\s*"(.*)""")
    private val urlRegex = Regex("""TARBALL_URL\s*\[\s*['"]([^'"]+)['"]\s*\]\s*=\s*"(.*)""")
    private val shaRegex = Regex("""TARBALL_SHA256\s*\[\s*['"]([^'"]+)['"]\s*\]\s*=\s*"(.*)""")

    fun parse(file: File): ProotDistroPlugin? {
        if (!file.isFile || !file.canRead()) return null

        val content = try {
            file.readText()
        } catch (_: Exception) {
            return null
        }

        val name = nameRegex.find(content)?.groupValues?.get(1) ?: return null
        val comment = commentRegex.find(content)?.groupValues?.get(1) ?: ""

        val urls = urlRegex.findAll(content).associate {
            it.groupValues[1] to it.groupValues[2]
        }
        val shas = shaRegex.findAll(content).associate {
            it.groupValues[1] to it.groupValues[2]
        }

        if (urls.isEmpty()) return null

        val id = file.nameWithoutExtension.lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .trim('-')

        // Extract setup function body if present
        val setupCommands = extractFunctionBody(content, "distro_setup")
        val packages = extractArrayValue(content, "DISTRO_PACKAGES")

        return ProotDistroPlugin(
            id = "plugin-$id",
            name = name,
            comment = comment,
            tarballUrls = urls,
            tarballSha256 = shas,
            setupCommands = setupCommands,
            recommendedPackages = packages,
        )
    }

    fun parseAll(directory: File): List<ProotDistroPlugin> {
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles()
            ?.filter { it.extension in listOf("sh", "plugin") }
            ?.mapNotNull { parse(it) }
            ?: emptyList()
    }

    private fun extractFunctionBody(content: String, functionName: String): List<String> {
        val regex = Regex("""$functionName\s*\(\s*\)\s*\{""")
        val start = regex.find(content) ?: return emptyList()
        val startIdx = start.range.last + 1

        var depth = 1
        var idx = startIdx
        while (idx < content.length) {
            when (content[idx]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return content.substring(startIdx, idx)
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                    }
                }
            }
            idx++
        }
        return emptyList()
    }

    private fun extractArrayValue(content: String, varName: String): List<String> {
        val regex = Regex("""$varName\s*=\s*\(\s*([^)]*)\s*\)""")
        val match = regex.find(content) ?: return emptyList()
        return match.groupValues[1]
            .split(Regex("""\s+"""))
            .map { it.trim('"', '\'', ' ') }
            .filter { it.isNotBlank() }
    }
}
