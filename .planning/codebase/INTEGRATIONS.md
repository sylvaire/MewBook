# 外部集成（INTEGRATIONS）

## 网络：WebDAV

- **实现位置**：`data.remote.DavClient`（OkHttp），通过 `DavRemoteDataSource` 抽象注入。
- **用途**：连接探测、与远程存储同步/上传下载/删除相关逻辑（与 `DavRepositoryImpl`、`domain.usecase.dav` 及 `DavSettingsScreen` 联动）。
- **权限**：`AndroidManifest.xml` 声明 `INTERNET`。
- **导出命名**：三种文件名前缀区分来源：
  - 手动导出（默认）：`mewbook_backup_yyyyMMdd_HHmmss.json`
  - 手动导出（自定义名）：`manual_{用户输入}.json`，自动添加前缀以区分
  - 自动备份：`mewbook_auto_backup_yyyyMMdd_HHmmss.json`
- **备份文件过滤**：`listBackupFiles` 仅识别 `mewbook_backup_`、`mewbook_auto_backup_`、`manual_` 前缀的 JSON 文件。
- **导入选择**：DAV 导入先通过 `listBackupFiles` 列出远端 JSON 备份候选，用户选择后预览并恢复同一文件。
- **导入安全**：`restoreEnvelope` 执行前自动创建本地安全备份（`context.filesDir/safety_backups/`），最多保留 3 份，确保恢复失败时可回溯。
- **导出安全**：备份导出时 `davConfig.password` 始终为空字符串，防止明文密码泄露到备份文件。
- **自动备份**：`DavAutoBackupCoordinator` 在 App 进入前台时执行每日一次备份；失败后延迟 5 秒重试一次；成功后 `pruneBackupFiles` 只保留最新 30 个 `mewbook_auto_backup_*.json`，不删除手动导出文件；清理操作有 60 秒超时保护（`withTimeoutOrNull`），超时不影响备份结果。
- **连接探测**：使用 `PROPFIND Depth: 0`（非 `OPTIONS`），XML 解析使用 `XmlPullParser` 命名空间感知解析。
- **凭据校验**：`DavConfig.isConfigured()` 要求 `serverUrl`、`username`、`password` 均非空才视为已配置，防止空密码触发自动备份。
- **配置界面**：服务器配置（URL、用户名、密码、远程路径）通过 `DavConfigDialog` 弹窗编辑，主页面仅显示配置摘要卡片、自动备份开关与同步操作按钮。

## 网络：智能导入

- **实现位置**：`data.smartimport.SmartImportRepository`、`SmartImportConfigRepository`、`SmartImportApiPolicy`。
- **用途**：将用户输入的文本、TXT、CSV、JSON 或上传文件转换为备份导入 envelope；CSV 优先本地解析，其他内容走 OpenAI 兼容接口。
- **配置**：默认 base URL 为 `https://api.openai.com/v1`，默认模型为 `gpt-4o-mini`；API Key 与配置通过 encrypted preferences 保存（可用时）。

## 网络：应用更新

- **实现位置**：`data.update.AppUpdateRepository`、`ui.update.AppUpdateViewModel`、`domain.policy.AppUpdatePolicy`、`data.preferences.AppUpdatePreferencesRepository`。
- **用途**：查询 `https://api.github.com/repos/sylvaire/MewBook/releases/latest`，选择可安装 APK asset，下载到应用外部 files 下载目录，并交给系统安装器。
- **偏好设置**：`AppUpdatePreferencesRepository`（DataStore）存储 `snoozedVersionName`（跳过的版本号）和 `updateEnabled`（更新开关，默认 true）。静默检查时自动跳过已跳过版本；设置页可重新开启更新并清除跳过记录。
- **权限**：`AndroidManifest.xml` 声明 `REQUEST_INSTALL_PACKAGES`，用于 Android 8.0+ 安装未知来源 APK 的授权流程。

## 本地持久化

- **Room**：主数据库 `MewBookDatabase`（`mewbook.db`），版本 4，含流水、分类、账本、账户、预算、WebDAV 配置与周期模板等 DAO。
- **DataStore**：`ThemePreferencesRepository`、`HomePreferencesRepository`、`DavAutoBackupPreferencesRepository`、`AppUpdatePreferencesRepository` 等，用于主题模式、首页周期、首页概览卡片、DAV 自动备份状态与应用更新偏好。

## 文件与系统

- **FileProvider**（`androidx.core.content.FileProvider`）：`authorities` 为 `${applicationId}.fileprovider`，用于安全地共享导出/备份等文件路径（见 `res/xml/file_paths`）。

## 数据交换与备份

- **备份/迁移**：`data.backup` 包（如 `BackupRepository`、`BackupMigration`、`BackupModels`），配合序列化模型做快照与版本迁移。
- **导出与导入**：`ExportRepository`、`ExportScreen` / `ExportViewModel` 负责备份还原、格式导出、外部导入预览与进入智能导入。

## 发布与分发

- **GitHub Actions**：`.github/workflows/release.yml` 监听 `v*.*.*` tag，准备签名配置（可选）、运行 `:app:assembleRelease`、重命名 APK 并发布 GitHub Release。
- **签名 secrets**：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`；缺失时 workflow 会继续发布 unsigned release APK。

## 第三方 UI

- **Vico**：统计/报表界面中的图表组件（`StatisticsScreen` 等）。

## 未在代码中体现的集成

- 无 Firebase、无自有后端 REST SDK；同步主要面向用户配置的 WebDAV 端点。
- `androidTest` 目录当前无测试文件；仪器测试依赖已声明但未使用。
