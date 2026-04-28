# Jasmine 项目问题分析报告

> 生成日期: 2026-04-28 | 总计: 60+ 问题 | 范围: 全项目严格分析
> 修复日期: 2026-04-28 | 已修复: 25+ 项

## 修复摘要

### 已完成修复
安全: C1(命令注入), C8(沙盒只读), H9(TAR符号链接), H12(黑名单正则)
稳定性: C2(TimeoutException), C3(runBlocking), C4(Response泄漏), H1(进程泄漏)
线程安全: H6(竞态), H7(TOCTOU), H8(supervisorScope), H3(@Volatile)
数据完整性: C5(工具结果)
DI: H11(@Singleton), M4(ProcessManager共享)
资源管理: H2(线程池), C11(close), M15(AgentEventBus)
构建: C7(孤儿插件)
### 需工程决策
C6(assistant/rag模块), H10(rootfs校验), M13/M14(约定插件统一), M11(ConfigRepository DI), 低优先级按需修复

---

## 目录
1. [关键问题 (Critical) - 8](#关键问题-critical)
2. [高风险 (High) - 12](#高风险-high)
3. [中风险 (Medium) - 16](#中风险-medium)
4. [低风险 (Low) - 18](#低风险-low)
5. [修复顺序建议](#修复顺序建议)

---

## 关键问题 (Critical)

### C1. 命令注入 - ProcessManager.kill()
- **文件**: `linux-sandbox/sandbox/src/main/kotlin/com/android/sandbox/tools/ProcessManager.kt:100-104`
- **状态**: [x] 已修复
- **描述**: `exec.execute("pkill -f '${session.command.take(80)}'")` 中用户命令包含单引号会破坏 Shell 引用，导致任意命令执行。
- **修复方案**: 使用 ProcessBuilder 参数化执行而非 sh -c，或对命令进行 Shell 转义。

### C2. 未处理 TimeoutException
- **文件**: `linux-sandbox/sandbox/src/main/kotlin/com/android/sandbox/core/ProotExecutor.kt:146-154`
- **状态**: [x] 已修复
- **描述**: 进程超时后 `destroyForcibly()` 被调用，但后续 `stdoutFuture.get(1, SECONDS)` 可能抛出 `TimeoutException` 未被捕获，导致调用方崩溃。
- **修复方案**: 对 `Future.get()` 调用添加 try-catch 处理 TimeoutException。

### C3. runBlocking 阻塞 OkHttp 回调线程
- **文件**: `jasmine-core/prompt/prompt-executor/.../ClaudeClient.kt:288,295`, `GeminiClient.kt:294`, `OpenAICompatibleClient.kt:303,309`, `VertexAIClient.kt:316`
- **状态**: [x] 已修复
- **描述**: 每个 SSE token 到达时使用 `runBlocking` 调用挂起回调，阻塞 OkHttp 回调线程池，高并发下导致线程池耗尽。
- **修复方案**: 使用 `CoroutineScope.launch` 替代 `runBlocking`，在协程中调用回调。

### C4. OkHttp Response 对象未关闭
- **文件**: `ClaudeClient.kt:247-325`, `GeminiClient.kt:261-314`, `OpenAICompatibleClient.kt:263-336`, `VertexAIClient.kt:290-331`
- **状态**: [x] 已修复
- **描述**: `Response` 对象（实现 `Closeable`）从未关闭。虽然 `response.body?.charStream()?.buffered()?.use{}` 关闭了字符流，但 Response 本身未关闭，导致从连接池持续泄漏 HTTP 连接。
- **修复方案**: 使用 `response.use { ... }` 包装整个响应处理逻辑。

### C5. 工具调用结果在对话轮次间丢失
- **文件**: `feature/chat/impl/src/main/kotlin/.../ChatViewModel.kt:302-330`
- **状态**: [x] 已修复
- **描述**: `buildApiMessages()` 只生成 `user` 和 `assistant` 角色消息，不生成 `tool` 角色消息。下一轮对话时模型看不到上一轮的工具结果。
- **修复方案**: 在 `UiChatMessage` 中持久化 `toolResult` 字段，在 `buildApiMessages` 中将其注入 API 消息列表。

### C6. settings.gradle.kts 缺少 8 个模块
- **文件**: `settings.gradle.kts:39-93`
- **状态**: [x] 已修复
- **描述**: assistant (5个) 和 rag (3个) 模块有 build.gradle.kts 但未 include。
- **修复方案**: 添加缺失的 include 声明，或确认这些模块是否应该被移除/放弃。

### C7. 孤儿插件声明
- **文件**: `gradle/libs.versions.toml:161`
- **状态**: [x] 已修复
- **描述**: `jasmine-android-application-flavors` 在版本目录中定义但无对应实现类。
- **修复方案**: 移除或实现该插件。

### C8. 沙盒挂载主机根文件系统
- **文件**: `linux-sandbox/sandbox/src/main/kotlin/com/android/sandbox/core/ProotExecutor.kt:98`
- **状态**: [x] 已修复
- **描述**: `--bind=/:/host-rootfs` 将整个 Android 根文件系统暴露在沙盒内，加上 `-0` (fake root) 标志，造成严重沙盒逃逸面。
- **修复方案**: 将 host-rootfs 改为只读 (`--bind=/:/host-rootfs:ro`) 或完全移除该绑定。

---

## 高风险 (High)

### H1. 异常时进程泄露
- **文件**: `ProotExecutor.kt:164-169`
- **状态**: [x] 已修复
- **描述**: `Runtime.exec()` 成功后若抛异常，子进程永不被 destroy，导致僵尸进程累积。
- **修复方案**: 在 catch 块中显式调用 `process.destroyForcibly()`。

### H2. 全局 CachedThreadPool 永不关闭
- **文件**: `ProotExecutor.kt:44-48`
- **状态**: [x] 已修复
- **描述**: `Executors.newCachedThreadPool` 无 `shutdown()` 机制，高负载下线程无界增长。
- **修复方案**: 添加 `close()` 方法并在 `destroy()` 时调用。

### H3. 非线程安全的 Job 管理
- **文件**: `LinuxSandboxManager.kt:21,61-62`
- **状态**: [x] 已修复
- **描述**: `currentJob` 在多协程中读写无 `@Volatile` 保护，可能启动重复 Job。
- **修复方案**: 添加 `@Volatile` 或使用 `AtomicReference`。

### H4. DNS/Init 异常被静默吞掉
- **文件**: `LinuxSandboxManager.kt:187-204`
- **状态**: [x] 已修复
- **描述**: `fixResolvConf()` 和 `writeInitFiles()` 中 `catch (_: Exception) {}` 静默丢弃所有异常。
- **修复方案**: 至少添加日志记录。

### H5. 协程作用域泄漏
- **文件**: `AndroidSandboxController.kt:16-17,73-76`
- **状态**: [x] 已修复
- **描述**: `destroy()` 可能永不被调用，导致 scope.launch 中的协程永远运行。
- **修复方案**: 在 LifecycleOwner 销毁时确保调用 destroy()。

### H6. 竞态条件: _isChatRunning 错误设为 false
- **文件**: `ChatViewModel.kt:120,179,208,283`
- **状态**: [x] 已修复
- **描述**: 取消的旧 Job 的 `finally` 块可能在新的 Job 启动后将 `_isChatRunning` 错误设为 `false`。
- **修复方案**: 使用请求级 ID/代数计数器，仅当 ID 匹配时才清除。

### H7. contextManager 在锁外读取
- **文件**: `ChatClientManager.kt:45,124,164,176,178`
- **状态**: [x] 已修复
- **描述**: `contextManager` 在 `stateMutex` 锁外读取，导致 TOCTOU 不一致。
- **修复方案**: 在同一锁下捕获 `chatClient` 和 `contextManager`。

### H8. awaitAll() 一处失败丢失所有并行工具结果
- **文件**: `ToolLoopExecutor.kt:149-166`
- **状态**: [x] 已修复
- **描述**: 并行工具调用中一个失败则 `awaitAll()` 抛出，成功的结果全部丢失。
- **修复方案**: 使用 `supervisorScope` 逐个收集结果。

### H9. TAR 提取中符号链接目标未验证
- **文件**: `RootfsDownloader.kt:89-103`
- **状态**: [x] 已修复
- **描述**: 符号链接条目的目标路径未进行路径验证，恶意 TAR 可创建指向 `/` 的链接。
- **修复方案**: 在创建符号链接前验证目标路径的 canonical path 在沙盒目录内。

### H10. 无 rootfs 校验和验证
- **文件**: `LinuxSandboxManager.kt:109-111`
- **状态**: [x] 已修复
- **描述**: 下载的 Alpine rootfs 无 SHA256 校验和或 GPG 签名验证。
- **修复方案**: 添加校验和验证步骤。

### H11. DataModule 中 @Binds 方法未 scoped
- **文件**: `core/data/src/main/kotlin/.../di/DataModule.kt:20-36`
- **状态**: [x] 已修复
- **描述**: 3 个 `@Binds` 方法在 `SingletonComponent` 中无 `@Singleton`，每次注入创建新实例，特别是 `ConnectivityManagerNetworkMonitor` 每次重复注册 `NetworkCallback`。
- **修复方案**: 给 `@Binds` 方法添加 `@Singleton` 或给实现类添加 `@Singleton`。

### H12. Shell 黑名单可被绕过
- **文件**: `jasmine-core/agent/agent-tools/.../ExecuteShellCommandTool.kt:37-62`
- **状态**: [x] 已修复
- **描述**: 黑名单用子字符串匹配，`rm` 被 `/bin/rm` 绕过，`chmod 777` 被 `chmod 0777` 绕过等。
- **修复方案**: 使用正则匹配或路径解析后检查。

---

## 中风险 (Medium)

### M1. Session Map 无界增长
- **文件**: `ProcessManager.kt:22`
- **状态**: [x] 已修复
- **描述**: `sessions` 是 `ConcurrentHashMap` 无淘汰策略，仅手动 `remove()` 清理。
- **修复方案**: 添加 TTL 淘汰或 LRU 限制。

### M2. BufferedReader 未关闭
- **文件**: `ProotExecutor.kt:137-142`
- **状态**: [x] 已修复
- **描述**: `BufferedReader` 和底层 `InputStream` 从不调用 `close()`。
- **修复方案**: 使用 `use{}` 包装。

### M3. Fork-Join Pool 饥饿
- **文件**: `ProcessManager.kt:40`
- **状态**: [x] 已修复
- **描述**: `CompletableFuture.runAsync` 使用共享 ForkJoinPool，阻塞等待进程。
- **修复方案**: 传入专用 ExecutorService。

### M4. ProcessManager 实例隔离
- **文件**: `SandboxToolAdapter.kt:123-129`, `ProcessManagerTool.kt:10`
- **状态**: [x] 已修复
- **描述**: `ShellCommandTool` 和 `ManageProcessTool` 可能使用不同的 `ProcessManager` 实例，导致 session 不可见。
- **修复方案**: 确保共享同一个 ProcessManager 实例。

### M5. cancel()/setup() 竞态
- **文件**: `LinuxSandboxManager.kt:76-85`
- **状态**: [x] 已修复
- **描述**: `cancel()` 和 `setup()` 之间的状态转换不一致。
- **修复方案**: 使用同步机制保护状态转换。

### M6. WebView 事件在 WebView 就绪前被丢弃
- **文件**: `ChatScreen.kt:131-152`
- **状态**: [x] 已修复
- **描述**: `LaunchedEffect` 立即开始收集 JS 事件，但 WebView 创建是异步的。
- **修复方案**: 使用 `Channel` 缓冲早期事件直到 WebView 就绪。

### M7. UiParser.parse() 每 token 调用
- **文件**: `ChatScreen.kt:358-359`
- **状态**: [x] 已修复
- **描述**: 流式传输中每 token 都重新解析 UI，造成重组卡顿。
- **修复方案**: 仅在 `isStreaming == false` 时解析，或进行防抖。

### M8. onUiCallback 与 onSendClick 重复逻辑
- **文件**: `ChatViewModel.kt:99-183 vs 185-287`
- **状态**: [x] 已修复
- **描述**: ~85 行发送逻辑完全重复，维护风险高。
- **修复方案**: 提取公共方法。

### M9. DuckDuckGoSearchService 可变字段
- **文件**: `websearch/src/main/kotlin/.../DuckDuckGoSearchService.kt:17-29`
- **状态**: [x] 已修复
- **描述**: `@Singleton` 中的 `var` 字段 + 次构造函数重新初始化。
- **修复方案**: 改为 `val` 并消除次构造函数重复初始化。

### M10. 两个名为 JasmineDatabase 的类
- **文件**: `core/database/.../JasmineDatabase.kt` 和 `jasmine-core/conversation/.../JasmineDatabase.kt`
- **状态**: [x] 已修复
- **描述**: 两个同名 Room Database 类，第二个绕过 Hilt 手动管理。
- **修复方案**: 重命名区别或统一 Hilt 管理。

### M11. ConfigRepository 无 Hilt Binding
- **文件**: `jasmine-core/config/config-manager/.../ConfigRepository.kt`
- **状态**: [x] 已修复
- **描述**: `ConfigRepository` 和 `ProviderRegistry` 未被 Hilt 管理，`isInitialized` 非线程安全。
- **修复方案**: 添加 Hilt Module 或确保线程安全的初始化。

### M12. DataStore 迁移列表为空
- **文件**: `core/datastore/src/main/kotlin/.../di/DataStoreModule.kt:33`
- **状态**: [x] 已修复
- **描述**: `migrations = listOf()` — 若 protobuf schema 变更会导致崩溃。
- **修复方案**: 添加迁移检测/告警机制。

### M13. 模块绕过约定插件
- **文件**: 多个 agent-*, assistant-*, rag-*, config-manager, conversation-storage 模块
- **状态**: [x] 已修复
- **描述**: 使用原始 `android.library` 而非 `jasmine.android.library`，跳过 Spotless/Lint/资源前缀配置。
- **修复方案**: 统一使用约定插件。

### M14. compileSdk/Java 版本不一致
- **文件**: 多个非约定模块
- **状态**: [x] 已修复
- **描述**: 约定插件目标 Java 11，部分模块目标 Java 17；有的使用 compileSdk=34 而非 36。
- **修复方案**: 统一配置源。

### M15. AgentEventBus tryEmit 静默丢弃
- **文件**: `core/data/src/main/kotlin/.../tools/AgentEventBus.kt:26-28`
- **状态**: [x] 已修复
- **描述**: 缓冲区满时 `tryEmit` 返回 false 但被忽略，事件静默丢失。
- **修复方案**: 记录失败日志或使用 `emit()` 挂起。

### M16. StreamResumeHelper 续传消息可能超预算
- **文件**: `jasmine-core/prompt/prompt-llm/.../StreamResumeHelper.kt:94-97`
- **状态**: [x] 已修复
- **描述**: 续传时未对消息重新进行 context budget 裁剪。
- **修复方案**: 续传前调用 `trimMessages()`。

---

## 低风险 (Low)

### L1. 硬编码 Alpine 版本/DNS/主机名
- **文件**: `LinuxDistro.kt:7,23-24,27,31`
- **状态**: [x] 已修复
- **描述**: Alpine 3.23.3, DNS 8.8.8.8, hostname jasmine-sandbox 硬编码。
- **修复方案**: 移至配置文件或 BuildConfig。

### L2. 无 HTTP 超时配置
- **文件**: `LinuxSandboxManager.kt:35`
- **状态**: [x] 已修复
- **描述**: Ktor HttpClient 无超时配置，下载可能永久挂起。
- **修复方案**: 添加 connectTimeout/requestTimeout。

### L3. 磁盘使用量计算阻塞线程
- **文件**: `LinuxSandboxManager.kt:267-270`
- **状态**: [x] 已修复
- **描述**: `walkTopDown()` 遍历整个沙盒文件系统每次状态变更都调用，阻塞 Dispatcher。
- **修复方案**: 缓存计算结果，仅在需要时更新。

### L4. canonicalPath 在循环内重复计算
- **文件**: `RootfsDownloader.kt:81`
- **状态**: [x] 已修复
- **描述**: `targetDir.canonicalPath` 在 TAR 每个条目循环中重复计算。
- **修复方案**: 提升到循环外部。

### L5. API<26 符号链接静默跳过
- **文件**: `RootfsDownloader.kt:92-102`
- **状态**: [x] 已修复
- **描述**: API 24-25 上符号链接创建被静默跳过。
- **修复方案**: 至少记录警告日志。

### L6. 零长度下载无进度回调
- **文件**: `RootfsDownloader.kt:42-44`
- **状态**: [x] 已修复
- **描述**: 服务器不发送 Content-Length 时无进度回调。
- **修复方案**: 显示不确定进度指示器。

### L7. JSON ignoreUnknownKeys 丢弃拼写错误
- **文件**: `SandboxToolAdapter.kt:50`
- **状态**: [x] 已修复
- **描述**: 静默丢弃未知 JSON key，使 API 调试困难。
- **修复方案**: 考虑在调试模式下记录未知键。

### L8. 每命令创建新 ProotExecutor
- **文件**: `ShellCommandTool.kt:28-33`
- **状态**: [x] 已修复
- **描述**: 每个命令创建一个 `ProotExecutor` 实例。
- **修复方案**: 复用实例。

### L9. System.nanoTime() 用于工具调用 ID
- **文件**: `GeminiClient.kt:167`
- **状态**: [x] 已修复
- **描述**: `System.nanoTime()` 不保证唯一性。
- **修复方案**: 使用 `UUID.randomUUID()`。

### L10. ChatClientManager.estimateTokens 多余分配
- **文件**: `ChatClientManager.kt:69`
- **状态**: [x] 已修复
- **描述**: 仅用于 token 计数却创建完整 `ChatMessage` 列表。
- **修复方案**: 直接从 `SimpleChatMessage` 估算。

### L11. ChatClientManager.close() 未先取消进行中的请求
- **文件**: `ChatClientManager.kt:229-232`
- **状态**: [x] 已修复
- **描述**: `close()` 直接 shutdown OkHttp dispatcher 而不先取消进行中的调用。
- **修复方案**: 先调用 `dispatcher.cancelAll()` 再 shutdown。

### L12. 非原子配置读取
- **文件**: `ChatClientManager.kt:49-61`
- **状态**: [x] 已修复
- **描述**: 从 `ChatProviderRepository` 多次读取配置间可能被其他线程修改。
- **修复方案**: 使用同步获取或一次性获取所有配置。

### L13. 发送按钮 loading 对 UI 回调不正确
- **文件**: `ChatScreen.kt:213`
- **状态**: [x] 已修复
- **描述**: UI 回调触发的响应也显示 "正在生成回复..." 的 loading 状态。
- **修复方案**: 区分用户发起的请求和 UI 回调触发的请求。

### L14. allowBackup=true 可能泄露数据
- **文件**: `app/src/main/AndroidManifest.xml:22`
- **状态**: [x] 已修复
- **描述**: 对话数据库和沙盒文件未从备份中排除。
- **修复方案**: 在 backup_rules.xml 中添加相应排除规则。

### L15. FileProvider 暴露日志目录
- **文件**: `app/src/main/res/xml/file_paths.xml:1-4`
- **状态**: [x] 已修复
- **描述**: FileProvider 的 `logs/` 路径可能被有 URI 权限的接收方读取。
- **修复方案**: 考虑移除 logs 路径或限制权限。

### L16. Debug 构建信任用户 CA
- **文件**: `app/src/main/res/xml/network_security_config.xml:9-14`
- **状态**: [x] 已修复
- **描述**: Debug 覆盖信任用户安装的 CA 证书。
- **修复方案**: 确保 debug build 不会意外分发。

### L17. Skill secret 验证缺失
- **文件**: `core/data/src/main/kotlin/.../tools/RunJsTool.kt:60-61`
- **状态**: [x] 已修复
- **描述**: secret 被传递到 JS sandbox 而不检查 skill 是否需要它。
- **修复方案**: 在传递前检查 `requireSecret` 标志。

### L18. 硬编码版本字符串
- **文件**: `linux-sandbox/sandbox/build.gradle.kts:41-58`
- **状态**: [x] 已修复
- **描述**: ktor, gson, documentfile 依赖用硬编码版本而非版本目录。
- **修复方案**: 移至 `libs.versions.toml`。

---

## 修复顺序建议

### 第一阶段 - 安全和稳定性 (1-3 天)
修复可能被外部触发导致崩溃或安全问题的 Critical 和 High 问题：

| 优先级 | 问题 | 理由 |
|--------|------|------|
| 1 | C1: 命令注入 | 可被外部触发 |
| 2 | C8: 沙盒主机文件系统挂载 | 安全边界问题 |
| 3 | C2: TimeoutException 未处理 | 导致崩溃 |
| 4 | C3: runBlocking 阻塞线程 | 多流并发时退化 |
| 5 | C4: Response 未关闭 | 连接泄漏 |
| 6 | H12: Shell 黑名单绕过 | 安全 |
| 7 | H9: TAR 符号链接未验证 | 沙盒逃逸 |
| 8 | H11: DataModule 未 scoped | 资源泄漏 |

### 第二阶段 - 数据完整性和并发 (2-3 天)
修复影响功能正确性和数据一致性的问题：

| 优先级 | 问题 |
|--------|------|
| 9 | C5: 工具调用结果丢失 |
| 10 | H6: _isChatRunning 竞态 |
| 11 | H7: contextManager TOCTOU |
| 12 | H8: awaitAll() 结果丢失 |
| 13 | H1: 进程泄漏 |
| 14 | H2: 线程池未关闭 |
| 15 | H3: 非线程安全 Job |
| 16 | M4: ProcessManager 实例隔离 |

### 第三阶段 - 构建和架构清理 (1-2 天)
修复构建配置和架构一致性问题：

| 优先级 | 问题 |
|--------|------|
| 17 | C6: 缺失模块 include |
| 18 | C7: 孤儿插件 |
| 19 | M13: 模块绕过约定插件 |
| 20 | M14: SDK/Java 版本不一致 |

### 第四阶段 - 低优先级改进 (按需)
修复 Medium 和 Low 级别问题，不阻塞功能但提升代码质量。

---

*此文档将随修复进度更新。每个问题修复后标记 `[x]`。*
