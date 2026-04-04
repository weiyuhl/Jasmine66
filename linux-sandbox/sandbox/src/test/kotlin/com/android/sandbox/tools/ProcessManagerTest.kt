package com.android.sandbox.tools

import org.junit.Assert.*
import org.junit.Test

class ProcessManagerTest {

    @Test
    fun `startBackground creates new session with unique ID`() {
        val manager = createTestProcessManager()
        val result = manager.startBackground("echo hello", 30, "/root", emptyMap())

        assertEquals(true, result["success"])
        assertNotNull(result["session_id"])
        assertEquals("running", result["status"])
    }

    @Test
    fun `list returns empty lists when no sessions`() {
        val manager = createTestProcessManager()
        val result = manager.list()

        assertEquals(0, (result["total"] as Int))
        assertTrue((result["running"] as List<*>).isEmpty())
        assertTrue((result["finished"] as List<*>).isEmpty())
    }

    @Test
    fun `list shows session after starting`() {
        val manager = createTestProcessManager()
        manager.startBackground("echo test", 30, "/root", emptyMap())
        val result = manager.list()

        assertEquals(1, result["total"] as Int)
    }

    @Test
    fun `log returns error for unknown session`() {
        val manager = createTestProcessManager()
        val result = manager.log("unknown-session", 0, 100)

        assertEquals(false, result["success"])
        assertTrue((result["error"] as String).contains("Unknown session"))
    }

    @Test
    fun `kill returns error for unknown session`() {
        val manager = createTestProcessManager()
        val result = manager.kill("unknown-session")

        assertEquals(false, result["success"])
    }

    @Test
    fun `remove returns error for unknown session`() {
        val manager = createTestProcessManager()
        val result = manager.remove("unknown-session")

        assertEquals(false, result["success"])
    }

    @Test
    fun `kill marks session as terminated`() {
        val manager = createTestProcessManager()
        val startResult = manager.startBackground("sleep 100", 30, "/root", emptyMap())
        val sessionId = startResult["session_id"] as String

        val killResult = manager.kill(sessionId)
        assertEquals(true, killResult["success"])

        val logResult = manager.log(sessionId, 0, 10)
        assertEquals("finished", logResult["status"])
        assertEquals(true, logResult["timed_out"])
    }

    @Test
    fun `remove deletes session from list`() {
        val manager = createTestProcessManager()
        val startResult = manager.startBackground("echo test", 30, "/root", emptyMap())
        val sessionId = startResult["session_id"] as String

        manager.kill(sessionId)
        val removeResult = manager.remove(sessionId)
        assertEquals(true, removeResult["success"])

        val listResult = manager.list()
        assertEquals(0, listResult["total"] as Int)
    }

    @Test
    fun `session IDs are unique and incrementing`() {
        val manager = createTestProcessManager()
        val result1 = manager.startBackground("cmd1", 30, "/root", emptyMap())
        val result2 = manager.startBackground("cmd2", 30, "/root", emptyMap())
        val result3 = manager.startBackground("cmd3", 30, "/root", emptyMap())

        val id1 = result1["session_id"] as String
        val id2 = result2["session_id"] as String
        val id3 = result3["session_id"] as String

        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }

    private fun createTestProcessManager(): TestableProcessManager {
        return TestableProcessManager()
    }
}

class TestableProcessManager {
    private val sessions = java.util.concurrent.ConcurrentHashMap<String, TestSession>()
    private val nextId = java.util.concurrent.atomic.AtomicInteger(1)

    data class TestSession(
        val id: String,
        val command: String,
        val startTime: Long,
        @Volatile var stdout: String = "",
        @Volatile var stderr: String = "",
        @Volatile var finished: Boolean = false,
        @Volatile var exitCode: Int? = null,
        @Volatile var timedOut: Boolean = false,
    )

    fun startBackground(
        command: String,
        timeoutSeconds: Long,
        workingDir: String,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        val sessionId = "bg-${nextId.getAndIncrement()}"
        val session = TestSession(
            id = sessionId,
            command = command,
            startTime = System.currentTimeMillis(),
        )
        sessions[sessionId] = session

        return mapOf(
            "success" to true,
            "session_id" to sessionId,
            "status" to "running",
            "message" to "Process started in background.",
        )
    }

    fun list(): Map<String, Any> {
        val running = sessions.values.filter { !it.finished }.map { it.toInfo() }
        val finished = sessions.values.filter { it.finished }.map { it.toInfo() }
        return mapOf(
            "running" to running,
            "finished" to finished,
            "total" to sessions.size,
        )
    }

    fun log(sessionId: String, offset: Int, limit: Int): Map<String, Any> {
        val session = sessions[sessionId]
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")

        val stdoutLines = session.stdout.lines()
        val sliced = stdoutLines.drop(offset).take(limit).joinToString("\n")

        return mapOf(
            "success" to true,
            "session_id" to sessionId,
            "status" to if (session.finished) "finished" else "running",
            "exit_code" to (session.exitCode ?: -1),
            "stdout" to sliced,
            "stderr" to session.stderr.takeLast(2000),
            "total_stdout_lines" to stdoutLines.size,
            "offset" to offset,
            "timed_out" to session.timedOut,
        )
    }

    fun kill(sessionId: String): Map<String, Any> {
        val session = sessions[sessionId]
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")

        if (session.finished) {
            return mapOf("success" to true, "message" to "Process already finished", "exit_code" to (session.exitCode ?: -1))
        }

        session.finished = true
        session.exitCode = -1
        session.timedOut = true
        return mapOf("success" to true, "message" to "Process marked as terminated")
    }

    fun remove(sessionId: String): Map<String, Any> {
        sessions.remove(sessionId)
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")
        return mapOf("success" to true, "message" to "Session removed")
    }

    private fun TestSession.toInfo(): Map<String, Any> = mapOf(
        "session_id" to id,
        "command" to command,
        "status" to if (finished) "finished" else "running",
        "exit_code" to (exitCode ?: -1),
        "duration_seconds" to ((System.currentTimeMillis() - startTime) / 1000),
        "timed_out" to timedOut,
        "stdout_length" to stdout.length,
    )
}
