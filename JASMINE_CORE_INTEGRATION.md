# jasmine-core 接入指南

本文档描述 Jasmine 应用如何通过 `core:data` 层接入 `jasmine-core` 引擎库，以及开发新功能时应遵循的架构规范。

---

## 模块架构

```
app
 └── feature:chat:impl          ← UI / ViewModel（纯展示层）
      └── core:data              ← 业务逻辑 / 数据仓库（抽象层）
           └── jasmine-core:prompt:prompt-executor   ← LLM 引擎（底层库）
                └── jasmine-core:prompt:prompt-llm
                     └── jasmine-core:prompt:prompt-model
```

> **核心原则：`feature` 模块禁止直接依赖 `jasmine-core`。**
> 所有对 `jasmine-core` 的调用必须通过 `core:data`（或其他 `core:*` 模块）封装后暴露。

---

## 关键组件

### 1. `core:data` — 抽象层

| 文件 | 职责 |
|---|---|
| `ChatProviderRepository` | SharedPreferences 持久化：供应商选择、API Key、Base URL、模型名称 |
| `ChatClientManager` | ChatClient 生命周期管理：创建 / 重建 / 销毁，封装 `ChatClientFactory` |
| `SimpleChatMessage` | 消息模型抽象，替代 `jasmine-core` 的 `ChatMessage` |
| `StreamChatResult` | 流式结果抽象，替代 `jasmine-core` 的 `StreamResult` |

#### 依赖声明 (`core/data/build.gradle.kts`)

```kotlin
dependencies {
    // ... 其他 core 依赖
    implementation(projects.jasmineCore.prompt.promptExecutor)
}
```

### 2. `feature:chat:impl` — UI 层

| 文件 | 职责 |
|---|---|
| `ChatViewModel` | 注入 `ChatClientManager`，管理 UI 状态，委托聊天调用 |
| `ChatScreen` | Compose UI，通过 `LaunchedEffect` 刷新供应商状态 |

#### 依赖声明 (`feature/chat/impl/build.gradle.kts`)

```kotlin
dependencies {
    implementation(projects.feature.chat.api)
    implementation(projects.core.data)      // ← 只依赖 core:data
    implementation(libs.compottie)
    // ❌ 禁止：implementation(projects.jasmineCore.prompt.promptExecutor)
}
```

---

## 数据流

```
用户输入
  │
  ▼
ChatScreen (Compose UI)
  │  onSendClick()
  ▼
ChatViewModel (feature:chat:impl)
  │  clientManager.streamChat(SimpleChatMessage, model, onChunk)
  ▼
ChatClientManager (core:data)
  │  SimpleChatMessage → ChatMessage 转换
  │  ChatClientFactory.create(config)
  │  chatClient.chatStreamWithUsage(...)
  ▼
jasmine-core (prompt-executor / prompt-llm)
  │  HTTP 请求 → SSE 流式响应
  ▼
ChatClientManager
  │  StreamResult → StreamChatResult 转换
  ▼
ChatViewModel
  │  onChunk 回调 → 更新 _messages StateFlow
  ▼
ChatScreen (UI 实时更新)
```

---

## 供应商配置流程

```
设置页面 (ProviderConfigScreen)
  │
  ├── 配置 API Key / Base URL / 模型  → ChatProviderRepository.saveProviderConfig()
  │                                      (仅保存，不激活)
  │
  └── 打开供应商开关                    → ChatProviderRepository.setActiveProviderId()
                                          │
                                          ▼
                                    SharedPreferences 监听器
                                          │
                                          ▼
                                    configChangesFlow (StateFlow<Long>)
                                          │
                                          ▼
                                    ChatClientManager.refreshState()
                                          │
                                          ▼
                                    ChatViewModel 收到更新 → UI 刷新
```

> **注意**：`ChatScreen` 还会在每次进入 Composition 时主动调用 `refreshProviderState()`，
> 以应对 Navigation 生命周期导致的 Flow 信号丢失。

---

## 新增供应商

在 `ChatProviderRepository.PRESETS` 中添加一条：

```kotlin
ProviderPreset(
    id = "your_provider",           // 唯一标识
    name = "Your Provider",         // 显示名称
    defaultBaseUrl = "https://api.example.com",
    apiTypeString = "OPENAI",       // OPENAI / CLAUDE / GEMINI
    defaultModel = "model-name",
)
```

支持的 `ApiType`：
- `OPENAI` — OpenAI 兼容格式（DeepSeek、硅基流动等）
- `CLAUDE` — Anthropic Claude Messages API
- `GEMINI` — Google Gemini generateContent API
- `LOCAL` — 本地推理（需 app 层单独实现）

---

## jasmine-core 模块一览

```
jasmine-core/
├── prompt/
│   ├── prompt-model      ← 数据模型（ChatMessage, Usage 等）
│   ├── prompt-llm        ← ChatClient 接口与重试逻辑
│   ├── prompt-executor    ← 各供应商客户端实现（OpenAI, Claude, Gemini 等）
│   └── prompt-mnn        ← 本地 MNN 推理客户端
├── agent/                ← Agent 框架（Tools, Graph, MCP 等）
├── assistant/            ← 助手运行时
├── config/               ← 配置管理
├── conversation/         ← 对话存储
├── rag/                  ← RAG（检索增强生成）
└── termux/               ← Termux 终端环境
```

---

## 开发规范 ⚠️

1. **`feature` 层禁止 import `com.lhzkml.jasmine.core.prompt.*`**
2. 新增引擎功能时，先在 `core:data` 中封装，再暴露给 `feature`
3. `ChatClientManager` 是 `@Singleton`，由 Hilt 管理，勿手动实例化
4. 供应商配置使用 `SharedPreferences`（文件名 `jasmine_provider`），非 Proto DataStore
5. 配置变更通过 `configChangesFlow: StateFlow<Long>` 广播，使用时间戳确保去重可靠性
