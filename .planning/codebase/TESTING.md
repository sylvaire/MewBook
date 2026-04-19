# 测试（TESTING）

## 单元测试（`app/src/test`）

当前存在的 JVM 单元测试包括（非穷尽，以源码树为准）：

| 区域 | 文件示例 | 关注点 |
|------|----------|--------|
| WebDAV | `DavClientTest.kt` | 远程客户端行为 |
| 仓库 | `DavRepositoryImplTest.kt` | DAV 仓库实现 |
| 备份 | `BackupMigrationTest.kt` | 备份迁移正确性 |
| UI 逻辑 | `AmountExpressionHelperTest.kt` | 金额表达式辅助纯逻辑 |

## 仪器测试（`androidTest`）

- `app/build.gradle.kts` 已声明 `androidTestImplementation`（JUnit4、Espresso、Compose UI Test 等）。
- **`app/src/androidTest` 下当前无 `*.kt` 测试文件**，即尚未落地仪器测试或已移除。

## 测试运行器

- `defaultConfig.testInstrumentationRunner` 为 `androidx.test.runner.AndroidJUnitRunner`。

## 建议（基于现状）

- 对 Room DAO 可增加 **in-memory** 或 **Robolectric** 级测试（若引入）验证查询。
- 导航与关键用户流可考虑补 **Compose UI Test**（`androidTest`）。
- WebDAV 相关测试已部分覆盖；备份迁移测试应与 **数据库版本 bump** 同步维护。
