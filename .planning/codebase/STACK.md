# 技术栈（STACK）

## 概览

MewBook 为单模块 Android 应用（`app`），使用 Kotlin、Jetpack Compose 与 Material 3 构建的个人记账类客户端。

## 语言与运行时

| 项目 | 版本/说明 |
|------|-----------|
| Kotlin | 1.9.22 |
| JVM 目标 | 17 |
| Android Gradle Plugin | 8.2.2 |
| minSdk / targetSdk | 26 / 34 |
| 应用版本 | versionCode 3, versionName 1.0.2 |

## UI

- **Jetpack Compose**（BOM `2024.02.00`，compiler extension `1.5.8`）
- **Material 3** + Material Icons Extended
- **Navigation Compose**（`2.7.7`）
- **Vico**（`1.13.1`：compose-m3 / compose / core）用于统计图表

## 架构与依赖注入

- **Hilt**（`2.50`）+ KSP 生成代码
- **ViewModel** + **Lifecycle**（含 `lifecycle-runtime-compose`、`lifecycle-viewmodel-compose`）

## 数据与存储

- **Room**（`2.6.1`）：本地 SQLite，实体含流水、分类、账本、资产、预算、WebDAV 配置等
- **DataStore Preferences**：主题等偏好
- **kotlinx-serialization-json**（`1.6.2`）：JSON 序列化（备份等场景）

## 网络与安全

- **OkHttp**（`4.12.0`）：WebDAV 同步/探测
- **androidx.security:security-crypto**：凭据相关安全能力（与配置配合使用）

## 异步

- **Kotlin Coroutines**（core + android）

## 构建与签名

- 根 `build.gradle.kts` 仅声明插件版本；`app/build.gradle.kts` 中通过可选的 `keystore.properties` 配置 release 签名。
- Release 构建当前 **未启用** `minify`（`isMinifyEnabled = false`）。

## 非代码资产

- 标准 Android 资源：`mipmap` 启动图标、`xml/file_paths` 配合 `FileProvider` 用于文件分享。
