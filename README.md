# Jasmine

一个基于现代 Android 架构和最佳实践的生产级 Android 应用程序。

## 功能特性
- **现代 UI**: 使用 Jetpack Compose 构建。
- **架构**: 遵循官方 Android 架构指南。
- **依赖注入**: 使用 Hilt 进行依赖注入。
- **数据层**: 使用 Room 进行本地存储，Retrofit 进行网络请求。
- **导航**: 使用 Navigation 3 实现类型安全导航。

## 技术栈
- **UI 框架**: Jetpack Compose 2025.09.01
- **架构模式**: MVVM + Clean Architecture
- **依赖注入**: Hilt 2.59
- **数据层**: Room 2.8.3, Retrofit 2.11.0
- **导航**: Navigation 3 1.0.0
- **编程语言**: Kotlin 2.3.0
- **构建系统**: Gradle with Kotlin DSL

## 项目结构
```
app/                    # 主应用模块
app--catalog/        # 目录应用模块
core/                   # 核心功能模块
  ├── analytics/        # 分析功能
  ├── common/           # 通用工具
  ├── data/             # 数据层
  ├── database/         # 数据库
  ├── datastore/        # 数据存储
  ├── designsystem/     # 设计系统
  ├── domain/           # 领域层
  ├── model/            # 数据模型
  ├── navigation/       # 导航
  ├── network/          # 网络层
  ├── notifications/    # 通知
  └── ui/               # UI 组件
feature/                # 功能模块
  ├── bookmarks/        # 书签功能
  ├── foryou/           # 为你推荐
  ├── interests/        # 兴趣功能
  ├── search/           # 搜索功能
  ├── settings/         # 设置功能
  └── topic/            # 主题功能
build-logic/            # 构建逻辑
```

## 开始使用
- 在 Android Studio (Ladybug 或更高版本) 中打开项目。
- 运行 `debug` 或 `release` 构建。

## 构建命令
```bash
# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease

# 运行测试
./gradlew test

# 运行 UI 测试
./gradlew connectedAndroidTest
```

## 开发环境要求
- **Android Studio**: Ladybug 或更高版本
- **JDK**: 17 或更高版本
- **Gradle**: 8.0 或更高版本
- **Kotlin**: 2.3.0 或更高版本
