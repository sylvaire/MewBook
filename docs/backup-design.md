# MewBook 备份系统设计文档

> 最后更新：2026-05-02 | Schema 版本：4

## 1. 概述

MewBook 备份系统负责数据的导出、导入、迁移和远程同步。核心设计原则：

- **单一交换格式**：所有备份路径（本地导出、DAV 同步、智能导入、CSV 导入）统一使用 `BackupEnvelope` 作为中间表示
- **事务安全**：全量恢复在单个 Room 事务中执行（delete all → insert all），保证原子性
- **安全回滚**：每次破坏性导入前自动创建本地安全备份，最多保留 3 份
- **密码保护**：导出时清除 DAV 密码，恢复时保留已有密码

## 2. 架构

```
┌─────────────────────────────────────────────────────┐
│                    UI 层                              │
│  ExportScreen / DavSettingsScreen / SmartImportScreen │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│              BackupRepository                         │
│  exportToJsonString / importFromUri / importRecords   │
│  previewRestore / previewImportRecords                │
│  createSafetyBackup / clearAllData                    │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│           BackupMigration                             │
│  parseToCurrentEnvelope / encodeEnvelope              │
│  migrateLegacyExportV1 / migrateLegacyDavV2           │
│  summarizeEnvelope / compareEnvelopes                 │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│         BackupImportPolicy                            │
│  parseExternalCsv / mergeRecordImport                 │
│  previewRecordImport / buildRecordImportPlan          │
│  CategorySemanticPolicy (语义分类匹配)                 │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│         Room Database (MewBookDatabase)               │
│  RecordDao / CategoryDao / AccountDao / ...           │
└─────────────────────────────────────────────────────┘
```

## 3. 数据模型

### 3.1 BackupEnvelope — 顶层信封

```kotlin
@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,        // 当前 = 4
    val appVersion: String,        // 应用版本号或导入来源标识
    val exportedAt: String,        // ISO_LOCAL_DATE_TIME
    val payload: BackupPayload
)
```

### 3.2 BackupPayload — 数据载荷

```kotlin
@Serializable
data class BackupPayload(
    val records: List<BackupRecord> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val accounts: List<BackupAccount> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val templates: List<BackupRecurringTemplate> = emptyList(),
    val ledgers: List<BackupLedger> = emptyList(),
    val davConfig: BackupDavConfig? = null,
    val themeMode: String? = null
)
```

### 3.3 实体模型

| 实体 | 关键字段 | 说明 |
|------|----------|------|
| `BackupRecord` | `id`, `amount`(正数), `type`(EXPENSE/INCOME), `categoryId`, `date`(epoch day), `ledgerId`, `accountId?` | 流水记录，`accountId` 可选 |
| `BackupCategory` | `id`, `name`, `icon`, `color`, `type`, `parentId?`, `semanticLabel?` | 分类，支持父子层级 |
| `BackupAccount` | `id`, `name`, `type`, `balance`, `icon`, `color`, `isDefault`, `ledgerId` | 账户，绑定到账本 |
| `BackupBudget` | `id`, `categoryId?`, `periodType`, `periodKey`, `amount`, `ledgerId` | 预算 |
| `BackupRecurringTemplate` | `id`, `name`, `amount`, `type`, `categoryId`, `scheduleType`, `startDate`, `nextDueDate` | 周期模板 |
| `BackupLedger` | `id`, `name`, `type`, `icon`, `color`, `isDefault` | 账本 |
| `BackupDavConfig` | `serverUrl`, `username`, `password`, `remotePath`, `isEnabled` | DAV 配置（导出时密码清空） |

### 3.4 预览与冲突类型

| 类型 | 用途 |
|------|------|
| `BackupSnapshotSummary` | 快照摘要：各实体数量、是否有 DAV 配置、主题模式 |
| `BackupConflictSummary` | ID 冲突计数 |
| `BackupRestorePreview` | 全量恢复预览：当前快照 + 传入快照 + 冲突 |
| `BackupRecordImportPreview` | 记录导入预览：重复数、待导入数、待创建分类/账户/账本、分类映射 |
| `BackupCategoryImportMapping` | 分类映射：源名 → 目标名，动作（复用/新建），原因 |

## 4. Schema 版本与迁移

### 4.1 版本历史

| 版本 | 格式特征 | 检测方式 |
|------|----------|----------|
| Legacy V1 | `version: "1.0"`（字符串），记录含 `categoryName`/`subCategoryName` | `versionElement?.content == "1.0"` |
| Legacy V2 | `version: 2`（整数），DAV 导出格式 | `versionElement?.intOrNull == 2` |
| V3-V4 | `schemaVersion` 字段存在 | `root["schemaVersion"]` |
| **当前 = 4** | 完整信封，含所有 payload 段 | — |

### 4.2 迁移策略

`BackupMigration.parseToCurrentEnvelope(jsonString)` 是统一入口：

1. 去除 BOM/空白，非 `{` 开头 → 委托 `BackupImportPolicy.parseExternalCsv()`（CSV 路径）
2. 解析 JSON，读取 `schemaVersion`：
   - 存在且 ≤ 4 → 直接反序列化 + `normalizeBudget` + 默认账本填充
   - 不存在 → 检查 legacy `version` 字段：
     - `"1.0"` → `migrateLegacyExportV1`：动态构建分类树，自增 ID，所有记录绑定 `ledgerId = 1`
     - `2` → `migrateLegacyDavV2`：逐字段映射，从记录中推断账本

### 4.3 预算规范化

`normalizeBudget` 确保：
- `periodType` 默认 `"MONTH"`
- `periodKey` 从 `periodKey ?: month ?: ""` 解析
- 当 `periodType == "MONTH"` 时回填 `month`

## 5. 导出流程

### 5.1 本地导出

```
buildCurrentEnvelope()
  ├─ recordDao.getAllRecordsOnce()     → toBackup()
  ├─ categoryDao.getAllCategoriesOnce() → toBackup()
  ├─ accountDao.getAllAccountsOnce()    → toBackup()
  ├─ budgetDao.getAllBudgetsOnce()      → toBackup()
  ├─ recurringTemplateDao.getAllTemplatesOnce() → toBackup()
  ├─ ledgerDao.getAllLedgersOnce()      → toBackup()
  ├─ davConfigDao.getDavConfigOnce()   → toBackup().copy(password = "")  ← 密码清空
  └─ themePreferencesRepository.getThemeModeOnce()
       │
       ▼
BackupMigration.encodeEnvelope() → JSON String
       │
       ▼
ContentResolver.openOutputStream(uri) → 写入文件
```

### 5.2 DAV 导出

手动导出：`ExportDataUseCase.invoke(config, fileName?)` → `DavRepository.exportData`
- 支持自定义文件名，留空使用 `mewbook_backup_yyyyMMdd_HHmmss.json`
- 手动导出文件不纳入自动清理范围

### 5.3 DAV 自动备份

触发：`MainActivity.onStart` → `DavAutoBackupCoordinator.runIfDue(now)`

```
shouldRun(config, lastAttemptDate, today)?
  ├─ config != null
  ├─ config.isEnabled == true
  ├─ config.isConfigured() == true  (serverUrl/username/password 非空)
  └─ lastAttemptDate != today  (每日一次)
       │
       ▼ 记录尝试
exportWithRetry(config)
  ├─ 第 1 次：ExportDataUseCase.autoBackup(config)
  └─ 失败后延迟 5 秒重试第 2 次
       │
       ▼ 成功后清理
pruneBackupFiles(config, keepLatestCount = 30)
  ├─ 筛选：mewbook_auto_backup_*.json
  ├─ 排序：字典序（时间戳保证时序）
  ├─ 删除超出 30 份的最旧文件
  └─ 60 秒超时保护
       │
       ▼ 记录结果
DavAutoBackupStatusRepository (DataStore)
  ├─ lastAttemptDate / lastAttemptTime
  ├─ lastSuccessTime
  └─ lastMessage / lastMessageIsError
```

关键约束：
- 只清理 `mewbook_auto_backup_` 前缀文件，不删手动导出
- 失败不弹窗、不 Snackbar，静默记录到 DAV 设置页
- 清理失败仅记录警告，不影响备份成功判定
- 使用 `Mutex` 防止同进程并发上传

## 6. 导入流程

### 6.1 全量恢复

```
importFromUri(uri) / importFromJsonString(jsonString)
  │
  ▼
createSafetyBackup()  →  {filesDir}/safety_backups/safety_backup_{yyyyMMdd_HHmmss}.json
  │  最多保留 3 份，失败不阻塞
  │
  ▼
BackupMigration.parseToCurrentEnvelope(jsonString)
  │
  ▼
restoreEnvelope(envelope)  ← Room.withTransaction {
  │  1. deleteAllRecords / deleteAllBudgets / deleteAllTemplates
  │  2. deleteAllAccounts / deleteAllCategories / deleteAllLedgers / deleteDavConfig
  │  3. insertLedgers → insertCategories → insertAccounts
  │  4. insertBudgets → insertTemplates → insertRecords
  │  5. DAV 密码合并：incoming.password.isBlank() && existing != null → 保留已有密码
  │  6. setThemeMode
  │ }
  └─ 事务完成
```

### 6.2 记录导入（合并模式）

```
importRecordsFromUri(uri) / importRecordsFromEnvelope(incoming)
  │
  ▼
createSafetyBackup()
  │
  ▼
buildRecordImportPlan(current, incoming)
  │
  ├─ 1. 账本映射：按规范化名称匹配，未匹配则新建
  │     ledgerIdMap[incomingId] → targetId
  │
  ├─ 2. 分类映射（CategorySemanticPolicy.resolveExistingCategory）
  │     四级瀑布：
  │     ① 同类型 + 同父级 + 精确名称匹配
  │     ② 同类型 + 精确名称匹配（忽略父级）
  │     ③ 同类型 + 同父级 + 语义标签匹配
  │     ④ 同类型 + 语义标签匹配（忽略父级）
  │     → REUSE_EXISTING 或 CREATE_NEW
  │
  ├─ 3. 账户映射：按 (targetLedgerId, normalizedName) 匹配
  │
  ├─ 4. 记录合并
  │     ├─ 映射 ID（账本/分类/账户）
  │     ├─ 生成 syncId（若缺失）
  │     ├─ 计算指纹：ledgerId|type|categoryId|accountId|date|normalizedNote|normalizedAmount
  │     ├─ 指纹已存在 → 跳过（duplicateRecords++）
  │     └─ 指纹不存在 → 追加 + 调整账户余额（收入加/支出减）
  │
  └─ 返回 RecordImportPlan(preview, mergedEnvelope)
       │
       ▼
restoreEnvelope(mergedEnvelope)  ← 同全量恢复
```

## 7. 安全机制

### 7.1 安全备份

- **时机**：每次全量恢复或记录导入前
- **位置**：`{context.filesDir}/safety_backups/`
- **命名**：`safety_backup_{yyyyMMdd_HHmmss}.json`
- **保留**：最多 3 份，按文件名降序排列，超出删除最旧
- **容错**：创建失败仅日志记录，不阻塞导入

### 7.2 DAV 密码保护

- **导出**：`davConfig.toBackup().copy(password = "")` — 密码字段清空
- **恢复**：若传入密码为空且本地已有配置 → 保留本地密码
- **实现位置**：`BackupRepository.restoreEnvelope()` 第 251 行，在事务内执行

### 7.3 DAV 连接安全

- `DavConfig.isInsecure()`：检测 `http://`（排除 localhost）
- DAV 设置页对 HTTP URL 显示安全警告
- `DavClient` 所有 `Log.d()` 调用用 `BuildConfig.DEBUG` 守卫

## 8. 文件命名规范

| 场景 | 文件名格式 | 示例 |
|------|-----------|------|
| 手动导出（默认） | `mewbook_backup_{yyyyMMdd_HHmmss}.json` | `mewbook_backup_20260502_143000.json` |
| 手动导出（带版本） | `mewbook_backup_v{schemaVersion}_{yyyyMMdd_HHmmss}.json` | `mewbook_backup_v4_20260502_143000.json` |
| 自动备份 | `mewbook_auto_backup_{yyyyMMdd_HHmmss}.json` | `mewbook_auto_backup_20260502_143000.json` |
| 安全备份 | `safety_backup_{yyyyMMdd_HHmmss}.json` | `safety_backup_20260502_143000.json` |

## 9. 关键设计决策

| 决策 | 理由 |
|------|------|
| 全量恢复用 Room 事务 | 保证 delete + insert 原子性，避免部分恢复 |
| 记录导入用指纹去重 | `ledgerId|type|categoryId|accountId|date|note|amount` 组合唯一标识一条记录 |
| 分类匹配用语义标签 | 中英文分类名差异大，精确匹配命中率低 |
| 导出清空 DAV 密码 | 防止密码泄露到备份文件 |
| 安全备份不阻塞导入 | 用户体验优先，备份失败不应阻止正常操作 |
| 自动备份每日一次 | 平衡频率与资源消耗，避免频繁网络请求 |
| 自动备份仅清理同前缀 | 保护手动导出文件不被误删 |

## 10. 配置常量

| 常量 | 值 | 位置 |
|------|-----|------|
| `CURRENT_SCHEMA_VERSION` | 4 | `BackupMigration.kt:16` |
| 安全备份保留数 | 3 | `BackupRepository.kt:168` |
| 自动备份保留数 | 30 | `DavAutoBackupCoordinator.kt` |
| 重试次数 | 1 | `DavAutoBackupCoordinator.kt` |
| 重试延迟 | 5000ms | `DavAutoBackupCoordinator.kt` |
| 清理超时 | 60000ms | `DavAutoBackupCoordinator.kt` |
