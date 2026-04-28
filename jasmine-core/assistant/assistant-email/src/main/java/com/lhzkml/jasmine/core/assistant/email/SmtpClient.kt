package com.lhzkml.jasmine.core.assistant.email

/**
 * 最小化 SMTP 客户端
 */
class SmtpClient(
    private val host: String,
    private val port: Int = 587,
    private val useStartTls: Boolean = true,
) {
    private var connection: EmailConnection? = null

    suspend fun connect() {
        connection = createEmailConnection(host, port, false)
        readResponse() // 220
    }

    suspend fun ehlo() {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("EHLO assistant")
        readResponse()
    }

    suspend fun startTls() {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("STARTTLS")
        readResponse() // 220
        conn.upgradeToTls(host)
        ehlo()
    }

    suspend fun authenticate(username: String, password: String) {
        val conn = connection ?: throw IllegalStateException("Not connected")
        conn.writeLine("AUTH LOGIN")
        readResponse() // 334
        conn.writeLine(encodeBase64(username))
        readResponse() // 334
        conn.writeLine(encodeBase64(password))
        readResponse() // 235
    }

    suspend fun sendReply(from: String, to: String, subject: String, body: String, inReplyTo: String? = null): Boolean {
        val conn = connection ?: throw IllegalStateException("Not connected")

        // Sanitize all header values: strip CR/LF to prevent SMTP header injection.
        val safeFrom = from.replace("\r", "").replace("\n", "")
        val safeTo = to.replace("\r", "").replace("\n", "")
        val safeSubject = subject.replace("\r", "").replace("\n", "")
        val safeBody = body.replace("\r", "").replace("\n", "\r\n")
        val safeInReplyTo = inReplyTo?.replace("\r", "")?.replace("\n", "")

        conn.writeLine("MAIL FROM:<$safeFrom>")
        readResponse()
        conn.writeLine("RCPT TO:<$safeTo>")
        readResponse()
        conn.writeLine("DATA")
        readResponse()

        val message = buildString {
            appendLine("From: $safeFrom")
            appendLine("To: $safeTo")
            appendLine("Subject: $safeSubject")
            if (safeInReplyTo != null) {
                appendLine("In-Reply-To: $safeInReplyTo")
                appendLine("References: $safeInReplyTo")
            }
            appendLine("Content-Type: text/plain; charset=UTF-8")
            appendLine()
            appendLine(safeBody)
            appendLine(".")
        }
        conn.writeLine(message)
        val response = readResponse()
        return response.startsWith("250")
    }

    suspend fun quit() {
        try {
            val conn = connection ?: return
            conn.writeLine("QUIT")
            conn.close()
        } catch (_: Exception) {
        } finally {
            connection = null
        }
    }

    private suspend fun readResponse(): String {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = StringBuilder()
        while (true) {
            val line = conn.readLine()
            result.appendLine(line)
            if (line.length >= 4 && line[3] == ' ') break
        }
        return result.toString()
    }

    private fun encodeBase64(s: String): String {
        return java.util.Base64.getEncoder().encodeToString(s.toByteArray())
    }
}
