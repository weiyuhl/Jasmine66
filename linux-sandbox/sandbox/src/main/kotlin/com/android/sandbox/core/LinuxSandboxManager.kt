package com.android.sandbox.core

import android.content.Context
import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class LinuxSandboxManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var currentJob: Job? = null
    private val _state = MutableStateFlow<SandboxState>(SandboxState.NotInstalled)
    val state: StateFlow<SandboxState> = _state

    private val sandboxDir: File
        get() = File(context.filesDir, "linux-sandbox")

    val rootfsPath: String get() = File(sandboxDir, "rootfs").absolutePath
    val homePath: String get() = File(sandboxDir, "home").absolutePath
    val tmpPath: String get() = File(sandboxDir, "tmp").absolutePath

    val prootPath: String get() = File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath
    val nativeLibDir: String get() = context.applicationInfo.nativeLibraryDir

    private val downloader = RootfsDownloader(HttpClient(Android))

    init {
        checkExistingInstallation()
    }

    private fun checkExistingInstallation() {
        val rootfs = File(rootfsPath)
        val proot = File(prootPath)
        if (rootfs.isDirectory && proot.exists() && proot.canExecute()) {
            _state.value = SandboxState.Ready
        }
    }

    private fun getLinuxArch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.startsWith("arm64") -> "aarch64"
            abi.startsWith("armeabi") -> "armhf"
            abi.startsWith("x86_64") -> "x86_64"
            abi.startsWith("x86") -> "x86"
            else -> "aarch64"
        }
    }

    fun setup() {
        if (currentJob?.isActive == true) return
        currentJob = scope.launch {
            try {
                setupInternal()
            } catch (e: CancellationException) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    checkExistingInstallation()
                }
                throw e
            } catch (e: Exception) {
                _state.value = SandboxState.Error(e.message ?: "Setup failed")
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        File(sandboxDir, "rootfs.tar.gz").delete()
        if (File(rootfsPath).isDirectory && prootExists()) {
            _state.value = SandboxState.Ready
        } else {
            _state.value = SandboxState.NotInstalled
        }
    }

    private suspend fun setupInternal() {
        val arch = getLinuxArch()

        val proot = File(prootPath)
        if (!proot.exists()) {
            throw IllegalStateException(
                "Proot binary not found at $prootPath. " +
                    "nativeLibraryDir contents: ${File(nativeLibDir).listFiles()?.map { it.name } ?: "empty"}",
            )
        }

        sandboxDir.mkdirs()
        File(sandboxDir, "home").mkdirs()
        File(sandboxDir, "tmp").mkdirs()

        copyLibtalloc()

        val rootfsDir = File(rootfsPath)
        val isFreshExtract = !rootfsDir.isDirectory
        if (isFreshExtract) {
            val tarGzFile = File(sandboxDir, "rootfs.tar.gz")
            try {
                _state.value = SandboxState.Downloading(0f)
                downloader.download(
                    url = AlpineInfo.getDownloadUrl(arch),
                    targetFile = tarGzFile,
                    onProgress = { progress -> _state.value = SandboxState.Downloading(progress) }
                )

                _state.value = SandboxState.Installing("Verifying checksum...")
                verifyRootfsChecksum(tarGzFile, arch)

                _state.value = SandboxState.Extracting
                downloader.extractTarGz(tarGzFile, rootfsDir)

                // Handle wrapped tarballs (single subdirectory)
                unwrapSingleSubdir(rootfsDir)

                // Smoke-test: verify PRoot can execute a command in the new rootfs
                _state.value = SandboxState.Installing("Verifying rootfs...")
                val testResult = createProotExecutor().execute("echo jasmine-smoke-test", timeoutSeconds = 15)
                if ((testResult["stdout"] as? String)?.contains("jasmine-smoke-test") != true) {
                    val error = testResult["error"] as? String
                    val stderr = testResult["stderr"] as? String ?: ""
                    throw IllegalStateException("Rootfs smoke test failed: ${error ?: stderr.ifEmpty { "unknown" }.take(300)}")
                }
            } finally {
                tarGzFile.delete()
            }
        }

        _state.value = SandboxState.Installing("Configuring...")
        downloader.makeWritable(rootfsDir)

        fixResolvConf(rootfsDir)

        val executor = createProotExecutor()
        for (cmd in AlpineInfo.POST_EXTRACT_COMMANDS) {
            if (currentJob?.isActive == false) throw CancellationException("Cancelled")
            val result = executor.execute(cmd, timeoutSeconds = 120)
            val success = result["success"] as? Boolean ?: false
            if (!success) {
                val error = result["error"] as? String
                val stderr = result["stderr"] as? String ?: ""
                val stdout = result["stdout"] as? String ?: ""
                throw IllegalStateException(
                    "Post-extract command failed: $cmd\n" +
                    "Error: ${error ?: stderr.ifEmpty { stdout }.take(300)}"
                )
            }
        }

        writeInitFiles(rootfsDir)

        _state.value = SandboxState.Ready
    }

    private fun verifyRootfsChecksum(tarGzFile: File, arch: String) {
        try {
            val checksumUrl = AlpineInfo.getChecksumUrl(arch)
            val expectedSha256 = downloader.downloadText(checksumUrl).trim().substringBefore(' ')
            if (expectedSha256.length < 64) {
                android.util.Log.w("LinuxSandbox", "Checksum file malformed, skipping verification")
                return
            }
            val actualSha256 = java.security.MessageDigest.getInstance("SHA-256")
                .digest(tarGzFile.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (!expectedSha256.equals(actualSha256, ignoreCase = true)) {
                android.util.Log.e("LinuxSandbox", "SHA256 mismatch!\nExpected: $expectedSha256\nActual: $actualSha256")
                throw IllegalStateException("Rootfs checksum verification failed")
            }
            android.util.Log.i("LinuxSandbox", "SHA256 checksum verified OK")
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("LinuxSandbox", "Checksum verification skipped: ${e.message}")
        }
    }

    private fun unwrapSingleSubdir(rootfsDir: File) {
        val children = rootfsDir.listFiles() ?: return
        if (children.size != 1) return
        val child = children.single()
        if (!child.isDirectory) return

        android.util.Log.i("LinuxSandbox", "Detected wrapper directory: ${child.name}; flattening")

        val tmpDir = File(rootfsDir.parentFile, "${rootfsDir.name}_tmp")
        try {
            tmpDir.deleteRecursively()
            if (!child.renameTo(tmpDir)) {
                child.copyRecursively(tmpDir, overwrite = true)
                child.deleteRecursively()
            }
            rootfsDir.deleteRecursively()
            if (!tmpDir.renameTo(rootfsDir)) {
                tmpDir.copyRecursively(rootfsDir, overwrite = true)
                tmpDir.deleteRecursively()
            }
        } catch (e: Exception) {
            android.util.Log.w("LinuxSandbox", "Failed to flatten wrapper: ${e.message}")
            tmpDir.deleteRecursively()
        }
    }

    private fun fixResolvConf(rootfsDir: File) {
        try {
            val resolvConf = File(rootfsDir, "etc/resolv.conf")
            resolvConf.delete()
            resolvConf.parentFile?.mkdirs()
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")
        } catch (e: Exception) {
            android.util.Log.w("LinuxSandbox", "Failed to write resolv.conf", e)
        }
    }

    private fun writeInitFiles(rootfsDir: File) {
        AlpineInfo.INIT_FILES.forEach { (relPath, content) ->
            try {
                val file = File(rootfsDir, relPath)
                file.parentFile?.mkdirs()
                if (!file.exists()) file.writeText(content)
            } catch (e: Exception) {
                android.util.Log.w("LinuxSandbox", "Failed to write init file: $relPath", e)
            }
        }
    }

    private fun copyLibtalloc() {
        val tallocTarget = File(sandboxDir, "libtalloc.so.2")
        if (tallocTarget.exists()) return
        val source = File(nativeLibDir, "libtalloc.so")
        if (source.exists()) source.copyTo(tallocTarget, overwrite = true)
    }

    fun createProotExecutor(): ProotExecutor {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else true
        return ProotExecutor(
            prootPath = prootPath,
            libDir = sandboxDir.absolutePath,
            rootfsPath = rootfsPath,
            homePath = homePath,
            tmpPath = tmpPath,
            hasExternalStorageAccess = hasStorage
        )
    }

    fun installPackages() {
        if (currentJob?.isActive == true) return
        val packages = AlpineInfo.DEFAULT_PACKAGES
        currentJob = scope.launch {
            try {
                val executor = createProotExecutor()
                ensureActive()
                _state.value = SandboxState.Installing("Updating package list...")
                executor.execute(AlpineInfo.getUpdateCommand(), timeoutSeconds = 60)

                for (pkg in packages) {
                    ensureActive()
                    _state.value = SandboxState.Installing("Installing $pkg...")
                    val result = executor.execute("apk add --no-cache $pkg", timeoutSeconds = 120)
                    ensureActive()
                    val success = result["success"] as? Boolean ?: false
                    if (!success) {
                        val stderr = result["stderr"] as? String ?: ""
                        val stdout = result["stdout"] as? String ?: ""
                        val error = result["error"] as? String ?: ""
                        _state.value = SandboxState.Error("Failed to install $pkg: ${stderr.ifEmpty { error }.ifEmpty { stdout }.take(200)}")
                        return@launch
                    }
                }
                _state.value = SandboxState.Ready
            } catch (_: CancellationException) {
                _state.value = SandboxState.Ready
            } catch (e: Exception) {
                _state.value = SandboxState.Error("Install failed: ${e.message}")
            }
        }
    }

    fun reset() {
        scope.launch {
            sandboxDir.deleteRecursively()
            _state.value = SandboxState.NotInstalled
        }
    }

    fun getDiskUsageMB(): Long {
        if (!sandboxDir.exists()) return 0
        return sandboxDir.walkTopDown().sumOf { it.length() } / (1024 * 1024)
    }

    fun arePackagesInstalled(): Boolean {
        if (_state.value !is SandboxState.Ready) return false
        return File(rootfsPath, "usr/bin/python3").exists()
    }

    private fun prootExists(): Boolean {
        val proot = File(prootPath)
        return proot.exists() && proot.canExecute()
    }

    fun destroy() {
        currentJob?.cancel()
        scope.cancel()
        downloader.close()
    }
}
