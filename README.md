# MewBook（喵喵记账）

一个基于 **Jetpack Compose + Material 3** 的 Android 记账应用，支持多账本、分类管理、预算管理、统计分析、资产账户与 WebDAV 同步。

## 功能概览

- 首页记账：新增/编辑/删除记录，按月查看收支
- 分类管理：收支分类维护与排序
- 资产账户：账户新增、编辑、余额联动
- 预算管理：按月预算与余额提醒
- 统计报表：周/月/年维度趋势与分类占比
- 数据导出：CSV/JSON 导出
- WebDAV 同步：远端备份与恢复
- 主题模式：浅色 / 深色 / 跟随系统

## 技术栈

- Kotlin + Coroutines + Flow
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt（依赖注入）
- Room（本地数据库）
- DataStore（偏好设置）
- OkHttp（WebDAV）
- Kotlin Serialization（JSON）

## 环境要求

- Android Studio（建议 Iguana / Hedgehog 或更高）
- JDK 17
- Android SDK 34
- 最低系统版本：Android 8.0（API 26）

## 快速开始

```bash
# 1) 克隆项目
git clone <your-repo-url>
cd MewBook

# 2) 编译 Debug 包
./gradlew :app:assembleDebug

# 3) 单元测试
./gradlew testDebugUnitTest

# 4) Lint 检查
./gradlew :app:lintDebug
```

Windows 下可使用：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## 项目结构

```text
app/src/main/java/com/mewbook/app
├─ data      # 数据层：Room / Repository / Preferences / Remote
├─ di        # Hilt 模块
├─ domain    # 领域层：model / repository / usecase
├─ ui        # 界面层：screens / components / navigation / theme
└─ util      # 工具与扩展
```

## 关键页面

- `Home`：记账首页 + 新增记账覆盖层
- `Statistics`：统计图表与时间周期切换
- `Asset`：资产账户与编辑
- `Settings`：主题、分类、预算、导出、DAV、账本管理入口

## 最近更新（2026-04）

- 修复新增记账页与键盘区域沉浸式问题
- 修复新增记账日期英文显示问题（改为中文 Locale）
- 修复设置页版本卡片在小屏机型显示不全问题
- 优化统计图布局，使坐标与折线更居中对称
- 新增统计周/月/年历史周期前后切换

## Gradle 与版本信息

- `compileSdk = 34`
- `targetSdk = 34`
- `minSdk = 26`
- `versionName = 1.0.0`
- `AGP = 8.2.2`
- `Kotlin = 1.9.22`

## License

本项目采用 **MIT License**，详情见 [LICENSE](./LICENSE)。
