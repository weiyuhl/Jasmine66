# Jasmine 项目全面问题分析报告

> 生成日期: 2026-04-28
> 分析范围: 全部模块 (267+ Kotlin 文件, 构建配置, 技能脚本, 配置文件)
> 分析方法: 代码静态审查（未参考 git 历史）

---

> 注: 原始分析中 C1~C2、C8 位于 `jasmine-core/assistant/*` 和 `jasmine-core/rag/*` 未注册模块中。
> 经核实，`core/`、`feature/`、`app/` 及已注册的 `jasmine-core/` 模块均不依赖这些模块，
> 它们未被 App 实际使用，无需注册，相关编译错误不影响项目构建。已从报告中移除。

## 一、CRITICAL（严重 — 共 5 项）

### C1. `SkillManager.kt` 非线程安全 — 数据竞争

**文件:** `core/domain/src/main/kotlin/.../SkillManager.kt`

| 行号 | 问题 |
|------|------|
| 63-64 | `private val skills = mutableListOf<Skill>()` — 普通 MutableList，多协程并发读写无保护 |
| 69-70 | `if (isLoaded)` 守卫 — 典型 TOCTOU 竞态，两个协程可同时通过检查并重复执行加载 |
| 124-126 | `getAllSkills()` 和 `getSelectedSkills()` 在无锁情况下遍历 `skills`，并发修改抛 `ConcurrentModificationException` |

---

### C2. `FileLogger.kt` 文件写入无同步

**文件:** `core/data/src/main/kotlin/.../FileLogger.kt:30-45`

`log()` 可被任何线程/协程调用。多个并发的 `FileWriter` 写同一文件将产生损坏的日志输出。无 `Mutex`、`synchronized` 或单线程调度器。

---

### C3. `ChatClientManager.kt` 竞态条件

**文件:** `core/data/src/main/kotlin/.../ChatClientManager.kt`

| 行号 | 问题 |
|------|------|
| 42, 73 | `refreshState()` 在 `stateMutex` 保护下写入 `chatClient` |
| 140 | `streamChat()` 读取 `chatClient` **不加** `stateMutex`，并发的 `refreshState()` 可能关闭正在流式传输的连接 |

---

### C4. linux-sandbox Gradle 版本不存在

**文件:** `linux-sandbox/gradle/wrapper/gradle-wrapper.properties:3`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
```

Gradle 9.2.1 不存在。sandbox 子项目的所有构建会因下载失败而崩溃。

---

### C5. `ProcessManager.kill()` 假杀死 — 僵尸进程泄漏

**文件:** `linux-sandbox/sandbox/src/main/kotlin/.../ProcessManager.kt:86-98`

```kotlin
fun kill(...) {
    session.finished = true
    session.exitCode = -1
    session.timedOut = true
    // 实际不终止进程！只标记了数据对象
}
```

后台进程持续运行并消耗系统资源（CPU、内存）。

---

## 二、HIGH（高危 — 共 24 项）

### 安全漏洞

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| S1 | `keystore.properties` | 全部 | 密钥密码明文存储（`jasmine2026`），密码强度低，store/key 密码相同 |
| S2 | `jasmine-release.jks` | — | 发布密钥库在项目根目录 |
| S3 | `app/build.gradle.kts` | 44-46 | Release 构建找不到密钥时静默降级为 debug 签名 |
| S4 | `app-jasmine-catalog/build.gradle.kts` | 27 | Release 硬编码使用 debug 签名 |
| S5 | `skills/.../calculate-hash/.../index.js` | 19 | 使用 SHA-1（已被破解） |
| S6 | `skills/.../restaurant-roulette/.../index.js` | 43-45 | Gemini API Key 通过 URL 查询参数传递 |
| S7 | 多个 skills HTML | — | 5 个文件从 CDN 加载 JS 无 SRI 校验（integrity 属性） |

### 内存 / 资源泄漏

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| S8 | `feature/chat/impl/.../ChatScreen.kt` | 114-125 | WebView 创建无 `onRelease`/`destroy()`，每次导航累积泄漏原生内存 |
| S9 | `app/di/JankStatsModule.kt` | 29-32 | `JankStats` 从未调用 `destroy()`，Activity 销毁后仍持有 Window 引用 |
| S10 | `jasmine-core/.../SseMcpClient.kt` | 57 | `OkHttpClient` 创建后从不 `close()` |
| S10b | `jasmine-core/.../HttpMcpClient.kt` | 42 | 同上 |
| S11 | `websearch/.../DuckDuckGoSearchService.kt` | 61-62 | `OkHttp Response` 不 close，socket 连接泄漏 |
| S12 | `linux-sandbox/.../LinuxSandboxManager.kt` | 20 | `CoroutineScope` 从不 cancel，实例销毁后协程继续运行 |
| S12b | `linux-sandbox/.../AndroidSandboxController.kt` | 16 | 同上 |
| S13 | `linux-sandbox/.../LinuxSandboxManager.kt` | 35 | Ktor `HttpClient` 不 close |

### 崩溃风险

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| S14 | `core/navigation/.../Navigator.kt` | 31 | 根页面按返回键调用 `error()` 直接 crash 应用 |
| S15 | `core/navigation/.../NavigationState.kt` | 57-59 | 子栈 key 不存在时 `error()` crash |
| S16 | `core/database/.../DatabaseModule.kt` | 19-26 | Room 数据库无 `fallbackToDestructiveMigration()`，schema 版本变化时全量用户 crash |

### 逻辑 / 数据错误

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| S17 | `jasmine-core/.../AgentSubgraph.kt` | 35-92 | `while(true)` 无循环检测、无最大迭代保护，有环图导致死循环 |
| S18 | `jasmine-core/.../Tool.kt` | 35-37 | 工具异常被 `catch` 吞掉，`ToolCallFailed` trace 永远不会触发 |
| S19 | `jasmine-core/.../SseMcpClient.kt` | 187-190 | SSE 监听中所有非取消异常静默吞掉 |
| S20 | `jasmine-core/.../SseMcpClient.kt` | 303 | `deferred.await()` 无超时，SSE 响应丢失时协程永远挂起 |
| S21 | `feature/search/impl/.../SearchViewModel.kt` | 27-38 | 搜索功能未实现，始终返回 `Success` |
| S22 | `core/domain/.../SkillManager.kt` | 49 | `MutableSet<*>` 不安全转换为 `Set<String>`，会抛出 ClassCastException |
| S23 | `core/data/.../ChatMessageBuilder.kt` | 33-34 | 硬编码字符串 `"No active skills."` 比较，SkillManager 修改后静默失效 |

### 协程问题

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| S24 | `jasmine-core/.../ExecuteShellCommandTool.kt` | 210 | `Thread.sleep()` 在 suspend 函数中阻塞 IO dispatcher 线程 |

---

## 三、MEDIUM（中危 — 共 28 项）

### 协程 / 线程安全 (7 项)

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| M1 | `feature/chat/impl/.../ChatViewModel.kt` | 106-172 | 非原子 read-modify-write 在 StateFlow 上导致消息丢失 |
| M2 | `feature/chat/impl/.../ChatViewModel.kt` | 143,229 | CancellationException 被 catch 吞掉，违反结构化并发 |
| M3 | `feature/chat/impl/.../ChatViewModel.kt` | 119,177,294 | `streamJob` 被覆盖前不 cancel 旧 job |
| M4 | `jasmine-core/.../McpConnectionManager.kt` | 74-75 | @Volatile 字段 check-then-act 竞态条件 |
| M5 | `feature/sandbox/impl/.../SandboxScreen.kt` | 382 | 非主线程 dispatcher 修改 `SnapshotStateList` |
| M6 | `core/data/.../ConnectivityManagerNetworkMonitor.kt` | 60-66 | 注册回调顺序导致初始连接状态可能过时 |
| M7 | `jasmine-core/.../AgentPipeline.kt` | 245-260 | `getOrPut` 在 MutableMap 上非原子使用 |

### 错误处理 / 逻辑漏洞 (9 项)

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| M8 | `feature/sandbox/impl/.../SandboxViewModel.kt` | 92 | 空 catch 块 `catch (_: Exception) { }`，静默失败 |
| M9 | `feature/sandbox/impl/.../SandboxViewModel.kt` | 100 | `getExecutor()` 非 suspend，可能在主线程阻塞 I/O |
| M10 | `feature/chat/impl/.../ChatViewModel.kt` | 119-151 | `onUiCallback` 缺少 tool call 回调，UI 流不显示工具进度 |
| M11 | `jasmine-core/.../ChatResult.kt` | 31-49 | `toAssistantMessage()` 静默丢弃 tool call 信息 |
| M12 | `core/data/.../SkillJsSandbox.kt` | 152 | URL 编码路径穿越 (`%2e%2e` 可能绕过 `..` 检测) |
| M13 | `core/data/.../DeviceControlTool.kt` | 173-178 | 日期解析失败静默回退到当前时间 |
| M14 | `core/data/.../DeviceControlTool.kt` | 121 | 不安全的 `as CameraManager` 转换 |
| M15 | `linux-sandbox/.../SandboxToolAdapter.kt` | 69,127 | ProcessManagerTool 和 ExecuteShellCommandTool 使用独立 ProcessManager 实例 |
| M16 | `feature/skills/impl/.../SkillsScreen.kt` | 349-370 | `remember()` 缓存过期的秘密值 |

### 资源 / 配置 (12 项)

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| M17 | `core/datastore/.../DataStoreModule.kt` | 36 | 创建孤儿的 `CoroutineScope`，ApplicationScope 关闭后协程继续运行 |
| M18 | `linux-sandbox/.../LinuxSandboxManager.kt` | 65-66 | CancellationException 被捕获后未重新抛出 |
| M19 | `linux-sandbox/.../ProotExecutor.kt` | 129-133 | `CompletableFuture.supplyAsync` 阻塞读占满 ForkJoinPool |
| M20 | `linux-sandbox/.../RootfsDownloader.kt` | 93 | `Files.createSymbolicLink` 在 API 24-25 崩溃 (需要 API 26+) |
| M21 | `linux-sandbox/.../RootfsDownloader.kt` | 96-97 | 符号链接创建异常静默吞掉 |
| M22 | `app/proguard-rules.pro` | 2 | `-repackageclasses` 无包名，反射依赖可能因类移入根包失败 |
| M23 | `core/domain/.../SkillManager.kt` | 32-35 | `MasterKeys.getOrCreate()` 在构造中可能主线程 ANR |
| M24 | `app/.../JasmineApp.kt` | 89-96 | 离线 snackbar 连接恢复后不自动关闭 |
| M25 | `linux-sandbox/sandbox/build.gradle.kts` | 41-57 | 硬编码依赖版本号而非使用 version catalog |
| M26 | `build-logic/gradle.properties` | 4 | `org.gradle.configureondemand=true` 已被弃用 |
| M27 | `skills/featured/mood-music/.../index.html` | 36-41 | API key 缺失时仍发起必然失败的请求 |
| M28 | 多个 skills | — | HTTP 响应无 `response.ok` 检查 |

---

## 四、LOW（低危 — 共 15 项）

| 编号 | 文件 | 行号 | 描述 |
|------|------|------|------|
| L1 | `core/designsystem/.../MarkdownText.kt` | 33 | 空的 `CompositionLocalProvider` 无操作包装 |
| L2 | `core/designsystem/.../Navigation.kt` | 210-213 | 每次重组创建新对象需 `remember` |
| L3 | `core/designsystem/.../Theme.kt` | 264-291 | `SideEffect` 每次重组都设置状态栏 |
| L4 | `core/database/.../JasmineDatabase.kt` | 15 | `exportSchema=true` 但未配置 schema 输出目录 |
| L5 | `core/datastore/.../DataStoreModule.kt` | 37 | 空的 migration 列表 |
| L6 | `websearch/.../SearchIntentDetector.kt` | 89 | 硬编码年份 "2024, 2025, 2026" |
| L7 | `feature/sandbox/impl/.../AnsiParser.kt` | 138-140 | 无法识别转义序列只跳过 2 字节 |
| L8 | `feature/settings/impl/.../SettingsScreen.kt` | 420-425 | catch 块内无嵌套异常保护 |
| L9 | `app/.../JankStatsModule.kt` | 17-22 | 每帧卡顿都记日志无采样 |
| L10 | `app/.../JasmineApp.kt` | 228-229 | `error()` 在导航数据不一致时 crash |
| L11 | `app/.../MainActivityViewModel.kt` | 22-28 | StateFlow 管道无 `.catch{}` |
| L12 | `skills/.../mood-tracker/.../index.html` | 101 | `parseInt('0') \|\| 7` falsy bug |
| L13 | `skills/.../text-spinner/.../index.html` | 25 | `<title>` 为 "Create github issue"（复制粘贴遗留） |
| L14 | `skills/.../mood-music/.../index.html` | 84-128 | 测试 UI 代码泄漏到生产文件 |
| L15 | `skills/.../restaurant-roulette/.../index.js` | 82-85 | 错误消息截断到 15 字符 |

---

## 五、统计

| 严重级别 | 数量 | 主要分类 |
|---------|------|---------|
| **CRITICAL** | 5 | 数据竞争、构建错误、假进程杀死 |
| **HIGH** | 24 | 安全漏洞、资源泄漏、崩溃风险、逻辑错误、协程问题 |
| **MEDIUM** | 28 | 线程安全、错误处理、资源管理、配置 |
| **LOW** | 15 | 代码质量、测试保真度、文档 |
| **合计** | **72** | |

---

## 六、修复优先级建议

### 第一批: 线程安全与数据完整性

1. C1 — SkillManager 线程安全 (加 Mutex)
2. C2 — FileLogger 线程安全 (加 Mutex)
3. C3 — ChatClientManager 竞态条件
4. M1-M3 — ChatViewModel 协程问题

### 第二批: 资源泄漏与崩溃

5. S8 — ChatScreen WebView 泄漏
6. S10-S13 — OkHttpClient/HttpClient 泄漏
7. S14-S16 — crash 风险修复
8. C4-C5 — linux-sandbox 构建和 ProcessManager

### 第三批: 安全

9. S1-S2 — 密钥安全
10. S3-S4 — 签名配置安全

### 第四批: 其余 MEDIUM 和 LOW

11. 按模块分批修复
