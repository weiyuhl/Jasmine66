# Jasmine

AI 驱动的 Android 移动应用，集成多模型 LLM 聊天、Agent 框架、Linux 沙盒环境和可扩展技能系统。

## 技术栈

| 领域 | 技术 | 版本 |
|------|------|------|
| UI | Jetpack Compose + Material 3 + Adaptive Layout | BOM 2025.09.01 |
| 架构 | MVVM + Clean Architecture + UDF | - |
| DI | Hilt (Dagger) | 2.59 |
| 导航 | Navigation 3 (Compose, 类型安全) | 1.0.0 |
| 本地存储 | Room + Proto DataStore | Room 2.8.3 / DS 1.2.0 |
| 网络 | Retrofit + OkHttp + Ktor Client | Retrofit 2.11.0 / Ktor 3.4.2 |
| 序列化 | Kotlinx Serialization | 1.8.0 |
| 语言 | Kotlin | 2.3.0 |
| 构建 | Gradle Kotlin DSL | AGP 9.0.0 / Gradle 8.x |
| JDK | Java 17 (编译), JVM Target 11 | - |

## 核心功能

- **多模型 LLM 聊天**：支持 OpenAI、Claude、Gemini、DeepSeek、硅基流动、OpenRouter、本地 MNN 推理
- **Agent 框架**：工具调用循环、图策略引擎、GOAP/LLM 规划器、子 Agent、MCP 协议
- **动态 UI**：LLM 可生成交互式 Compose UI（表单、按钮、选择器、表格等 30 种组件），在聊天界面中渲染
- **Linux 沙盒**：通过 PRoot (ptrace) 在无 Root Android 上运行 Alpine Linux，支持终端和包管理
- **联网搜索**：DuckDuckGo Instant Answer API，支持时间/地区筛选
- **技能系统**：内置 8 个技能（JS/Native），支持导入、密钥管理，选中技能指令自动注入 LLM 提示词
- **对话存储**：Room 数据库持久化对话历史和 token 用量
- **供应商配置**：支持 API Key、Base URL、模型选择、采样参数、系统提示词自定义

## 模块架构（42 个模块）

```
app/                              # 主应用入口
app-jasmine-catalog/              # UI 组件目录（开发用）

build-logic/convention/           # 11 个自定义 Gradle 约定插件

core/                             # 13 个核心模块
├── analytics/                    # 分析埋点抽象
├── common/                       # 协程调度器、Result 类型
├── data/                         # 数据层（Repository、ChatClientManager、Agent 工具注册）
├── database/                     # Room 数据库（搜索历史）
├── datastore/                    # Proto DataStore（用户偏好）
├── datastore-proto/              # Protobuf 定义
├── designsystem/                 # Material 3 主题、组件库、Markdown 渲染
├── domain/                       # 领域层（SkillManager）
├── model/                        # 纯数据类（无 Android 依赖）
├── navigation/                   # 双栈导航系统
├── network/                      # OkHttp/Retrofit/Coil 基础设施
├── notifications/                # 通知抽象
└── ui/                           # 共享 UI 工具（Jank 追踪、设备预览）

jasmine-core/                     # AI 引擎核心（18 个模块）
├── prompt/
│   ├── prompt-model              # 消息模型、Token 估算、Claude/Gemini 模型定义
│   ├── prompt-llm                # ChatClient 接口、ContextManager、压缩策略、系统上下文
│   ├── prompt-executor           # 各供应商客户端实现（ChatClientFactory）
│   ├── prompt-ui                 # 动态 UI 节点模型、JSON 解析器、Compose 渲染器
│   └── prompt-mnn                # 本地 MNN 推理（ARM64 JNI）
├── agent/
│   ├── agent-tools               # 20+ 工具（文件、Shell、Web、子 Agent、用户交互）
│   ├── agent-observe             # 事件系统、Tracing、AgentCheckpoint
│   ├── agent-graph               # 图策略引擎（DAG + DSL + Pipeline）
│   ├── agent-planner             # GOAP A* 规划器 + LLM 规划器（含 Critic）
│   ├── agent-mcp                 # MCP 协议（HTTP JSON-RPC + SSE）
│   └── agent-runtime             # 运行时组装（工具注册、MCP 连接、检查点）
├── config/config-manager         # ~180 个配置方法
├── conversation/conversation-storage  # Room 对话/消息/token 用量存储
├── assistant/                    # 助手运行时（心跳、记忆、调度）
└── rag/                          # RAG 嵌入服务

feature/                          # 功能模块（API/Impl 分离）
├── chat/{api,impl}               # 聊天（ChatScreen、ChatViewModel、WebView JS 沙盒）
├── search/{api,impl}             # 应用内搜索
├── settings/impl                 # 设置、供应商配置、许可证
├── sandbox/{api,impl}            # Linux 沙盒管理、终端、ANSI 解析
├── skills/{api,impl}             # 技能管理
├── tools/{api,impl}              # 工具页（占位）
└── knowledgebase/{api,impl}      # 知识库（占位）

linux-sandbox/sandbox/            # Linux 沙盒核心（PRoot、Alpine rootfs、Shell 执行）

websearch/                        # DuckDuckGo API 联网搜索

skills/                           # 技能定义文件（SKILL.md）
├── built-in/                     # 8 个内置技能
└── featured/                     # 3 个精选技能
```

## 架构原则

1. **API/Impl 分离**：feature 模块拆分为 `api`（NavKey + 公共接口）和 `impl`（Screen + ViewModel）
2. **严格分层隔离**：feature 层禁止直接依赖 jasmine-core，必须通过 core:data 封装
3. **单向数据流**：UI → ViewModel → Repository → DataSource，使用 Kotlin Flow
4. **约定插件统一配置**：多模块共享的 AGP/Kotlin/Compose/Hilt 配置由 build-logic 的 11 个约定插件管理

## 关键数据流

```
ChatScreen (Compose)
  → ChatViewModel.onSendClick()
    → userDataRepository.userData.first()  ← 读取 uiEnabled, webSearchEnabled
    → ChatClientManager.streamChat(messages, model, uiEnabled, webSearchEnabled)
      → buildApiMessagesWithSystemPrompt()
        ← SystemPromptManager + SkillManager.getSelectedSkillsInstructions()
        ← webSearchEnabled ? 注入搜索指引 : 无
      → 工具过滤: webSearchEnabled ? 全部 : 移除 web_search
      → ChatClient → LLM API (SSE 流式)
      → Agent 循环 (executeToolLoop): LLM → 工具调用 → 结果追加 → 迭代
    → ChatScreen 渲染: MarkdownText + UiParser → UiRenderer (动态 UI)
```

## 构建命令

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK（需 keystore.properties 配好签名）
./gradlew :app:assembleRelease

# 代码格式化
./gradlew spotlessApply

# 运行测试
./gradlew test
```

APK 输出路径: `app/build/outputs/apk/{debug,release}/app-{debug,release}.apk`

## 签名配置

Release 签名通过 `keystore.properties` 配置（已 gitignore）：

```properties
storeFile=jasmine-release.jks
storePassword=xxx
keyAlias=jasmine-release
keyPassword=xxx
```

## 测试

| 模块 | 测试文件数 | 覆盖 |
|------|----------|------|
| jasmine-core (prompt/agent/config/conversation) | 40+ | 模型、LLM 客户端、Agent 工具、MCP、可观测性 |
| core (navigation/datastore/database/common/data) | 7 | 导航、偏好、搜索、结果 |
| feature/settings | 1 | SettingsViewModel |
| linux-sandbox | 4 | PRoot 执行、rootfs 下载、沙盒状态、进程管理 |

## 发送 Release

1. `git checkout main && git pull`
2. 更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`
3. 确保 `keystore.properties` 和 `jasmine-release.jks` 存在
4. `./gradlew :app:assembleRelease`
5. APK 位于 `app/build/outputs/apk/release/app-release.apk`
