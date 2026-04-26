# Jasmine

AI 驱动的 Android 原生应用 —— 多模型 LLM 聊天、Agent 框架、Linux 沙盒、技能系统。

## 功能

- **多模型聊天** — OpenAI / Claude / Gemini / DeepSeek / 硅基流动 / OpenRouter / 本地 MNN 推理，支持流式响应、思考过程、工具调用
- **Agent 框架** — 图策略引擎、GOAP/LLM 规划器、子 Agent、MCP 协议 (HTTP + SSE)、20+ 内置工具
- **动态 UI** — LLM 可在聊天中生成交互式 Compose UI：表单、按钮、选择器、表格、卡片、倒计时等 30 种组件
- **Linux 沙盒** — 通过 PRoot (ptrace) 在无 Root 设备上运行 Alpine Linux，支持终端、包管理 (apk)、文件系统访问
- **联网搜索** — DuckDuckGo Instant Answer API，支持时间筛选和地区过滤
- **技能系统** — 8 个内置技能 (JS/Native)，支持导入自定义技能、API 密钥管理，选中技能指令自动注入 LLM 上下文
- **对话持久化** — Room 数据库存储对话历史与 token 用量

## 技术栈

| 领域 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 + Adaptive Layout |
| 架构 | MVVM + Clean Architecture + 单向数据流 |
| DI | Hilt (Dagger) |
| 导航 | Navigation 3 (Compose, 类型安全) |
| 存储 | Room 2.8 + Proto DataStore |
| 网络 | Retrofit 2.11 + OkHttp 4.12 + Ktor 3.4 |
| 构建 | Kotlin 2.3 + AGP 9.0 + Gradle Kotlin DSL |
| 测试 | JUnit + Turbine + Truth + Mockito |

## 模块

```
app/                    # 主应用
jasmine-core/           # AI 引擎 (18 个模块)
core/                   # 核心基础设施 (13 个模块)
feature/                # 功能模块 (chat/search/settings/sandbox/skills/tools/knowledgebase)
linux-sandbox/          # Linux 沙盒
websearch/              # DuckDuckGo 搜索
build-logic/            # 自定义 Gradle 约定插件
skills/                 # 技能定义 (SKILL.md)
```

## 快速开始

**环境**：Android Studio Ladybug+ · JDK 17+ · Android SDK 36

```bash
# Debug
./gradlew :app:assembleDebug

# Release（需配置签名）
./gradlew :app:assembleRelease

# 格式化
./gradlew spotlessApply
```

## 签名

Release 签名配置在 `keystore.properties`：

```properties
storeFile=jasmine-release.jks
storePassword=xxx
keyAlias=jasmine-release
keyPassword=xxx
```

## 开发

架构遵循 **feature API/Impl 分离**：各功能模块拆分为 `api`（NavKey + 公共接口）和 `impl`（Screen + ViewModel），feature 层通过 `core:data` 访问 `jasmine-core`，禁止直接依赖。

详细架构说明见 [AGENTS.md](AGENTS.md)。
