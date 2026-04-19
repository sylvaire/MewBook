# 外部集成（INTEGRATIONS）

## 网络：WebDAV

- **实现位置**：`data.remote.DavClient`（OkHttp），通过 `DavRemoteDataSource` 抽象注入。
- **用途**：连接探测、与远程存储同步/上传下载相关逻辑（与 `DavRepositoryImpl`、`domain.usecase.dav` 及 `DavSettingsScreen` 联动）。
- **权限**：`AndroidManifest.xml` 声明 `INTERNET`。

## 本地持久化

- **Room**：主数据库 `MewBookDatabase`（`mewbook.db`），版本 2，含多 DAO。
- **DataStore**：`ThemePreferencesRepository` 等，用于主题模式等用户偏好。

## 文件与系统

- **FileProvider**（`androidx.core.content.FileProvider`）：`authorities` 为 `${applicationId}.fileprovider`，用于安全地共享导出/备份等文件路径（见 `res/xml/file_paths`）。

## 数据交换与备份

- **备份/迁移**：`data.backup` 包（如 `BackupRepository`、`BackupMigration`、`BackupModels`），配合序列化模型做快照与版本迁移。
- **导出**：`ExportRepository`、`ExportScreen` / `ExportViewModel` 负责对外导出流程。

## 第三方 UI

- **Vico**：统计/报表界面中的图表组件（`StatisticsScreen` 等）。

## 未在代码中体现的集成

- 无 Firebase、无自有后端 REST SDK；同步主要面向用户配置的 WebDAV 端点。
- `androidTest` 目录当前无测试文件；仪器测试依赖已声明但未使用。
