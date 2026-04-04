# Linux Sandbox Android

一个独立的 Android 模块，提供基于 proot 的 Linux 沙盒环境。

## 功能

- 在非 root 的 Android 设备上运行完整的 Alpine Linux 环境
- 支持 aarch64、armhf、x86_64 架构
- 可执行 shell 命令、脚本、Python、Node.js 等
- 支持后台进程管理

## 集成方式

### 1. 添加依赖

将此模块作为本地依赖添加到你的项目中：

```kotlin
// settings.gradle.kts
include(":sandbox")
project(":sandbox").projectDir = file("path/to/linux-sandbox-android/sandbox")
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":sandbox"))
}
```

### 2. 初始化 Koin

在你的 Application 类中添加 sandboxModule：

```kotlin
import com.android.sandbox.di.sandboxModule
import org.koin.android.ext.koin.androidContext

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(sandboxModule)
        }
    }
}
```

### 3. 使用

```kotlin
import com.android.sandbox.core.LinuxSandboxManager
import com.android.sandbox.core.SandboxState
import org.koin.java.KoinJavaComponent.inject

val sandboxManager: LinuxSandboxManager by inject(LinuxSandboxManager::class.java)

// 观察状态
lifecycleScope.launch {
    sandboxManager.state.collect { state ->
        when (state) {
            is SandboxState.NotInstalled -> // 未安装
            is SandboxState.Downloading -> // 下载中 (state.progress)
            is SandboxState.Extracting -> // 解压中
            is SandboxState.Installing -> // 安装中 (state.detail)
            is SandboxState.Ready -> // 就绪
            is SandboxState.Error -> // 错误 (state.message)
        }
    }
}

// 安装沙盒
sandboxManager.setup()

// 安装额外包
sandboxManager.installPackages()

// 执行命令
val executor = sandboxManager.createProotExecutor()
val result = executor.execute("ls -la")

// 重置
sandboxManager.reset()
```

## 构建 proot 原生库

```bash
cd /path/to/original/project
./build-proot.sh
```

然后将生成的 `.so` 文件复制到 `sandbox/src/main/jniLibs/`。

## 许可证

proot 来源于 [termux/proot](https://github.com/termux/proot)，遵循其原始许可证。
