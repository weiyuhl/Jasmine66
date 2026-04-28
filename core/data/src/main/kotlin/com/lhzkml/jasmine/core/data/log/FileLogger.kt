package com.lhzkml.jasmine.core.data.log

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private const val LOG_DIR_NAME = "logs"
    private const val LOG_FILE_PREFIX = "jasmine_"
    private const val LOG_FILE_EXTENSION = ".log"
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_LOG_FILES = 5

    private var logDir: File? = null
    private val writeLock = Any()

    fun init(context: Context) {
        logDir = File(context.filesDir, LOG_DIR_NAME)
        if (!logDir!!.exists()) {
            logDir!!.mkdirs()
        }
        cleanupOldLogs()
    }

    fun getLogDirPath(): String? = logDir?.absolutePath

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val logFile = logDir ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] [$level] [$tag] $message\n"

        synchronized(writeLock) {
            try {
                val currentLogFile = File(logFile, "$LOG_FILE_PREFIX${getCurrentDate()}$LOG_FILE_EXTENSION")
                rotateLogFile(currentLogFile)

                FileWriter(currentLogFile, true).use { writer ->
                    writer.append(logEntry)
                }
            } catch (e: Exception) {
                android.util.Log.e("FileLogger", "Failed to write log", e)
            }
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeApiKey(message)
        val exceptionDetails = if (throwable != null) {
            "\n${throwable.javaClass.name}: ${sanitizeApiKey(throwable.message ?: "")}\n${android.util.Log.getStackTraceString(throwable)}"
        } else {
            ""
        }
        log(tag, "$sanitizedMessage$exceptionDetails", LogLevel.ERROR)
    }

    fun logApiError(tag: String, url: String, statusCode: Int, responseBody: String, extra: String = "") {
        val message = buildString {
            appendLine("=== API Error ===")
            appendLine("URL: $url")
            appendLine("Status Code: $statusCode")
            if (extra.isNotEmpty()) {
                appendLine("Extra: $extra")
            }
            appendLine("Response Body:")
            appendLine(responseBody)
            appendLine("=== End API Error ===")
        }
        log(tag, message, LogLevel.ERROR)
    }

    fun getLogFiles(): List<File> {
        val dir = logDir ?: return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.name.endsWith(LOG_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getCombinedLogs(): String {
        val files = getLogFiles()
        return buildString {
            for (file in files) {
                try {
                    appendLine("=== ${file.name} ===")
                    append(file.readText())
                    appendLine()
                } catch (e: Exception) {
                    appendLine("Failed to read ${file.name}: ${e.message}")
                }
            }
        }
    }

    fun clearLogs() {
        val dir = logDir ?: return
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun sanitizeApiKey(message: String): String {
        var result = message
            .replace(Regex("""Bearer\s+[\w\-._~+/]+=*""", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("""(?i)x-api-key:\s*[\w\-._~+/]+=*"""), "x-api-key: [REDACTED]")
            .replace(Regex("""[?&]key=[\w\-._~+/]+=*"""), "?key=[REDACTED]")
            .replace(Regex("""(?i)api_key=[\w\-._~+/]+=*"""), "api_key=[REDACTED]")
            .replace(Regex("""(?i)api_key:\s*[\w\-._~+/]+=*"""), "api_key: [REDACTED]")
            .replace(Regex("""(?i)authorization:\s*Basic\s+[\w+/]+=*"""), "Authorization: Basic [REDACTED]")
            .replace(Regex("""sk-[\w\-]{20,}"""), "sk-[REDACTED]")
        return result
    }

    private fun getCurrentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun rotateLogFile(file: File) {
        if (file.exists() && file.length() > MAX_LOG_FILE_SIZE) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val archivedFile = File(file.parent, "${file.nameWithoutExtension}_${timestamp}$LOG_FILE_EXTENSION")
            file.renameTo(archivedFile)
        }
    }

    private fun cleanupOldLogs() {
        val dir = logDir ?: return
        val logFiles = dir.listFiles { file ->
            file.isFile && file.name.endsWith(LOG_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size > MAX_LOG_FILES) {
            logFiles.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
