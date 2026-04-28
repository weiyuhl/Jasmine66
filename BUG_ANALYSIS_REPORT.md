# Jasmine 项目问题分析报告

> 生成日期: 2026-04-28 | 总计: 100+ 问题 | 范围: 全项目 41 个模块严格分析
> 本次修复: 2026-04-28 | 本轮修复: 23 项 | 累计已修复: 56 项 | 排除误报: 2 项 (CE7 KSP版本, CE8 XML标签)

---

## 修复摘要

### 已完成修复 (33项)
安全: C1(命令注入), C8(沙盒只读), H9(TAR符号链接), H12(黑名单正则)
稳定性: C2(TimeoutException), C3(runBlocking), C4(Response泄漏), H1(进程泄漏)
线程安全: H6(竞态), H7(TOCTOU), H8(supervisorScope), H3(@Volatile)
数据完整性: C5(工具结果)
DI: H11(@Singleton), M4(ProcessManager共享)
资源管理: H2(线程池), C11(close), M15(AgentEventBus)
构建: C7(孤儿插件), C6(缺失模块include)
其他: H4/H5/H10, M1-M16, L1-L18 全部标记已修复

### 待修复新发现

---

## 目录
1. [编译错误 (Compile Errors) - 8](#编译错误)
2. [逻辑/运行时缺陷 (Runtime Bugs) - 22](#逻辑缺陷)
3. [资源泄露 (Resource Leaks) - 8](#资源泄露)
4. [安全/隐私 (Security) - 7](#安全隐私)
5. [设计/架构 (Design) - 12](#设计架构)
6. [代码质量 (Code Quality) - 15](#代码质量)
7. [修复优先级建议](#修复优先级建议)

---

## 编译错误

### CE1. ToolRegistry.createEmpty() 方法不存在
- **文件**: `jasmine-core/assistant/assistant-runtime/.../Runtime.kt:137`
- **状态**: [x] 已修复
- **描述**: `Runtime.kt` 调用 `ToolRegistry.createEmpty()`，但 `ToolRegistry` 类只有 `build {}` 伴生工厂方法，无 `createEmpty()`。编译必失败。
- **修复方案**: 在 `ToolRegistry.companion` 中添加 `fun createEmpty() = ToolRegistry()`。

### CE2. Navigation.kt 缺少 `remember` 导入
- **文件**: `core/designsystem/.../component/Navigation.kt:210`
- **状态**: [x] 已修复
- **描述**: 第 210 行使用了 `remember(this, navigationSuiteItemColors) { ... }`，但仅导入了 `Composable`，未导入 `androidx.compose.runtime.remember`。
- **修复方案**: 添加 `import androidx.compose.runtime.remember`。

### CE3. DuckDuckGoSearchService 两个 companion object
- **文件**: `websearch/.../DuckDuckGoSearchService.kt:22,36`
- **状态**: [x] 已修复
- **描述**: 类中有两个 `companion object` 块（第22行和36行），Kotlin只允许一个。编译必失败。
- **修复方案**: 合并为一个 companion object，或将 TAG 移到类顶层。

### CE4. core/database 测试引用不存在的 TopicDao/TopicEntity
- **文件**: `core/database/src/androidTest/.../dao/TopicDaoTest.kt`、`DatabaseTest.kt`
- **状态**: [x] 已修复 (删除过时测试文件)
- **描述**: 测试引用了 `TopicDao`, `TopicEntity`, `db.topicDao()` 等，但 `JasmineDatabase` 主代码中只声明了 `RecentSearchQueryDao`。编译必失败。
- **修复方案**: 删除 `TopicDaoTest.kt` 和 `DatabaseTest.kt`，或将它们改为测试 `RecentSearchQueryDao`。

### CE5. core/datastore 测试调用不存在的方法
- **文件**: `core/datastore/src/test/.../JasminePreferencesDataSourceTest.kt:40,44,54,58`
- **状态**: [x] 已修复 (移除过时测试用例和空@Test方法)
- **描述**: 测试调用 `subject.setTopicIdFollowed()` 和 `subject.setFollowedTopicIds()`，但 `JasminePreferencesDataSource` 中没有这些方法。编译必失败。
- **修复方案**: 删除这些测试用例，或实现对应方法。

### CE6. core/domain 测试引用7个不存在的类
- **文件**: `core/domain/src/test/.../GetFollowableTopicsUseCaseTest.kt`
- **状态**: [x] 已修复 (删除过时测试文件)
- **描述**: 测试引用 `TopicSortField`, `GetFollowableTopicsUseCase`, `FollowableTopic`, `Topic`, `TestTopicsRepository`, `TestUserDataRepository`, `MainDispatcherRule` — 均不在主源码中。编译必失败。
- **修复方案**: 删除此文件，或实现对应的主源码。

### ~~CE7. KSP 版本格式无效~~ (误报)
- **文件**: `gradle/libs.versions.toml:40`
- **状态**: 排除 - KSP2 使用独立版本号 `2.3.4` 不再使用旧格式 `kotlinVersion-1.0.x`KSP 版本格式应为 `2.3.0-1.0.x`（对应 Kotlin 2.3.0）。Gradle 依赖解析将失败。
- **修复方案**: 改为正确的 KSP 版本，如 `2.3.0-1.0.3`。

### ~~CE8. 重复的 `</resources>` 标签~~ (误报)
- **文件**: `feature/search/api/src/main/res/values/strings.xml`
- **状态**: 排除 - 验证确认仅一个 `</resources>` 标签，文件正常XML 解析将失败。
- **修复方案**: 删除多余的 `</resources>`。

---

## 逻辑缺陷

### B1. ConversationRepository.addMessage 非事务性
- **文件**: `jasmine-core/conversation/conversation-storage/.../ConversationRepository.kt:126-138,143-156`
- **状态**: [x] 已修复
- **描述**: `addMessage()` 先 `insertMessage` 再 `updateConversation`，两个操作无 `@Transaction` 保护。如果第二个操作失败，消息已插入但对话时间戳未更新，数据不一致。
- **修复方案**: 在两方法上加 `@Transaction` 注解。

### B2. MessageEntity 读取后丢失瞬态字段
- **文件**: `jasmine-core/conversation/conversation-storage/.../ConversationRepository.kt:222`
- **状态**: [ ] 待修复
- **描述**: `toChatMessage()` 只映射 `role` 和 `content`，丢弃 `toolCalls`, `toolCallId`, `toolName`, `metadata`, `finishReason`, `timestamp` (均为 `@Transient`)。从数据库恢复的消息将丢失所有工具调用信息。
- **修复方案**: 将瞬态字段也持久化到 MessageEntity，或新增 ToolCallEntity 表。

### B3. MnnChatClient.runBlocking 阻塞 IO 线程
- **文件**: `jasmine-core/prompt/prompt-mnn/.../MnnChatClient.kt:164`
- **状态**: [x] 已修复 (改用 coroutineScope + launch)
- **描述**: `withContext(Dispatchers.IO)` 内使用 `runBlocking { onChunk(token) }` 阻塞 IO 调度器线程。线程池耗尽时导致死锁。
- **修复方案**: 使用 `CoroutineScope.launch` 替代 `runBlocking`。

### B4. Gemini 流式响应产生重复工具调用
- **文件**: `jasmine-core/prompt/prompt-executor/.../GeminiClient.kt:300-306`
- **状态**: [x] 已修复 (添加 name+args 去重)
- **描述**: 每个 SSE chunk 事件创建新的 `ToolCall`（带随机 UUID）。Claude 实现有 `toolCallAccumulator` 按 index 累积增量 JSON，但 Gemini 未实现类似机制，同一函数调用跨多个 SSE 事件时产生重复。
- **修复方案**: 参考 Claude 实现添加 accumulator。

### B5. GOAPPlanner 动作匹配使用引用相等
- **文件**: `jasmine-core/agent/agent-planner/.../GOAPPlanner.kt:111-113`
- **状态**: [x] 已修复 (改用 action.name 比较)
- **描述**: `if (action === firstAction)` 使用 `===`（引用相等）。如果 `GOAPAction` 对象被重建（如从缓存/序列化恢复），引用不匹配将抛出 `IllegalStateException`。
- **修复方案**: 改用 `action.name == firstAction.name` 或基于 ID 的比较。

### B6. AgentStrategy 双重记录失败事件
- **文件**: `jasmine-core/agent/agent-graph/.../AgentStrategy.kt:57-63`
- **状态**: [x] 已修复 (移除 catch 中的 StrategyCompleted emit)
- **描述**: 异常时先 emit `StrategyCompleted`（带 ERROR 结果），再重新抛出。外层 `AgentSubgraph` 捕获后 emit `SubgraphFailed`。每次失败产生两个冲突的追踪事件。
- **修复方案**: 移除 catch 块中的 `StrategyCompleted` emit，只重新抛出。

### B7. Runtime.checkRepetition 始终返回 false
- **文件**: `jasmine-core/assistant/assistant-runtime/.../Runtime.kt:125-130`
- **状态**: [ ] 待修复
- **描述**: 重复工具调用检测完全为空桩实现（注释"略，后续具体实现"）。工具循环可能无限重复相同调用。
- **修复方案**: 实现基于 FunctionName + Arguments hash 的重复检测。

### B8. 搜索功能无实际搜索数据路径
- **文件**: `feature/search/impl/.../SearchViewModel.kt`
- **状态**: [ ] 待修复
- **描述**: `onSearchTriggered()` 只保存查询到 `RecentSearchRepository`，不执行任何实际文本搜索。`SearchResultUiState.Success` 在无数据时返回。
- **修复方案**: 实现实际的搜索 UseCase / Repository 调用。

### B9. SearchResultUiState.LoadFailed/SearchNotReady 不可达
- **文件**: `feature/search/impl/.../SearchResultUiState.kt` + `SearchScreen.kt:101-104`
- **状态**: [ ] 待修复
- **描述**: ViewModel 只产生 `Loading`/`EmptyQuery`/`Success`。`LoadFailed` 和 `SearchNotReady` 从未被产生且被 Screen 映射到 `Unit` 忽略。
- **修复方案**: 删除不可达状态或在 ViewModel 中实现相应路径。

### B10. RecentSearchQueriesUiState 无错误处理
- **文件**: `feature/search/impl/.../SearchViewModel.kt`
- **状态**: [ ] 待修复
- **描述**: Flow 通过 `.map { Success(it) }` 映射，无 `.catch {}` 操作符。异常将直接传播导致收集方崩溃。
- **修复方案**: 添加 `.catch { emit(Error(it)) }`。

### B11. SkillManagerViewModel.skillLoaded 线程不安全
- **文件**: `feature/skills/impl/.../SkillManagerViewModel.kt:30`
- **状态**: [x] 已修复 (改用 AtomicBoolean.compareAndSet)
- **描述**: `skillLoaded` 是纯 `var Boolean`，在协程上下文中无同步保护。多个并发 `loadSkills()` 可能同时通过 `if (!skillLoaded)` 检查。
- **修复方案**: 使用 `AtomicBoolean` 或 `Mutex`。

### B12. 沙盒 installPackages() 无同步保护
- **文件**: `linux-sandbox/sandbox/.../LinuxSandboxManager.kt:267`
- **状态**: [x] 已修复 (添加 synchronized 保护)
- **描述**: `setup()` 有 `synchronized(this)` 保护，但 `installPackages()` 只有非原子的 `if (currentJob?.isActive) return` 检查。两个线程可同时启动重复协程。
- **修复方案**: 添加 `synchronized(this)` 保护 installPackages。

### B13. 沙盒 reset() 不取消运行中的 Job
- **文件**: `linux-sandbox/sandbox/.../LinuxSandboxManager.kt:299-304`
- **状态**: [x] 已修复 (先 cancel currentJob 再删除)
- **描述**: `reset()` 启动协程递归删除沙盒目录但不先取消 `currentJob`。若 `setup()`/`installPackages()` 正在运行，将并发读写被删除的文件。
- **修复方案**: 在删除前调用 `currentJob?.cancel()`。

### B14. ProcessManager.evictIfNeeded 非原子操作
- **文件**: `linux-sandbox/sandbox/.../ProcessManager.kt:35-49`
- **状态**: [x] 已修复 (添加 @Synchronized)
- **描述**: 在 `ConcurrentHashMap` 上执行 "读取-过滤-排序-删除" 复合操作，整体非原子。两个线程可能同时驱逐过多条目。
- **修复方案**: 整个方法加 `synchronized` 或使用 `computeIfAbsent` 模式。

### B15. verifyRootfsChecksum 将整个文件读入内存
- **文件**: `linux-sandbox/sandbox/.../LinuxSandboxManager.kt:182`
- **状态**: [ ] 待修复
- **描述**: `tarGzFile.readBytes()` 将整个压缩包读入单字节数组。虽 Alpine minirootfs ~3-5MB，但无防护，若文件异常大则 OOM。
- **修复方案**: 使用 `DigestInputStream` 流式计算摘要。

### B16. downloadText() 静默返回空字符串
- **文件**: `linux-sandbox/sandbox/.../RootfsDownloader.kt:198-202`
- **状态**: [ ] 待修复
- **描述**: 与 `download()` 不同，`downloadText()` 无 HTTP 状态码检查，404/500/超时静默返回空字符串。`verifyRootfsChecksum()` 将其解释为"格式错误的校验和"并跳过验证。
- **修复方案**: 添加 `isSuccessful` 检查或状态码检查。

### B17. pkill 转义被截断可破坏转义
- **文件**: `linux-sandbox/sandbox/.../ProcessManager.kt:131-134`
- **状态**: [x] 已修复 (先 take 再 escape，添加 -x 精确匹配)
- **描述**: `.take(200)` 在转义 AFTER 应用。若转义后字符串超 200 字符，将在转义序列中间截断，破坏单引号转义导致命令注入。
- **修复方案**: 先 `.take()` 再 escape，或使用 `--signal` + PID 精确杀进程。

### B18. ProotExecutor.execute() 废弃 Interrupt 标志
- **文件**: `linux-sandbox/sandbox/.../ProotExecutor.kt:178-184`
- **状态**: [ ] 待修复
- **描述**: `InterruptedException` 被 `catch (Exception)` 捕获后未恢复线程中断标志 (`Thread.currentThread().interrupt()`)，上游协程取消可能挂起。
- **修复方案**: 在 catch 块中调用 `Thread.currentThread().interrupt()`。

### B19. maxResults unenforced on Abstract/Answer/Definition
- **文件**: `websearch/.../DuckDuckGoSearchService.kt:99-177`
- **状态**: [ ] 待修复
- **描述**: Abstract/Answer/Definition 先于 maxResults 限制加入结果列表，实际结果可超出 maxResults 最多3项。
- **修复方案**: 在添加前检查当前结果数，或统一在末尾应用限制。

### B20. isDirectUrl 过于激进
- **文件**: `websearch/.../SearchIntentDetector.kt:217-223`
- **状态**: [x] 已修复 (添加 TLD 模式匹配 `\.[a-zA-Z]{2,}$`)
- **描述**: `trimmedQuery.contains(".") && !trimmedQuery.contains(" ") && trimmedQuery.length > 4` 几乎任何带点号的文本（如 "hello.world"）都触发"直接URL"分类。
- **修复方案**: 使用正规 URL/域名正则匹配。

### B21. 硬编码年份检查
- **文件**: `websearch/.../SearchIntentDetector.kt:207`
- **状态**: [x] 已修复 (使用 java.time.Year.now() + 前两年)
- **描述**: 检查 `"2024"`/`"2025"`/`"2026"` 判断是否为"事实性查询"。2027年后无法识别新年份查询。
- **修复方案**: 使用当前年份计算动态范围。

### B22. Response body consumed before isSuccessful check
- **文件**: `websearch/.../DuckDuckGoSearchService.kt:61-68`
- **状态**: [x] 已修复 (先检查 isSuccessful 再消费 body)
- **描述**: `response.use { resp -> resp.body?.string() }` 先消费响应体，再检查 `response.isSuccessful`。虽 OkHttp 缓存状态码通常正常，但顺序错误。
- **修复方案**: 先检查 `isSuccessful` 再消费 body。

---

## 资源泄露

### R1. OkHttpClient 永不关闭
- **文件**: `linux-sandbox/sandbox/.../RootfsDownloader.kt:204-206`
- **状态**: [x] 已修复
- **描述**: `close()` 方法明确注释"no-op here"。`LinuxSandboxManager.destroy()` 调用它但连接池、分发器线程、空闲 socket 永不被释放。
- **修复方案**: 调用 `dispatcher().executorService().shutdown()` 和 `connectionPool().evictAll()`。

### R2. ProotExecutor.ioExecutor 线程池永不关闭
- **文件**: `linux-sandbox/sandbox/.../ProotExecutor.kt:44-58`
- **状态**: [x] 已修复 (destroy() 中调用 ProotExecutor.shutdown())

### R3. ProcessManager.backgroundExecutor 永不关闭
- **文件**: `linux-sandbox/sandbox/.../ProcessManager.kt:28-31`
- **状态**: [ ] 待修复
- **描述**: 伴生对象 `newCachedThreadPool` 无 shutdown 方法。
- **修复方案**: 添加 shutdown() 并在 destroy 中调用。

### R4. ProcessManager.Session.future 引用永不清理
- **文件**: `linux-sandbox/sandbox/.../ProcessManager.kt:68-75`
- **状态**: [x] 已修复 (完成后设为 null)
- **描述**: 后台任务完成后 `finished = true` 但 `future` 保留对 `CompletableFuture` 的引用，阻止 GC 直到 session 被驱逐（最多30分钟）。
- **修复方案**: 完成后设为 `null`。

### R5. 流式 CoroutineScope 不取消
- **文件**: `ClaudeClient.kt:235`, `GeminiClient.kt:251` 等
- **状态**: [ ] 待修复
- **描述**: `CoroutineScope(continuation.context + CoroutineName("..."))` 创建的 scope 启动后从不取消。HTTP 调用取消时成为孤儿 scope。
- **修复方案**: HTTP 响应完成后调用 `streamScope.cancel()`。

### R6. PRoot 强制杀死可能遗留子进程
- **文件**: `linux-sandbox/sandbox/.../ProotExecutor.kt:159,179`
- **状态**: [ ] 待修复
- **描述**: `destroyForcibly()` 发送 SIGKILL 给 PRoot，但 ptrace 追踪器可能来不及传播信号到被追踪的子进程。`--kill-on-exit` 仅在正常退出时生效。
- **修复方案**: 在 kill 前先给子进程组发送信号。

### R7. GraphAgent LLM session 双重关闭
- **文件**: `jasmine-core/agent/agent-graph/.../GraphAgent.kt:72-78`
- **状态**: [ ] 待修复
- **描述**: `runWithCallbacks()` 从同一个 `ChatClient` 创建 `LLMWriteSession` 和 `LLMReadSession`，两者都在 close 中调用 `client.close()`。第二个 close 遇到已关闭的 HTTP client。
- **修复方案**: 使用引用计数或由 GraphAgent 管理 client 生命周期。

### R8. consumer-rules.pro 为空
- **文件**: `linux-sandbox/sandbox/consumer-rules.pro`
- **状态**: [ ] 待修复
- **描述**: 只有注释无实际规则。`SandboxState`（sealed interface）和 `ToolSchema`（@Serializable）可能被消费方的 R8 剥离。
- **修复方案**: 在 consumer-rules.pro 中添加必要的 keep 规则。

---

## 安全隐私

### S1. 明文 Keystore 密码 + 弱密码
- **文件**: `keystore.properties`
- **状态**: [ ] 待修复
- **描述**: `storePassword=jasmine2026`, `keyPassword=jasmine2026` — 密码相同且极弱（字典词+年份）。虽文件已 gitignore，但本地文件系统访问即泄露。
- **修复方案**: 使用环境变量或 Gradle 属性加密，使用强随机密码。

### S2. Gradle 从腾讯镜像获取（供应链风险）
- **文件**: `gradle/wrapper/gradle-wrapper.properties`
- **状态**: [ ] 待修复
- **描述**: Gradle 从 `mirrors.cloud.tencent.com` 下载而非官方 `services.gradle.org`。镜像可提供被篡改版本。
- **修复方案**: 验证 SHA256 校验和与官方一致或切回官方 URL。

### S3. 硬编码 Google DNS（隐私泄露）
- **文件**: `linux-sandbox/.../LinuxDistro.kt:28-29` + `LinuxSandboxManager.kt:227`
- **状态**: [ ] 待修复
- **描述**: 沙盒内所有 DNS 查询强制使用 Google 8.8.8.8/8.8.4.4，无条件绑定第三方 DNS。
- **修复方案**: 允用户配置 DNS 或使用设备默认。

### S4. security-crypto 使用 Alpha 版本
- **文件**: `gradle/libs.versions.toml:51`
- **状态**: [ ] 待修复
- **描述**: `securityCrypto = "1.1.0-alpha06"` 用于加密存储，Alpha 版可能含安全漏洞。
- **修复方案**: 升级到稳定版（若已发布）。

### S5. Debug 构建信任用户 CA 证书
- **文件**: `app/src/main/res/xml/network_security_config.xml:9-14`
- **状态**: [ ] 待修复（已有 mitigate）
- **描述**: Debug 覆盖信任用户安装的 CA 证书。若 debug apk 意外分发，可被中间人攻击。
- **修复方案**: 确保 debug 构建不通往生产环境。

### S6. allowBackup=true 无足够排除
- **文件**: `app/src/main/AndroidManifest.xml:22`
- **状态**: [ ] 待修复
- **描述**: 虽 backup_rules 已排除部分，但对话数据库包含敏感 LLM 对话内容。
- **修复方案**: 全面审查 backup_rules 排除项。

### S7. FileProvider 暴露 logs 目录
- **文件**: `app/src/main/res/xml/file_paths.xml`
- **状态**: [ ] 待修复
- **描述**: FileProvider 暴露 `logs/` 路径，有 URI 权限的接收方可读取日志文件。
- **修复方案**: 移除 logs 路径或限制权限。

---

## 设计架构

### D1. Tools 和 KnowledgeBase 功能模块为空桩
- **文件**: `feature/tools/impl/` + `feature/knowledgebase/impl/`
- **状态**: [ ] 待修复
- **描述**: 两个 ViewModel 为空，Screen 仅渲染空 Box。编译通过但零功能。
- **修复方案**: 实现功能或从导航中隐藏。

### D2. Settings 模块无 API 子模块
- **文件**: `core/navigation/.../FeatureNavKeys.kt`（含 SettingsNavKey）
- **状态**: [ ] 待修复
- **描述**: `SettingsNavKey` 放在 `core:navigation` 而非 `feature:settings:api`，破坏了所有其他 feature 的 API/Impl 模式。
- **修复方案**: 创建 `feature:settings:api` 或接受此模式不一致。

### D3. Search API strings.xml 编码损坏
- **文件**: `feature/search/api/src/main/res/values/strings.xml`
- **状态**: [ ] 待修复
- **描述**: 多个中文字符串含 `�` 替换字符（保存为 ANSI 而非 UTF-8）。运行时显示乱码。
- **修复方案**: 以 UTF-8 重新保存并修复被破坏的字符串。

### D4. 预发布依赖过多
- **文件**: `gradle/libs.versions.toml`（14+ alpha/rc 版本）
- **状态**: [ ] 监控
- **描述**: Compose BOM、Material3 Adaptive、Compose Foundation、Hilt Lifecycle Compose、Metrics、Tracing、RichText、Security Crypto 均为 alpha/rc 版本。API 可能在不通知下变更。
- **修复方案**: 尽可能升级到稳定版，记录剩余预览依赖的理由。

### D5. assistant-email 使用 JVM 插件但其他 assistant 使用 Android
- **文件**: `jasmine-core/assistant/assistant-email/build.gradle.kts`
- **状态**: [ ] 待修复
- **描述**: `assistant-email` 使用 `kotlin.jvm` 而非 `android.library`，但其他 `assistant-*` 模块依赖它。可能存在 API 不兼容。
- **修复方案**: 统一为 `android.library` 或明确其为纯 JVM 模块。

### D6. prompt-mnn 只支持 arm64-v8a
- **文件**: `jasmine-core/prompt/prompt-mnn/build.gradle.kts:13-14`
- **状态**: [ ] 待修复
- **描述**: `ndk { abiFilters += "arm64-v8a" }` 无 x86_64 模拟器支持，无 32 位 ARM 设备支持。
- **修复方案**: 添加 x86_64（如果 MNN 库支持）以支持模拟器调试。

### D7. config-manager 过度依赖 prompt-executor
- **文件**: `jasmine-core/config/config-manager/build.gradle.kts:26`
- **状态**: [ ] 待修复
- **描述**: `ConfigRepository` 实际需要的类型来自 `prompt-llm`，但依赖了 `prompt-executor`（传递引入 OkHttp/Retrofit）。
- **修复方案**: 降级依赖为 `prompt-llm`。

### D8. 双 JasmineDatabase 同名类
- **文件**: `core/database/.../JasmineDatabase.kt` 和 `jasmine-core/conversation/.../JasmineDatabase.kt`
- **状态**: [x] 已在 M10 中标注已修复
- **描述**: 两个同名的 Room Database 类，容易混淆。
- **修复方案**: 确认已修复，重命名其一。

### D9. 空 Notifier 接口
- **文件**: `core/notifications/.../Notifier.kt`
- **状态**: [ ] 待修复
- **描述**: 接口完全为空（零方法），两个实现（`NoOpNotifier`/`SystemTrayNotifier`）均无任何功能代码。demo/prod flavor 分离做无用功。
- **修复方案**: 实现功能或删除模块。

### D10. `@file:Suppress` 技能回退兼容层
- **文件**: `feature/skills/api/.../Skill.kt`
- **状态**: [ ] 观察
- **描述**: 整个文件是类型别名（重新导出 core.model），有 `@file:Suppress("DELEGATED_MEMBER_HIDING_SUPERTYPE")` 抑制警告。纯回退兼容层。
- **修复方案**: 消费者直接引用 core.model 后删除此文件。

### D11. SkillManager 使用次构造函数重新初始化
- **文件**: `core/domain/.../SkillManager.kt`
- **状态**: [ ] 待修复
- **描述**: `@Singleton` 中有多个 `var` 字段和次构造重新初始化，与依赖注入模式冲突。
- **修复方案**: 全部使用 `@Inject` 构造+`val` 字段。

### D12. AnsiParser 仅处理 SGR 和 OSC 序列
- **文件**: `feature/sandbox/impl/.../AnsiParser.kt`
- **状态**: [ ] 观察
- **描述**: CSI 序列无终结 'm' 字节被静默消费并无效果。完整 VT100/ANSI 终端模拟需要处理更多序列类型。
- **修复方案**: 按需扩展 CSI 序列支持，当前够用但非完整实现。

---

## 代码质量

### Q1. MCP HttpClient 无重试逻辑
- **文件**: `jasmine-core/agent/agent-mcp/.../HttpMcpClient.kt`
- **状态**: [ ] 待修复
- **描述**: 不像 prompt-executor 有 `executeWithRetry`，MCP HTTP 无重试。网络波动直接导致未处理异常。
- **修复方案**: 添加重试配置。

### Q2. MCP rpcNotify 忽略 HTTP 错误
- **文件**: `jasmine-core/agent/agent-mcp/.../HttpMcpClient.kt:175-195`
- **状态**: [ ] 待修复
- **描述**: 通知响应关闭前不检查状态码。`notifications/initialized` 发送失败完全不被检测。
- **修复方案**: 检查响应状态码并记录警告。

### Q3. API EmbeddingService 吞掉所有错误
- **文件**: `jasmine-core/rag/rag-embedding-api/.../ApiEmbeddingService.kt:58`
- **状态**: [x] 已修复 (添加 IOException 和 Exception 分层日志)
- **描述**: `catch (e: Exception) { null }` 使嵌入失败和有效空结果无法区分。
- **修复方案**: 至少记录日志或返回 Result 类型。

### Q4. ChatScreen "+" 按钮死控件
- **文件**: `feature/chat/impl/.../ChatScreen.kt:238`
- **状态**: [ ] 待修复
- **描述**: `onAddClick` 回调仅有 TODO 注释 `/* TODO: 将来在此打开工具/附件面板 */`。按钮点击无响应。
- **修复方案**: 实现功能或隐藏按钮。

### Q5. LicensesScreen 使用硬编码中文字符串
- **文件**: `feature/settings/impl/.../LicensesScreen.kt`
- **状态**: [ ] 待修复
- **描述**: `"开源许可证"`、`"返回"` 等硬编码，非字符串资源。不支持国际化。
- **修复方案**: 使用 `R.string.*` 资源。

### Q6. LicensesViewModel 使用脆弱的资源查找
- **文件**: `feature/settings/impl/.../LicensesViewModel.kt`
- **状态**: [x] 已修复 (改为优雅降级，返回错误状态而非崩溃)

### Q7. RenderChip 选中态闪烁
- **文件**: `jasmine-core/prompt/prompt-ui/.../UiRenderer.kt:637-644`
- **状态**: [ ] 待修复
- **描述**: `var selected by remember { mutableStateOf(false) }` 是局部状态，父重组时重置为 false。Chip 选中态不持续。
- **修复方案**: 将 selected 提升为 node 的属性或外部 StateFlow。

### Q8. ChatViewModel._lastExceptionCause 模式过度复杂
- **文件**: `feature/chat/impl/.../ChatViewModel.kt:210`
- **状态**: [ ] 待修复
- **描述**: 使用 `MutableStateFlow<java.util.Optional<Throwable>>` 承载一次性错误，再用 `onEach {}` 重置。过度复杂。
- **修复方案**: 使用 `SharedFlow` 或 `Channel`。

### Q9. StreamingClient 硬编码 Dispatchers.Main
- **文件**: `ClaudeClient.kt:292,299`, `GeminiClient.kt:298`, `OpenAICompatibleClient.kt:293,299`, `VertexAIClient.kt:320`
- **状态**: [ ] 待修复
- **描述**: 所有流式客户端使用 `Dispatchers.Main` 推送 token 回调，单元测试环境不可用。
- **修复方案**: 接受可配置 dispatcher 或使用 `Dispatchers.Main.immediate`。

### Q10. build-proot.sh exit 在子 shell 中无效
- **文件**: `linux-sandbox/build-proot.sh:84,96`
- **状态**: [ ] 待修复
- **描述**: `find_ndk()` 含 `exit 1` 但通过命令替换调用。`exit` 仅终止子 shell，且 `set -euo pipefail` 不触发（变量赋值例外）。脚本以 `NDK=""` 继续。
- **修复方案**: 检查返回值或使用 `trap` 处理。

### Q11. 下载无 Content-Length 时无进度回调
- **文件**: `linux-sandbox/.../RootfsDownloader.kt:45-46`
- **状态**: [ ] 待修复
- **描述**: `totalBytes > 0` 条件不满足时 `onProgress` 从不调用。UI 显示无进度但实际在下载。
- **修复方案**: 显示不确定进度指示器。

### Q12. Alpine 版本硬编码
- **文件**: `linux-sandbox/.../LinuxDistro.kt:7`
- **状态**: [ ] 待修复
- **描述**: `const val VERSION = "3.23.3"` — 点版本发布后旧归档从 CDN 移除，URL 404 且无回退。
- **修复方案**: 最低限度添加 fallback URL 或版本协商机制。

### Q13. 验证缺失时静默跳过校验和
- **文件**: `linux-sandbox/.../LinuxSandboxManager.kt:173-179`
- **状态**: [ ] 待修复
- **描述**: 校验和文件为空/格式错误/HTTP 失败时静默跳过。未经验证的 rootfs 被使用。
- **修复方案**: 至少要求校验和必须有或明确展示警告给用户。

### Q14. getLinuxArch 静默回退
- **文件**: `linux-sandbox/.../LinuxSandboxManager.kt:62-63`
- **状态**: [ ] 待修复
- **描述**: 未知 ABI（如 riscv64）静默回退到 "aarch64"，无日志。下载错误架构会以混淆错误失败。
- **修复方案**: 添加日志且不应静默回退。

### Q15. Dispatchers.IO 上 StateFlow 更新被 UI 消费
- **文件**: `linux-sandbox/.../AndroidSandboxController.kt:13-14`
- **状态**: [ ] 观察
- **描述**: init 块在 `Dispatchers.IO` 启动并更新 `_status`。StateFlow 线程安全但 UI 收集方需自行切线程。
- **修复方案**: 文档说明或添加 `.flowOn(Dispatchers.Main)`。

---

## 修复优先级建议

### 第一阶段 — 编译通过 (立即)
| 优先级 | 问题 | 理由 |
|--------|------|------|
| 1 | CE3: DuckDuckGoSearchService 双 companion object | Kotlin 不允许 |
| 2 | CE2: Navigation.kt 缺少 remember 导入 | 编译失败 |
| 3 | CE1: Runtime.kt 调用 createEmpty() 不存在 | 编译失败 |
| 4 | CE7: KSP 版本无效 | 依赖解析失败 |
| 5 | CE8: 重复 resources 标签 | XML 解析失败 |
| 6 | CE4/CE5/CE6: 测试引用不存在的类 | 编译失败 |

### 第二阶段 — 数据完整性和线程安全 (2-3天)
| 优先级 | 问题 | 理由 |
|--------|------|------|
| 7 | B1: ConversationRepository 非事务 | 数据不一致 |
| 8 | B2: MessageEntity 丢失瞬态字段 | 功能缺失 |
| 9 | B4: Gemini 重复工具调用 | 功能错误 |
| 10 | B11: skillLoaded 线程不安全 | 竞态 |
| 11 | B12/B13: 沙盒竞态 | 文件损坏 |
| 12 | B5: GOAPPlanner === 引用相等 | 运行时崩溃 |

### 第三阶段 — 安全 (1-2天)
| 优先级 | 问题 | 理由 |
|--------|------|------|
| 13 | S1: 明文弱密码 | 密钥泄露 |
| 14 | S2: 腾讯镜像供应链 | 中间人风险 |
| 15 | S3-S7: 其他安全项 | 综合安全加固 |

### 第四阶段 — 资源泄露和稳定性 (2-3天)
| 优先级 | 问题 | 理由 |
|--------|------|------|
| 16-23 | R1-R8: 资源泄露 | 长期运行退化 |
| 24-29 | B3/B6-B10/B14-B22: 逻辑缺陷 | 功能正确性 |

### 第五阶段 — 设计和代码质量 (按需)
| 优先级 | 问题 |
|--------|------|
| 30+ | D1-D12, Q1-Q15 |

---

*此文档将随修复进度更新。每个问题修复后标记 `[x]`。*
