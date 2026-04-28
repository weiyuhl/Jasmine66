package com.android.sandbox.tools

import com.android.sandbox.core.LinuxSandboxManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ProcessManager(private val sandboxManager: LinuxSandboxManager) {

    class Session(
        val id: String,
        val command: String,
        val startTime: Long,
        @Volatile var stdout: String = "",
        @Volatile var stderr: String = "",
        @Volatile var finished: Boolean = false,
        @Volatile var exitCode: Int? = null,
        @Volatile var timedOut: Boolean = false,
        internal var future: CompletableFuture<Void>? = null,
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    private val nextId = AtomicInteger(1)

    companion object {
        private const val FINISHED_SESSION_TTL_MS = 30 * 60 * 1000L // 30 min
        private const val MAX_SESSIONS = 200
        private val backgroundExecutor: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newCachedThreadPool { r ->
                Thread(r, "sandbox-bg").apply { isDaemon = true }
            }
    }

    /** Evict finished sessions older than TTL, and trim oldest if over max. */
    private fun evictIfNeeded() {
        val now = System.currentTimeMillis()
        val toRemove = sessions.entries.filter { (_, s) ->
            s.finished && (now - s.startTime) > FINISHED_SESSION_TTL_MS
        }
        toRemove.forEach { sessions.remove(it.key) }

        // Trim oldest finished sessions if still over limit
        if (sessions.size > MAX_SESSIONS) {
            val sorted = sessions.entries
                .filter { it.value.finished }
                .sortedBy { it.value.startTime }
            val toDrop = sorted.take(sessions.size - MAX_SESSIONS)
            toDrop.forEach { sessions.remove(it.key) }
        }
    }

    fun startBackground(
        command: String,
        timeoutSeconds: Long,
        workingDir: String,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        evictIfNeeded()
        val sessionId = "bg-${nextId.getAndIncrement()}"
        val session = Session(
            id = sessionId,
            command = command,
            startTime = System.currentTimeMillis(),
        )
        sessions[sessionId] = session

        val executor = sandboxManager.createProotExecutor()
        session.future = CompletableFuture.runAsync({
            val result = executor.execute(command, timeoutSeconds, workingDir, envMap)
            session.stdout = result["stdout"] as? String ?: ""
            session.stderr = result["stderr"] as? String ?: ""
            session.exitCode = result["exit_code"] as? Int ?: -1
            session.timedOut = result["timed_out"] as? Boolean ?: false
            session.finished = true
        }, backgroundExecutor)

        return mapOf(
            "success" to true,
            "session_id" to sessionId,
            "status" to "running",
            "message" to "Process started in background. Use manage_process tool to check status.",
        )
    }

    fun list(): Map<String, Any> {
        evictIfNeeded()
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

        session.future?.cancel(true)
        session.finished = true
        session.exitCode = -1
        session.timedOut = true

        // Attempt to kill the actual command via pkill inside the sandbox.
        // Escape single quotes in the command to prevent shell injection.
        val escaped = session.command.replace("'", "'\"'\"'").take(200)
        try {
            val exec = sandboxManager.createProotExecutor()
            exec.execute("pkill -f '$escaped'", timeoutSeconds = 5L)
        } catch (_: Exception) { }

        return mapOf("success" to true, "message" to "Process terminated")
    }

    fun remove(sessionId: String): Map<String, Any> {
        sessions.remove(sessionId)
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")
        return mapOf("success" to true, "message" to "Session removed")
    }

    private fun Session.toInfo(): Map<String, Any> = mapOf(
        "session_id" to id,
        "command" to command,
        "status" to if (finished) "finished" else "running",
        "exit_code" to (exitCode ?: -1),
        "duration_seconds" to ((System.currentTimeMillis() - startTime) / 1000),
        "timed_out" to timedOut,
        "stdout_length" to stdout.length,
    )
}
