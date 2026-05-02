# 测试（TESTING）

## 单元测试（`app/src/test`）

当前存在的 JVM 单元测试包括（非穷尽，以源码树为准）：

| 区域 | 文件示例 | 关注点 |
|------|----------|--------|
| WebDAV | `DavClientTest.kt`、`DavRepositoryImplTest.kt` | 远程客户端、DAV 备份列表、手动选择导入、手动导出文件名、自动备份保留策略与 DAV 仓库行为 |
| 备份与导入 | `BackupMigrationTest.kt`、`BackupImportPolicyTest.kt` | 备份迁移、外部 CSV 导入解析 |
| 智能导入 | `SmartImportApiPolicyTest.kt`、`SmartImportPolicyTest.kt`、`SmartImportRepositoryTest.kt` | OpenAI 兼容接口约束、AI 响应解析、CSV 本地解析与 fallback |
| 领域 policy | `AccountDefaultsPolicyTest.kt`、`BudgetCategoryBudgetPolicyTest.kt`、`HomeRecordSearchPolicyTest.kt`、`DavAutoBackupPolicyTest.kt`、`DavAutoBackupCoordinatorTest.kt`、`RecurringTemplateSchedulePolicyTest.kt` 等 | 账户默认值、预算、首页搜索、DAV 自动备份、周期模板调度等纯逻辑 |
| UI 逻辑 | `AmountExpressionHelperTest.kt`、`RecordDetailTimeFormatterTest.kt`、`RecurringTemplateUsageGuideTest.kt` | 金额表达式、记录详情时间、周期模板说明 |
| 统计与日期 | `StatisticsSummaryCalculatorTest.kt`、`PeriodDateRangeTest.kt` | 统计汇总与周期日期范围 |

## 仪器测试（`androidTest`）

- `app/build.gradle.kts` 已声明 `androidTestImplementation`（JUnit4、Espresso、Compose UI Test 等）。
- **`app/src/androidTest` 下当前无 `*.kt` 测试文件**，即尚未落地仪器测试或已移除。

## 测试运行器

- `defaultConfig.testInstrumentationRunner` 为 `androidx.test.runner.AndroidJUnitRunner`。

## 常用验证命令

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

## 建议（基于现状）

- 对 Room DAO 可增加 **in-memory** 或 **Robolectric** 级测试（若引入）验证查询。
- 导航与关键用户流可考虑补 **Compose UI Test**（`androidTest`）。
- WebDAV、备份、智能导入、DAV 自动备份与多个 domain policy 已有 JVM 测试；备份迁移测试应与 **数据库版本 bump** 同步维护。
