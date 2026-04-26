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
    private var currentJob: Job? = null
    private val _state = MutableStateFlow<SandboxState>(SandboxState.NotInstalled)
    val state: StateFlow<SandboxState> = _state

    private val sandboxDir: File
        get() = File(context.filesDir, "linux-sandbox")

    private val distrosDir: File
        get() = File(sandboxDir, "distros")

    private val activeDistroFile: File
        get() = File(sandboxDir, ".active-distro")

    val homePath: String get() = File(sandboxDir, "home").absolutePath
    val tmpPath: String get() = File(sandboxDir, "tmp").absolutePath

    val prootPath: String get() = File(context.applicationInfo.nativeLibraryDir, "libproot.so").absolutePath
    val nativeLibDir: String get() = context.applicationInfo.nativeLibraryDir

    private val downloader = RootfsDownloader(HttpClient(Android))

    @Volatile
    var activeDistro: LinuxDistro = LinuxDistro.DEFAULT
        private set

    val rootfsPath: String
        get() = File(distrosDir, "${activeDistro.id}/rootfs").absolutePath

    private fun distroRootfsDir(distro: LinuxDistro): File =
        File(distrosDir, "${distro.id}/rootfs")

    private fun distroArchiveFile(distro: LinuxDistro): File {
        val url = distro.getDownloadUrl(getLinuxArch())
        val ext = if (url.endsWith(".tar.xz")) "tar.xz" else "tar.gz"
        return File(distrosDir, "${distro.id}/rootfs.$ext")
    }

    init {
        migrateLegacyRootfs()
        loadActiveDistro()
        checkExistingInstallation()
    }

    private fun migrateLegacyRootfs() {
        val legacyRootfs = File(sandboxDir, "rootfs")
        if (legacyRootfs.isDirectory) {
            val alpineDir = distroRootfsDir(LinuxDistro.Alpine)
            if (!alpineDir.isDirectory) {
                distrosDir.mkdirs()
                try {
                    legacyRootfs.renameTo(alpineDir)
                    android.util.Log.i("LinuxSandbox", "Migrated legacy rootfs to alpine distro")
                } catch (_: Exception) {
                    legacyRootfs.copyRecursively(alpineDir, overwrite = true)
                    legacyRootfs.deleteRecursively()
                    android.util.Log.i("LinuxSandbox", "Copied legacy rootfs to alpine distro")
                }
            }
        }
    }

    private fun loadActiveDistro() {
        try {
            if (activeDistroFile.exists()) {
                val distroId = activeDistroFile.readText().trim()
                LinuxDistro.fromId(distroId)?.let {
                    activeDistro = it
                }
            }
        } catch (_: Exception) {
            // fall through to default
        }
    }

    private fun saveActiveDistro() {
        try {
            activeDistroFile.writeText(activeDistro.id)
        } catch (_: Exception) {
        }
    }

    fun setActiveDistro(distro: LinuxDistro) {
        if (distro == activeDistro) return
        activeDistro = distro
        saveActiveDistro()
        checkExistingInstallation()
    }

    fun getInstalledDistros(): List<LinuxDistro> {
        return LinuxDistro.ALL.filter { d ->
            distroRootfsDir(d).isDirectory
        }
    }

    fun getDistroStatus(distro: LinuxDistro): DistroStatus {
        val rootfsDir = distroRootfsDir(distro)
        val isReady = rootfsDir.isDirectory && prootExists()
        val diskUsageMB = if (rootfsDir.isDirectory) {
            rootfsDir.walkTopDown().sumOf { it.length() } / (1024 * 1024)
        } else 0L
        return DistroStatus(
            distro = distro,
            installed = rootfsDir.isDirectory,
            ready = isReady,
            diskUsageMB = diskUsageMB,
        )
    }

    private fun prootExists(): Boolean {
        val proot = File(prootPath)
        return proot.exists() && proot.canExecute()
    }

    private fun checkExistingInstallation() {
        val rootfs = File(rootfsPath)
        if (rootfs.isDirectory && prootExists()) {
            _state.value = SandboxState.Ready(activeDistro)
        } else if (rootfs.exists()) {
            // rootfs exists but proot missing - partial install
            _state.value = SandboxState.NotInstalled
        } else {
            _state.value = SandboxState.NotInstalled
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
                _state.value = SandboxState.Error(activeDistro, e.message ?: "Setup failed")
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        val archiveFile = distroArchiveFile(activeDistro)
        archiveFile.delete()
        val rootfs = File(rootfsPath)
        if (rootfs.isDirectory && prootExists()) {
            _state.value = SandboxState.Ready(activeDistro)
        } else {
            _state.value = SandboxState.NotInstalled
        }
    }

    private suspend fun setupInternal() {
        val arch = getLinuxArch()
        val distro = activeDistro

        val proot = File(prootPath)
        if (!proot.exists()) {
            throw IllegalStateException(
                "Proot binary not found at $prootPath. " +
                    "nativeLibraryDir contents: ${File(nativeLibDir).listFiles()?.map { it.name } ?: "empty"}",
            )
        }

        sandboxDir.mkdirs()
        distrosDir.mkdirs()
        File(sandboxDir, "home").mkdirs()
        File(sandboxDir, "tmp").mkdirs()

        copyLibtalloc()

        val rootfsDir = distroRootfsDir(distro)
        rootfsDir.parentFile?.mkdirs()

        if (!rootfsDir.isDirectory) {
            val archiveFile = distroArchiveFile(distro)
            try {
                _state.value = SandboxState.Downloading(0f)
                downloader.download(
                    url = distro.getDownloadUrl(arch),
                    targetFile = archiveFile,
                    onProgress = { progress ->
                        _state.value = SandboxState.Downloading(progress)
                    }
                )

                _state.value = SandboxState.Extracting
                downloader.extractTarArchive(archiveFile, rootfsDir)
            } finally {
                archiveFile.delete()
            }
        }

        _state.value = SandboxState.Installing(activeDistro, "Configuring...")
        downloader.makeWritable(rootfsDir)

        val executor = createProotExecutor()
        for (cmd in distro.getPostExtractCommands()) {
            if (currentJob?.isActive == false) throw kotlinx.coroutines.CancellationException()
            executor.execute(cmd, timeoutSeconds = 60)
        }

        _state.value = SandboxState.Ready(activeDistro)
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
            true
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
        val distro = activeDistro
        val packages = distro.getDefaultPackages()
        currentJob = scope.launch {
            try {
                val executor = createProotExecutor()

                // Run update first
                ensureActive()
                _state.value = SandboxState.Installing(distro, "Updating package list...")
                executor.execute(distro.getUpdateCommand(), timeoutSeconds = 60)

                for (pkg in packages) {
                    ensureActive()
                    _state.value = SandboxState.Installing(distro, "Installing $pkg...")
                    val installCmd = when (distro) {
                        is LinuxDistro.Alpine -> "apk add --no-cache $pkg"
                        is LinuxDistro.Ubuntu, is LinuxDistro.Debian -> "apt-get install -y $pkg"
                    }
                    val result = executor.execute(installCmd, timeoutSeconds = 120)
                    ensureActive()
                    val success = result["success"] as? Boolean ?: false
                    if (!success) {
                        val stderr = result["stderr"] as? String ?: ""
                        val stdout = result["stdout"] as? String ?: ""
                        val error = result["error"] as? String ?: ""
                        val timedOut = result["timed_out"] as? Boolean ?: false
                        val exitCode = result["exit_code"] as? Int ?: -1
                        android.util.Log.e("LinuxSandbox", "Failed to install $pkg on ${distro.name}: exit=$exitCode timedOut=$timedOut error=$error stdout=$stdout stderr=$stderr")
                        _state.value = SandboxState.Error(distro, "Failed to install $pkg: ${stderr.ifEmpty { error }.ifEmpty { stdout }.take(200)}")
                        return@launch
                    }
                }
                _state.value = SandboxState.Ready(distro)
            } catch (_: kotlinx.coroutines.CancellationException) {
                _state.value = SandboxState.Ready(distro)
            } catch (e: Exception) {
                android.util.Log.e("LinuxSandbox", "Package install exception on ${distro.name}", e)
                _state.value = SandboxState.Error(distro, "Install failed: ${e.message}")
            }
        }
    }

    fun reset() {
        scope.launch {
            val rootfsDir = distroRootfsDir(activeDistro)
            rootfsDir.deleteRecursively()
            checkExistingInstallation()
        }
    }

    fun resetAll() {
        scope.launch {
            distrosDir.deleteRecursively()
            activeDistroFile.delete()
            _state.value = SandboxState.NotInstalled
        }
    }

    fun getDiskUsageMB(): Long {
        if (!sandboxDir.exists()) return 0
        return sandboxDir.walkTopDown().sumOf { it.length() } / (1024 * 1024)
    }

    fun getDistroDiskUsageMB(distro: LinuxDistro): Long {
        val rootfsDir = distroRootfsDir(distro)
        if (!rootfsDir.isDirectory) return 0
        return rootfsDir.walkTopDown().sumOf { it.length() } / (1024 * 1024)
    }

    fun arePackagesInstalled(): Boolean {
        if (_state.value !is SandboxState.Ready) return false
        return File(rootfsPath, "usr/bin/python3").exists()
    }
}

data class DistroStatus(
    val distro: LinuxDistro,
    val installed: Boolean = false,
    val ready: Boolean = false,
    val diskUsageMB: Long = 0,
)
