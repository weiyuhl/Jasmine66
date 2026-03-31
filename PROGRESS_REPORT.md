# Jasmine 项目开发进度

## 已完成

### 1. 主界面架构重构
- [x] 清理 `JasmineApp.kt`，移除 `ChatComposer`、`EmptyStateView` 等特定功能代码
- [x] 将 `ChatComposer` 迁移到 `feature:chat:impl` 模块（`ChatScreen.kt` + `ChatViewModel.kt`）
- [x] 所有 UI 组件统一使用 `MaterialTheme.colorScheme` 代替硬编码颜色
- [x] 删除无法编译的旧测试文件 `NavigationTest.kt`

### 2. 键盘（IME）适配修复
- [x] 在 `AndroidManifest.xml` 中添加 `android:windowSoftInputMode="adjustResize"`
- [x] 使用手动计算 `WindowInsets.ime` 减去底部导航栏高度的方式，实现输入框平滑跟随键盘动画
- [x] 彻底解决了键盘弹出时的跳动（Jitter）和悬空（Gap）问题

### 3. 空状态动画
- [x] 从 `koog` 项目复制 `Coding_Slide.json` 到 `feature/chat/impl/src/main/assets/`
- [x] 引入 `compottie 2.0.0-rc02` 库（Compose 原生 Lottie 引擎）
- [x] 实现 `EmptyStateView`（无限循环，0.6 倍速播放）

### 4. 底部面板（Bottom Sheet）
- [x] 为聊天输入框 "+" 按钮添加点击事件
- [x] 集成 Material 3  `ModalBottomSheet`，点击 "+" 弹出扩展功能面板

### 5. jasmine-core 目录迁移
- [x] 将 `koog/jasmine-core` 完整复制到项目根目录
- [x] 删除 `koog` 文件夹

### 6. jasmine-core 基础聊天模块接入
- [x] 在 `libs.versions.toml` 中添加 `okhttp` 和 `okhttp-mockwebserver` 库别名
- [x] 在 `settings.gradle.kts` 中注册 3 个子模块：
  - `jasmine-core:prompt:prompt-model`（数据模型）
  - `jasmine-core:prompt:prompt-llm`（ChatClient 接口）
  - `jasmine-core:prompt:prompt-executor`（OpenAI/DeepSeek/Claude/Gemini 等 API 实现）
- [x] 3 个模块 build 文件统一使用 `jasmine.android.library` 约定插件
- [x] `feature:chat:impl` 已添加 `implementation(projects.jasmineCore.prompt.promptExecutor)`
- [x] **Kotlin 编译全部通过**（82 个任务 UP-TO-DATE）

---

### 7. 网络/TLS 问题 — ✅ 已解决

**问题：** `assembleRelease` 打包时无法从 `dl.google.com` 下载 `annotation-1.2.0.jar`（TLS handshake 失败）。

**解决方案：** 在 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中添加了阿里云镜像：
```kotlin
maven {
    url = uri("https://maven.aliyun.com/repository/google")
    content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
    }
}
```

---

### 8. 接入真实 LLM 流式对话
- [x] 创建 `ChatProviderRepository.kt` — SharedPreferences 轻量供应商配置存储
- [x] 内置 5 个供应商预设（DeepSeek、OpenAI、Claude、Gemini、硅基流动）
- [x] 重写 `ChatViewModel.kt` — 接入 `ChatClientFactory.create()` + `chatStreamWithUsage()` 流式对话
- [x] 消息列表状态管理（`UiChatMessage` 数据类 + 流式追加）
- [x] 错误处理与生命周期管理（`ChatClient.close()`）

### 9. 供应商配置 UI & 消息列表
- [x] 重写 `ChatScreen.kt` — 消息气泡列表（`LazyColumn` + 自动滚动）
- [x] 复用 "+" 按钮的 Bottom Sheet 改为供应商配置表单
- [x] 配置表单包含：供应商选择、API Key、Base URL、模型名称、保存按钮
- [x] 未配置时显示提示条，已配置后可直接发送消息
- [x] Kotlin 编译全部通过 ✅

---

## 待开发

- [ ] 接入 `config-manager` 模块（需先解耦 agent 依赖）
- [ ] 接入 `conversation-storage` 模块实现聊天历史持久化
- [ ] 工具调用（Tool Calls）/ Agent 模式
