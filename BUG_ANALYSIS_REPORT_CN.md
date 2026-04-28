# Jasmine 项目代码问题与Bug全面分析报告

> 分析时间：2026-04-28
> 分析范围：全部 ~445 个 Kotlin 源文件，覆盖所有模块
> 分析方法：纯源码静态审查，不依赖 git 提交历史

---

## 目录

- [一、严重程度统计](#一严重程度统计)
- [二、致命级问题 (CRITICAL)](#二致命级问题-critical)
- [三、高危级问题 (HIGH)](#三高危级问题-high)
- [四、中危级问题 (MEDIUM)](#四中危级问题-medium)
- [五、低危级问题 (LOW)](#五低危级问题-low)

---

## 一、严重程度统计

| 严重程度 | 数量 |
|---------|------|
| 致命 (CRITICAL) | 8 |
| 高危 (HIGH) | 35 |
| 中危 (MEDIUM) | 38 |
| 低危 (LOW) | 22 |
| **合计** | **103** |

---

## 二、致命级问题 (CRITICAL)

### C-01: GOAPPlanner 始终选择最差方案而非最优方案

- **文件**: `jasmine-core/agent/agent-planner/.../GOAPPlanner.kt` 第99-102行
- **问题**: `goals.mapNotNull { ... }.minByOrNull { it.value }` 选择了 value 最小的方案。但 `GOAPGoal.value` 定义为 `exp(-cost)`，value 越高代表 cost 越低（方案越优）。应使用 `maxByOrNull` 选择最优方案。
- **影响**: Agent 规划器在所有场景下都会选择最差的执行方案，导致 Agent 行为质量严重下降。

### C-02: SseMcpClient 连接失败时永久挂起

- **文件**: `jasmine-core/agent/agent-mcp/.../SseMcpClient.kt` 第154-156行
- **问题**: `startSseListener()` 中 OkHttp 的 `Callback.onFailure()` 方法体为空，连接错误被静默吞掉。`endpointReady` CompletableDeferred 永远不会完成，导致 `connect()` 中的 `endpointReady.await()` 永久阻塞。
- **影响**: MCP 服务器连接失败时，整个 Agent 启动流程永久挂起，无法恢复。

### C-03: SMTP 邮件头注入漏洞（安全）

- **文件**: `jasmine-core/assistant/assistant-email/.../SmtpClient.kt` 第42-63行
- **问题**: `sendReply` 方法将 `from`、`to`、`subject`、`body`、`inReplyTo` 直接拼接到 SMTP 头部，未做任何清洗。包含 `\r\n` 的恶意 subject 或 from 值可注入任意 SMTP 头部（如 BCC 到额外收件人）。
- **影响**: 攻击者或 LLM 异常输出可导致发送垃圾邮件或数据泄露。

### C-04: InMemoryConfigRepository 引用不存在的枚举值（编译错误）

- **文件**: `jasmine-core/config/config-manager/.../InMemoryConfigRepository.kt` 第125-128行
- **问题**: 代码引用了 `AgentStrategyType.AUTO`、`GraphToolCallMode.AUTO`、`ToolSelectionStrategyType.AUTO`，但这些枚举值在 `AgentConfig.kt` 中不存在。实际枚举值为 `SIMPLE_LOOP`、`SINGLE_RUN_GRAPH`、`SEQUENTIAL`、`PARALLEL` 等。
- **影响**: 编译失败，或运行时崩溃。

### C-05: MemoryStore.forget 返回类型与调用方不匹配（编译错误）

- **文件**: `jasmine-core/assistant/assistant-memory/.../MemoryStore.kt` 第97-99行
- **文件**: `jasmine-core/assistant/assistant-tools/.../MemoryTools.kt` 第113行
- **问题**: `MemoryStore.forget()` 返回 `MemoryEntry?`（被移除的值或 null），但 `MemoryTools` 中用 `if (removed)` 将其当作 Boolean 使用。Kotlin 中 `MemoryEntry?` 不能隐式转换为 Boolean。
- **影响**: 编译失败，forget 工具完全不可用。

### C-06: 多个文件缺少 package 声明（编译错误）

- **文件**: `jasmine-core/assistant/assistant-memory/.../MemoryStore.kt` 第1行
- **文件**: `jasmine-core/assistant/assistant-scheduler/.../TaskStore.kt` 第1行
- **文件**: `jasmine-core/assistant/assistant-email/.../ServerAutoDetect.kt` 第1行
- **问题**: 这些文件缺少 `package` 声明，导致类位于默认包中。其他文件通过完整包路径 import 这些类时将无法解析。`ServerAutoDetect.kt` 还缺少 `@Serializable` 的 import。
- **影响**: 编译失败，所有依赖这些类的模块都无法构建。

### C-07: FilePersistenceStorageProvider 反序列化丢失 properties 字段

- **文件**: `jasmine-core/agent/agent-observe/.../PersistenceStorageProvider.kt` 第191-233行
- **问题**: `serializeCheckpoint` 正确写入了 `properties` map，但 `deserializeCheckpoint` 从未读取它。反序列化后 `properties` 始终为 null，导致 `isTombstone()` 始终返回 false。
- **影响**: 已完成的 Agent 会话在应用重启后会被错误地显示为可恢复状态，tombstone 机制完全失效。

### C-08: 签名凭据提交到版本控制（安全）

- **文件**: `keystore.properties`
- **问题**: 文件包含明文密码（`storePassword=jasmine2026`、`keyPassword=jasmine2026`），已被提交到仓库。虽然 `.gitignore` 列出了该文件，但它已存在于工作树中。
- **影响**: 任何有仓库访问权限的人都能看到发布签名凭据，应立即轮换密码并从 git 历史中移除。

---

## 三、高危级问题 (HIGH)

### H-01: 流式传输客户端协程作用域泄漏与竞态条件

- **文件**: `jasmine-core/prompt/prompt-executor/.../ClaudeClient.kt` 第235-237行
- **文件**: `jasmine-core/prompt/prompt-executor/.../OpenAICompatibleClient.kt` 第253-255行
- **文件**: `jasmine-core/prompt/prompt-executor/.../GeminiClient.kt` 第251-253行
- **文件**: `jasmine-core/prompt/prompt-executor/.../VertexAIClient.kt` 第279-281行
- **问题**: 在 OkHttp 回调线程中创建 `CoroutineScope` 并用 `launch` 发射 fire-and-forget 协程来传递 chunk。这些协程是孤立的，`continuation.resume(Unit)` 后 chunk 传递协程可能仍在执行，导致对 `StringBuilder`（fullContent）的并发读写竞态。
- **影响**: 流式响应内容可能损坏或丢失，异常被静默吞掉。

### H-02: continuation.resume/resumeWithException 可能被多次调用

- **文件**: 同 H-01 涉及的四个流式客户端
- **问题**: 在 OkHttp `onResponse` 回调中，如果响应不成功则调用 `resumeWithException`，但后续代码路径可能再次调用 `resume` 或 `resumeWithException`。对 `CancellableContinuation` 多次调用 resume 会抛出 `IllegalStateException`。
- **影响**: 应用崩溃，出现 "Already resumed" 异常。

### H-03: MnnConfig.toJson() 未转义 systemPrompt 字符串

- **文件**: `jasmine-core/prompt/prompt-mnn/.../MnnConfig.kt` 第22行
- **问题**: `systemPrompt` 直接插入 JSON 字符串，未转义引号、反斜杠、换行符等特殊字符。系统提示词中包含这些字符非常常见。
- **影响**: 生成的 JSON 格式错误，MNN 初始化失败或行为异常。

### H-04: MnnChatClient.ensureSession 非线程安全（原生内存泄漏）

- **文件**: `jasmine-core/prompt/prompt-mnn/.../MnnChatClient.kt` 第41-65行
- **问题**: `session` 字段的检查和赋值没有同步。两个协程同时调用 `ensureSession` 时，都会创建新的 `MnnLlmSession` 并调用 `init()`，第一个原生会话指针丢失且永远无法释放。
- **影响**: MNN 模型会话的原生内存泄漏，可能达数百 MB 到 GB 级别。

### H-05: @Transaction 注解在非 DAO 类上无效

- **文件**: `jasmine-core/conversation/conversation-storage/.../ConversationRepository.kt` 第127、145行
- **问题**: `@Transaction` 是 Room 注解，仅在 `@Dao` 接口/抽象类的方法上生效。`ConversationRepository` 是普通类，注解被静默忽略。`addMessage()` 执行 insertMessage + updateConversation 两个独立操作，无原子性保证。
- **影响**: 应用崩溃或协程取消时，消息已保存但会话的 `updatedAt` 未更新，数据不一致。

### H-06: AgentEventBus 缓冲区满时丢弃 continuation 导致 RunJsTool 挂起60秒

- **文件**: `core/data/.../AgentEventBus.kt` 第27-29行
- **文件**: `core/data/.../RunJsTool.kt` 第71-76行
- **问题**: `emitJsEvent()` 在 SharedFlow 缓冲区满（容量64）时记录警告并返回，但 `CallJsEvent` 中包含的 `CancellableContinuation<String>` 永远不会被 resume。`RunJsTool` 中的协程将等待完整的60秒超时。
- **影响**: JS 工具调用在高负载下无故等待60秒才返回超时错误。

### H-07: SkillJsSandbox.validateUrl 拒绝所有合法的 file:///android_asset/ URL

- **文件**: `core/data/.../SkillJsSandbox.kt` 第148-155行
- **问题**: 对于 `file:///android_asset/skills/foo/scripts/index.html`，`java.net.URI` 将 authority 解析为空/null（三斜杠表示空 authority）。代码检查 `uri.authority == "android_asset"` 将始终为 false，因为 `android_asset` 实际上是路径的第一段。
- **影响**: 所有本地技能脚本的沙箱路径验证失败，技能执行被阻断。

### H-08: SkillJsSandbox JS 字符串转义不完整（安全）

- **文件**: `core/data/.../SkillJsSandbox.kt` 第109-114行
- **问题**: `buildJsPollScript` 转义了反斜杠、反引号和美元符号，但未转义 `\r`、`\n`，且 `$` 的转义在 JS 模板字面量中仍可被解释为 `${...}` 表达式。
- **影响**: 包含 `${...}` 的数据可在模板字面量中执行任意 JavaScript，突破沙箱安全模型。

### H-09: SkillJsSandbox WebView 操作在非 UI 线程调用

- **文件**: `core/data/.../SkillJsSandbox.kt` 第66-104行
- **问题**: `executeSkill` 是 suspend 函数，直接调用 `webView.addJavascriptInterface()`、`webView.loadUrl()` 等。Android 的 WebView 必须仅在主线程访问。如果从 `Dispatchers.IO` 调用，将崩溃。
- **影响**: 应用崩溃，抛出 `RuntimeException: A WebView method was called on thread 'DefaultDispatcher-worker-X'`。

### H-10: ToolLoopExecutor 错误回退丢失工具调用标识

- **文件**: `core/data/.../ToolLoopExecutor.kt` 第174-180行
- **问题**: 并行工具调用失败时，回退的 `ToolResult` 使用 `toolCallId = "error"` 和 `toolName = "unknown"`，丢失了与原始工具调用的关联。LLM API 要求 tool result 的 ID 必须匹配原始调用 ID。
- **影响**: 下一轮 API 调用可能失败或产生混乱响应，工具循环中断。

### H-11: ConversationDatabase 破坏性迁移静默销毁会话历史

- **文件**: `jasmine-core/conversation/conversation-storage/.../ConversationDatabase.kt` 第12行
- **问题**: `exportSchema = false` 加上 DI 模块中的 `fallbackToDestructiveMigration(dropAllTables = true)`，任何 schema 变更都会静默销毁所有用户会话数据。
- **影响**: 用户在应用更新后丢失全部聊天历史，无迁移路径。

### H-12: ProviderRegistry 非线程安全的可变列表

- **文件**: `jasmine-core/config/config-manager/.../ProviderRegistry.kt` 第13、27-58行
- **问题**: `_providers` 是普通 `mutableListOf()`，`initialize`、`registerProvider`、`getActiveConfig` 等方法并发访问无同步保护。
- **影响**: `ConcurrentModificationException` 或静默数据损坏。

### H-13: ExecuteShellCommandTool 进程输出未在 waitFor 期间排空

- **文件**: `jasmine-core/agent/agent-tools/.../ExecuteShellCommandTool.kt` 第203-204行
- **问题**: 进程完成后才读取 `process.inputStream`。如果进程产生大量输出，输出缓冲区满会导致进程阻塞，`waitFor` 永远不返回。输出流应与 `waitFor` 并发消费。
- **影响**: 产生大量输出的命令执行挂起或输出截断。

### H-14: ExecuteShellCommandTool 命令注入绕过（安全）

- **文件**: `jasmine-core/agent/agent-tools/.../ExecuteShellCommandTool.kt` 第167-171行
- **问题**: 命令字符串直接传递给 `sh -c`。黑名单/白名单策略基于简单关键词匹配，可被轻易绕过。例如 `cat file$(rm -rf /)` 不会被黑名单模式捕获。
- **影响**: 恶意命令可绕过安全检查执行破坏性操作。

### H-15: FetchUrlTool SSRF 保护可被 DNS 重绑定和重定向绕过（安全）

- **文件**: `jasmine-core/agent/agent-tools/.../FetchUrlTool.kt` 第178-203行
- **问题**: `validateUrl()` 解析主机名检查是否为私有地址，但 OkHttpClient 连接时进行独立的 DNS 解析。攻击者可使用 DNS 重绑定：验证时返回公网 IP，连接时 DNS 记录变为 `127.0.0.1`。且 `followRedirects(true)` 允许公网 URL 重定向到内网地址。
- **影响**: SSRF 攻击可访问内部服务。

### H-16: GetCurrentTimeTool 损坏 JSON 输出

- **文件**: `jasmine-core/agent/agent-tools/.../GetCurrentTimeTool.kt` 第78-79行
- **问题**: `deleteCharAt(length - 1)` 执行两次，第一次删除换行符，第二次删除 `second` 字段值的最后一位数字，导致 JSON 输出损坏。
- **影响**: 时间工具返回格式错误的 JSON，second 字段值被截断。

### H-17: McpConnectionManager.reconnect() 未受 mutex 保护

- **文件**: `jasmine-core/agent/agent-runtime/.../McpConnectionManager.kt` 第195-231行
- **问题**: `reconnect()` 直接访问 `clients`、`preloadedTools`、`connectionCache`（均为可变集合）而不持有 mutex。与 `loadToolsInto()` 或 `preconnect()` 并发时导致 `ConcurrentModificationException`。
- **影响**: MCP 重连时应用崩溃或数据损坏。

### H-18: McpConnectionManager MCP 禁用时 connecting 标志未重置导致30秒挂起

- **文件**: `jasmine-core/agent/agent-runtime/.../McpConnectionManager.kt` 第78-80行
- **问题**: 如果 `isMcpEnabled()` 返回 false，方法提前返回但未将 `connecting` 重置为 false。`loadToolsInto()` 进入忙等循环等待30秒后才放弃。
- **影响**: MCP 禁用时每次工具加载额外等待30秒。

### H-19: Windows 平台路径遍历检查可绕过（安全）

- **文件**: `jasmine-core/agent/agent-tools/.../WriteFileTool.kt` 第115行
- **文件**: `jasmine-core/agent/agent-tools/.../EditFileTool.kt` 第318行
- **文件**: `jasmine-core/agent/agent-tools/.../ReadFileTool.kt` 第266行
- **问题**: `resolved.path.startsWith(base.path)` 在 Windows 上可被绕过。例如 `basePath` 为 `C:\workspace` 时，`C:\workspace-evil\secret.txt` 也会通过检查。应使用 `startsWith(base.path + File.separator)`。
- **影响**: 路径遍历攻击可读写沙箱外的文件。

### H-20: FileTraceWriter 线程安全问题

- **文件**: `jasmine-core/agent/agent-observe/.../FileTraceWriter.kt` 第25-36行
- **问题**: `BufferedWriter` 从 `Dispatchers.IO` 的不同线程并发访问。并行工具调用时多个协程同时发射 trace 事件，写入可能交错产生损坏输出。
- **影响**: Trace 文件内容损坏，无法用于调试。

### H-21: TraceMessageFormat 中 SimpleDateFormat 非线程安全

- **文件**: `jasmine-core/agent/agent-observe/.../TraceMessageFormat.kt` 第13行
- **问题**: `SimpleDateFormat` 作为单例字段存储，并发调用 `format()` 时内部 `Calendar` 对象被损坏，产生乱码时间戳或 `ArrayIndexOutOfBoundsException`。
- **影响**: 并行工具执行时 trace 时间戳错误或崩溃。

### H-22: FilePersistenceStorageProvider JSON 解析器反斜杠处理缺陷

- **文件**: `jasmine-core/agent/agent-observe/.../PersistenceStorageProvider.kt` 第266-284行
- **问题**: `extractJsonStringValue` 的转义处理算法有缺陷，不计算连续反斜杠数量。对于 `\\\\"` 这样的字符串，解析器会错误地提前终止。
- **影响**: 包含反斜杠的检查点数据（代码片段、文件路径、正则表达式中常见）反序列化失败。

### H-23: Persistence currentVersion 未从持久化检查点恢复

- **文件**: `jasmine-core/agent/agent-observe/.../Persistence.kt` 第47行
- **问题**: `currentVersion` 每次创建实例时从0开始。应用重启后新检查点的版本号低于已有检查点，`getLatestCheckpoint` 返回旧检查点。
- **影响**: 检查点排序错误，恢复时使用过期数据。

### H-24: AgentPipeline.uninstall 未移除事件处理器

- **文件**: `jasmine-core/agent/agent-graph/.../AgentPipeline.kt` 第51-58行
- **问题**: `uninstall` 从 `registeredFeatures` 移除了 feature，但未清理对应的事件处理器。卸载后处理器继续触发，且由于 config 为 null 导致条件检查被跳过，处理器无条件运行。
- **影响**: 卸载 feature 后其行为不仅不停止，反而变为无条件执行——与预期完全相反。

### H-25: 重复的 LinuxSandboxManager 实例导致状态不一致

- **文件**: `feature/sandbox/impl/.../SandboxModule.kt` 第20-32行
- **问题**: Hilt DI 模块中 `provideLinuxSandboxManager()` 创建一个实例，`provideSandboxController()` 内部又创建一个独立实例。两个实例有独立的 CoroutineScope、状态流和 OkHttpClient。
- **影响**: ViewModel 通过不同实例操作沙箱，状态不一致，可能双重分配资源。

### H-26: ManageProcessTool 与 ExecuteShellCommandTool 使用不同的 ProcessManager 实例

- **文件**: `linux-sandbox/.../SandboxToolAdapter.kt` 第124-126行
- **问题**: `ManageProcessTool` 创建自己的默认 `ProcessManager`，`ExecuteShellCommandTool` 接收另一个实例。通过 shell 命令启动的后台进程对 manage 工具不可见。
- **影响**: 后台进程管理完全失效——kill、log、list 操作始终返回 "Unknown session"。

### H-27: ProotExecutor 静态线程池被永久关闭

- **文件**: `linux-sandbox/.../ProotExecutor.kt` 第44-58行
- **问题**: `ioExecutor` 是 `companion object` 的静态 `ExecutorService`。`shutdown()` 永久终止它，无法重建。`LinuxSandboxManager.destroy()` 调用后，所有后续 PRoot 命令执行都会失败。
- **影响**: 沙箱销毁/重建周期后永久不可用，直到进程重启。

### H-28: ChatViewModel Optional.get() 在 cause 为 null 时崩溃

- **文件**: `feature/chat/impl/.../ChatViewModel.kt` 第146-147行
- **问题**: `exceptionCause()` 用 `java.util.Optional` 包装 `_lastExceptionCause.value`，调用方用 `.get()` 获取。当异常没有 cause（很常见）时，`Optional.ofNullable(null).get()` 抛出 `NoSuchElementException`。
- **影响**: 错误处理器本身崩溃，无 cause 的异常导致应用闪退。

### H-29: LicensesViewModel 许可证索引解析错误

- **文件**: `feature/settings/impl/.../LicensesViewModel.kt` 第77-83行
- **问题**: 元数据格式为 `"index:length library_name"`，其中 index 是许可证文件的字节偏移量。代码将其当作行索引使用，导致几乎所有库显示错误的许可证名称或抛出 `IndexOutOfBoundsException`。
- **影响**: 许可证归属信息对大多数条目都是错误的。

### H-30: LicensesViewModel 错误状态被立即覆盖

- **文件**: `feature/settings/impl/.../LicensesViewModel.kt` 第53-63行
- **问题**: `parseLicenses()` 在错误时设置 `_state.value` 为错误状态并返回 `emptyList()`，但调用方 `loadLicenses()` 随后用返回的空列表覆盖状态，错误信息丢失。
- **影响**: 用户永远看不到 "许可证信息暂不可用" 的错误提示。

### H-31: SkillManagerViewModel 回调在 IO 线程更新 Compose 状态

- **文件**: `feature/skills/impl/.../SkillManagerViewModel.kt` 第110行
- **问题**: `withContext(ioDispatcher) { onResult(true, "技能已导入: ${skill.name}") }` 在 IO 线程调用回调，回调更新 Compose 的 `mutableStateOf` 状态。Compose 状态只能在主线程修改。
- **影响**: 从后台线程修改 Compose 状态导致崩溃或未定义行为。

### H-32: Tar 解压硬链接路径遍历漏洞（安全）

- **文件**: `linux-sandbox/.../RootfsDownloader.kt` 第119-124行
- **问题**: 对于硬链接（type flag `'1'`），代码从 tar 的 `linkName` 创建 `linkTarget` 但未执行规范路径检查（普通文件和符号链接有此检查）。恶意 tarball 可包含指向目标目录外文件的硬链接。
- **影响**: 路径遍历攻击可读取设备上的任意文件。

### H-33: 全局排版硬编码 LTR 方向，破坏 RTL 语言支持

- **文件**: `core/designsystem/.../Type.kt` 所有文本样式
- **问题**: `JasmineTypography` 中每个文本样式都硬编码 `textDirection = TextDirection.Ltr` 和 `textAlign = TextAlign.Left`。应用支持阿拉伯语、希伯来语等 RTL 语言，但所有文本都强制从左到右渲染。
- **影响**: RTL 语言用户的阅读体验完全破坏。

### H-34: MainActivityViewModel 错误时无限停留在启动画面

- **文件**: `app/.../MainActivityViewModel.kt` 第25-27行
- **问题**: `userData` flow 异常时 catch 块发射 `Loading` 状态。`shouldKeepSplashScreen()` 对 `Loading` 返回 true，启动画面永远不会消失。
- **影响**: 数据层失败时应用表现为永久卡在启动画面，无错误提示。

### H-35: IMAP 连接在轮询循环中泄漏

- **文件**: `jasmine-core/assistant/assistant-scheduler/.../TaskScheduler.kt` 第70-100行
- **问题**: `imap.logout()` 仅在无异常时到达。catch 块吞掉所有异常但不调用 `imap.logout()`。此方法在60秒轮询循环中运行。
- **影响**: IMAP 服务器间歇性故障时，每分钟泄漏一个 TCP/TLS 连接。

<!-- PLACEHOLDER_MEDIUM -->
