package com.android.sandbox.core

import android.content.Context
import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
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
    private var currentJob: Job? = null
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
        val rootfs = File(sandboxDir, "rootfs")
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                checkExistingInstallation()
            } catch (e: Exception) {
                _state.value = SandboxState.Error(e.message ?: "Setup failed")
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        File(sandboxDir, "rootfs.tar.gz").delete()
        val rootfs = File(sandboxDir, "rootfs")
        if (rootfs.isDirectory && File(prootPath).exists()) {
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

        val rootfsDir = File(sandboxDir, "rootfs")
        if (!rootfsDir.isDirectory) {
            val tarGzFile = File(sandboxDir, "rootfs.tar.gz")
            try {
                _state.value = SandboxState.Downloading(0f)
                downloader.download(arch, tarGzFile) { progress ->
                    _state.value = SandboxState.Downloading(progress)
                }

                _state.value = SandboxState.Extracting
                downloader.extractTarGz(tarGzFile, rootfsDir)
            } finally {
                tarGzFile.delete()
            }
        }

        _state.value = SandboxState.Installing("Configuring...")
        downloader.makeWritable(rootfsDir)
        downloader.writeResolvConf(rootfsDir)

        val executor = createProotExecutor()
        executor.execute("apk update", timeoutSeconds = 60)

        _state.value = SandboxState.Ready
    }

    private fun copyLibtalloc() {
        val tallocTarget = File(sandboxDir, "libtalloc.so.2")
        if (tallocTarget.exists()) return

        val source = File(nativeLibDir, "libtalloc.so")
        if (source.exists()) {
            source.copyTo(tallocTarget, overwrite = true)
        }
    }

    fun createProotExecutor(): ProotExecutor {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true // Prior to R, permissions are handled by manifest on install
        }
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
        val packages = listOf("bash", "curl", "wget", "git", "jq", "python3", "py3-pip", "nodejs")
        currentJob = scope.launch {
            try {
                val executor = createProotExecutor()
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
                        val timedOut = result["timed_out"] as? Boolean ?: false
                        val exitCode = result["exit_code"] as? Int ?: -1
                        android.util.Log.e("LinuxSandbox", "Failed to install $pkg: exit=$exitCode timedOut=$timedOut error=$error stdout=$stdout stderr=$stderr")
                        _state.value = SandboxState.Error("Failed to install $pkg: ${stderr.ifEmpty { error }.ifEmpty { stdout }.take(200)}")
                        return@launch
                    }
                }
                _state.value = SandboxState.Ready
            } catch (_: kotlinx.coroutines.CancellationException) {
                _state.value = SandboxState.Ready
            } catch (e: Exception) {
                android.util.Log.e("LinuxSandbox", "Package install exception", e)
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
}
