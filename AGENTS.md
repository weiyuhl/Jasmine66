# Jasmine 项目

Jasmine 是一个使用 Kotlin 编写的原生 Android 移动应用程序。它提供关于 Android 开发的定期新闻。用户可以选择关注主题，在有新内容可用时收到通知，并收藏项目。

## 架构

本项目是一个现代 Android 应用程序，遵循 Google 的官方架构指南。它是一个响应式的单 Activity 应用，使用以下技术：

-   **UI**: 完全使用 Jetpack Compose 构建，包括 Material 3 组件和针对不同屏幕尺寸的自适应布局。
-   **状态管理**: 使用 Kotlin 协程和 `Flow` 实现单向数据流 (UDF)。`ViewModel` 作为状态持有者，将 UI 状态作为数据流暴露。
-   **依赖注入**: 在整个应用中使用 Hilt 进行依赖注入，简化依赖管理并提高可测试性。
-   **导航**: 使用 Jetpack Navigation 3 for Compose 处理导航，提供声明式和类型安全的屏幕间导航方式。
-   **数据**: 数据层使用 Repository 模式实现。
    -   **本地数据**: 使用 Room 和 DataStore 进行本地数据持久化。
    -   **远程数据**: 使用 Retrofit 和 OkHttp 从网络获取数据。
-   **后台处理**: 使用 WorkManager 处理可延迟的后台任务。

## 模块结构

主 Android 应用位于 `app/` 文件夹中。功能模块位于 `feature/` 中，核心和共享模块位于 `core/` 中。

### 核心模块 (core/)
- **analytics**: 分析功能，跟踪用户行为
- **common**: 通用工具和扩展函数
- **data**: 数据层实现，包含 Repository 模式
- **database**: Room 数据库实现
- **datastore**: DataStore 实现
- **designsystem**: 设计系统，包含主题、颜色、字体等
- **domain**: 领域层，包含 UseCase
- **model**: 数据模型定义
- **navigation**: 导航相关组件
- **network**: 网络层实现
- **notifications**: 通知功能
- **ui**: UI 组件和工具

### 功能模块 (feature/)
- **bookmarks**: 书签功能
- **foryou**: "为你推荐"功能
- **interests**: 兴趣功能
- **search**: 搜索功能
- **settings**: 设置功能
- **topic**: 主题功能

## 构建和测试命令

应用和 Android 库有两种产品风味：`demo` 和 `prod`，以及两种构建类型：`debug` 和 `release`。

- 构建: `./gradlew assemble{Variant}`。通常为 `assembleDemoDebug`。
- 修复代码格式: `./gradlew spotlessApply`
- 运行本地测试: `./gradlew {variant}Test`
- 运行单个测试: `./gradlew {variant}Test --tests "com.lhzkml.jasmine.MyTestClass"`

### 测试

#### 仪器化测试

- UI 功能测试应仅使用带有 `ComponentActivity` 的 `ComposeTestRule`。
- 更大的测试位于 `:app` 模块中，它们可以启动像 `MainActivity` 这样的活动。

#### 本地测试

- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) 用于大多数断言
- [cashapp/turbine](https://github.com/cashapp/turbine) 用于复杂的协程测试
- [google/truth](https://github.com/google/truth) 用于断言

## 持续集成

- 工作流定义在 `.github/workflows/*.yaml` 中，包含各种检查。

## 版本控制和代码位置

- 项目使用 git 进行版本控制。
