# MewBook（喵喵记账）

一个基于 **Jetpack Compose + Material 3** 的 Android 记账应用，支持多账本、分类管理、预算管理、统计分析、资产账户、周期模板、数据迁移、智能导入、WebDAV 同步与应用内更新。

## 功能概览

- 首页记账：新增、编辑、删除记录，支持快捷记账、流水详情、内联搜索与日/周/月周期切换
- 分类管理：收支分类维护、语义匹配、图标展示与排序
- 资产账户：账户新增、编辑、余额联动与账本关联
- 账本管理：默认账本、排序与多账本数据隔离
- 预算管理：总预算、分类预算、不同预算周期与余额提醒
- 周期模板：工资、房租、订阅等固定收入/支出模板
- 统计报表：周/月/年维度趋势、分类占比与支出分类明细下钻（支持点击查看、编辑和删除记录）
- 数据迁移：本地备份/还原、CSV/JSON 导出、外部导入预览与智能导入
- WebDAV 同步：远端备份、手动导出自定义文件名（自动加 `manual_` 前缀）、手动选择远端备份恢复、连接探测、同步预览、导入前自动安全备份与每日首次打开自动备份（失败自动重试）
- 应用更新：检查 GitHub Release、下载 APK、引导系统安装器，支持跳过此版本和关闭更新功能
- 主题模式：浅色 / 深色 / 跟随系统

## 技术栈

- Kotlin + Coroutines + Flow
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt（依赖注入）
- Room（本地数据库）
- DataStore（偏好设置）
- OkHttp（WebDAV / 智能导入 / 应用更新）
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
├─ data      # 数据层：Room / Repository / Preferences / Remote / Smart Import / Update
├─ di        # Hilt 模块
├─ domain    # 领域层：model / policy / repository / usecase
├─ ui        # 界面层：screens / components / navigation / theme
└─ util      # 工具与扩展
```

## 关键页面

- `Home`：记账首页、新增/编辑记账覆盖层、记录详情、快捷记账、内联搜索
- `Statistics`：统计图表、时间周期切换、支出构成与分类明细下钻
- `Asset`：资产账户列表、账户编辑、新增账户
- `Settings`：主题、首页偏好、账本、分类、预算、周期模板、迁移备份、DAV、更新检查、清除数据入口
- `Export` / `SmartImport`：备份还原、格式导出、外部数据导入与智能导入
- `RecurringTemplates`：固定记账模板管理

## v1.0.7 更新内容

- 新增 WebDAV 每日自动备份，首次打开应用时自动执行，失败自动重试一次
- 支出构成分类明细页重新设计为 Claymorphism 卡片风格，支持点击查看、编辑和删除记录
- 记账详情弹窗重新设计为 Claymorphism 风格，视觉层次更清晰
- 修复记账编辑页状态栏颜色与顶部栏不一致的问题
- WebDAV 备份导出去除密码明文，防止凭据泄露到备份文件
- 导入恢复前自动创建本地安全备份，最多保留 3 份
- 自定义备份文件名自动添加 `manual_` 前缀区分来源
- 备份文件过滤改为前缀匹配，避免误识别非备份 JSON
- 连接探测改用 `XmlPullParser` 命名空间感知解析
- 应用更新支持"跳过此版本"和"关闭更新功能"，设置页可重新开启
- DAV 同步设置页服务器配置改为弹出窗口
- 自动备份清理旧文件操作增加 60 秒超时保护
- 首页、统计、资产、预算、设置等多个页面 UI 优化

## 2026-04 已完成更新

- 新增统计页支出构成分类明细下钻
- 新增周期模板、首页快捷入口、还原预览与多周期预算能力
- 新增智能导入，支持文本、CSV、JSON 与 OpenAI 兼容接口转换
- 新增 GitHub Release 应用内更新检查、APK 下载与安装引导
- 新增设置页“清除数据”二次确认流程
- 将“分支”统一改名为“账本”，并补齐账本排序与默认账本行为
- 优化 Clay 风格主题、顶部栏、底部导航、FAB 遮挡、首页周期与搜索交互

## Gradle 与版本信息

- `compileSdk = 34`
- `targetSdk = 34`
- `minSdk = 26`
- `versionName = 1.0.7`
- `versionCode = 8`
- `AGP = 8.2.2`
- `Kotlin = 1.9.22`

## License

本项目采用 **MIT License**，详情见 [LICENSE](./LICENSE)。
