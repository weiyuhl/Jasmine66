package com.android.sandbox.core

import java.io.BufferedReader
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val MAX_OUTPUT_LENGTH = 15_000
private const val DEFAULT_TIMEOUT_SECONDS = 30L
private const val MAX_TIMEOUT_SECONDS = 180L

/**
 * PRoot-based executor for Linux sandbox commands.
 *
 * ## How PRoot works (ptrace-based chroot simulation)
 *
 * PRoot uses `ptrace(PTRACE_SYSCALL)` to trace every system call of the
 * guest process. When it detects a syscall that manipulates the filesystem
 * (e.g. `chroot`, `execve`, `open`), it intercepts and rewrites path
 * arguments to redirect them into the rootfs — all without real `chroot`
 * or `mount` privileges. This is how it provides a full Linux userspace
 * on unrooted Android.
 *
 * Key ptrace interception points:
 * - `execve` / `execveat` — rewrites executable paths into rootfs
 * - `open` / `openat` / `stat` — redirects file access into rootfs
 * - `chdir` / `getcwd` — maintains the illusion of a real root
 * - `link` / `symlink` / `unlink` — `--link2symlink` converts hardlinks to symlinks
 *
 * Performance: On kernels 5.10+, ptrace overhead is ~5-15% for most
 * workloads. io-heavy commands see less impact, CPU-heavy see more.
 */
class ProotExecutor(
    private val prootPath: String,
    private val libDir: String,
    private val rootfsPath: String,
    private val homePath: String,
    private val tmpPath: String,
    private val hasExternalStorageAccess: Boolean = false,
) {

    companion object {
        private val ioExecutor: ExecutorService = Executors.newCachedThreadPool { r ->
            Thread(r, "proot-io").apply { isDaemon = true }
        }

        fun shutdown() {
            ioExecutor.shutdown()
            try {
                if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    ioExecutor.shutdownNow()
                }
            } catch (_: InterruptedException) {
                ioExecutor.shutdownNow()
            }
        }
    }

    fun execute(
        command: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        workingDir: String = "/root",
        extraEnv: Map<String, String> = emptyMap(),
        loginShell: Boolean = false,
        killOnExit: Boolean = true,
    ): Map<String, Any> {
        val effectiveTimeout = timeoutSeconds.coerceIn(1, MAX_TIMEOUT_SECONDS)

        // --- Build PRoot arguments ---
        val baseArgs = mutableListOf(
            prootPath,
            "--link2symlink",
            "--rootfs=$rootfsPath",
        )

        // kill-on-exit: when PRoot exits, kill all child processes in the
        // traced process tree. Prevents orphan daemons and zombie shells.
        if (killOnExit) {
            baseArgs.add("--kill-on-exit")
        }

        // NOTE: fake root (UID 0) is handled by the `-0` flag in processArgs
        // below. Don't also add --root-id here, they're the same option.

        // Core filesystem binds — these provide the Linux pseudo-filesystems
        // that most programs expect to find at runtime.
        baseArgs.addAll(listOf(
            "--bind=/dev",        // /dev, /dev/pts, /dev/urandom, etc.
            "--bind=/proc",       // CPU info, meminfo, self/
            "--bind=/sys",        // kernel params, devices
            "--bind=$homePath:/root",   // persistent home across sessions
            "--bind=$tmpPath:/tmp",     // writable tmp
        ))

        // /dev/shm — shared memory, needed by Python multiprocessing,
        // some npm packages, and glibc pthreads.
        // Only bind if host Android has /dev/shm AND we can create it in rootfs.
        if (File("/dev/shm").isDirectory) {
            val devShm = File(rootfsPath, "dev/shm")
            devShm.mkdirs()
            baseArgs.add("--bind=/dev/shm")
        }

        // Host filesystem access — bind the Android root as /host-rootfs (read-only)
        // so users can access /sdcard, /data, etc. from inside the chroot.
        // e.g. `ls /host-rootfs/sdcard/` shows Android's shared storage.
        baseArgs.add("--bind=/:/host-rootfs:ro")

        // External storage — full read/write access when permission granted.
        if (hasExternalStorageAccess) {
            baseArgs.add("--bind=/storage/emulated/0")
            baseArgs.add("--bind=/sdcard")
        }

        // Shell and command: use login shell mode (-l) if requested,
        // which sources /etc/profile and ~/.profile for proper env setup.
        val shellCmd = if (loginShell) "/bin/sh -l -c" else "/bin/sh -c"
        val processArgs = (baseArgs + listOf("-0", "-w", workingDir, *shellCmd.split(" ").toTypedArray(), command)).toTypedArray()

        // --- Build environment ---
        val loaderPath = File(prootPath).parent.orEmpty() + "/libproot-loader.so"
        val baseEnv = mutableListOf(
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=C.UTF-8",
            "LD_LIBRARY_PATH=$libDir",
            "PROOT_TMP_DIR=$tmpPath",
            "PROOT_LOADER=$loaderPath",
            "ANDROID_ROOT=/",
            "ANDROID_DATA=/data",
            // Prevent some programs from trying to use systemd/logind
            "container=jasmine",
        )

        // SHELL variable — detect bash if installed
        val bashPath = File(rootfsPath, "usr/bin/bash")
        baseEnv.add(if (bashPath.exists()) "SHELL=/bin/bash" else "SHELL=/bin/sh")

        val envVars = (baseEnv + extraEnv.map { (k, v) -> "$k=$v" }).toTypedArray()

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(processArgs, envVars, File(rootfsPath).parentFile)

            val stdoutFuture = CompletableFuture.supplyAsync({
                readBounded(process.inputStream.bufferedReader())
            }, ioExecutor)
            val stderrFuture = CompletableFuture.supplyAsync({
                readBounded(process.errorStream.bufferedReader())
            }, ioExecutor)

            val completed = process.waitFor(effectiveTimeout, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                val stdout = tryGet(stdoutFuture).smartTruncate(MAX_OUTPUT_LENGTH)
                val stderr = tryGet(stderrFuture).smartTruncate(MAX_OUTPUT_LENGTH)
                return mapOf(
                    "success" to false,
                    "stdout" to stdout,
                    "stderr" to stderr,
                    "exit_code" to -1,
                    "timed_out" to true,
                )
            }

            mapOf(
                "success" to (process.exitValue() == 0),
                "stdout" to stdoutFuture.get().smartTruncate(MAX_OUTPUT_LENGTH),
                "stderr" to stderrFuture.get().smartTruncate(MAX_OUTPUT_LENGTH),
                "exit_code" to process.exitValue(),
                "timed_out" to false,
            )
        } catch (e: Exception) {
            process?.destroyForcibly()
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Failed to execute command in sandbox"),
            )
        }
    }

    private fun tryGet(future: java.util.concurrent.Future<String>): String {
        return try {
            future.get(1, TimeUnit.SECONDS)
        } catch (_: Exception) {
            ""
        }
    }

    private fun readBounded(reader: BufferedReader): String {
        val sb = StringBuilder()
        val buf = CharArray(8192)
        var read: Int
        while (reader.read(buf).also { read = it } != -1) {
            sb.append(buf, 0, read)
            if (sb.length >= MAX_OUTPUT_LENGTH) break
        }
        if (sb.length >= MAX_OUTPUT_LENGTH) {
            while (reader.read(buf) != -1) { /* discard */ }
        }
        return sb.toString()
    }

    private fun String.smartTruncate(maxLength: Int): String {
        if (length <= maxLength) return this
        val keep = (maxLength - 80) / 2
        return take(keep) +
            "\n[... ${length - 2 * keep} characters truncated ...]\n" +
            takeLast(keep)
    }
}
