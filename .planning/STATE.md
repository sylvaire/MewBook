# GSD 状态 — 2026-05-02

## 当前里程碑

UI/UX 优化与账户体系完善

## 已完成（2026-05-02 会话）

- [x] 分类管理移除左滑删除（`CategoriesScreen.kt` 清除 `AnchoredDraggableState` 相关代码）
- [x] DAV 密码丢失修复（`BackupRepository.restoreEnvelope()` 在事务外读取现有 DAV 配置）
- [x] `AccountEditScreen` 重设计为 Claymorphism 风格（hero card + 编辑卡片 + 变更检测）
- [x] `AddAccountScreen` 升级为 Clay 卡片风格
- [x] `AccountEditScreen` 合并 `updateName()`+`updateBalance()` 为单一 `saveChanges()`
- [x] `AssetScreen` 空态添加”立即添加”引导按钮
- [x] 新建账本自动创建默认”现金”账户（`LedgerManagementViewModel.addLedger()`）
- [x] 新建 `EnsureDefaultAccountForLedgerUseCase`：按账本检查，无账户时自动创建”现金”
- [x] `HomeViewModel` 初始化时调用 `EnsureDefaultAccountForLedgerUseCase`，确保首页加载时当前账本已有默认账户
- [x] `AssetViewModel` 改用 `EnsureDefaultAccountForLedgerUseCase`（替换原全局版）
- [x] 删除废弃的 `InitializeDefaultAccountsUseCase` 和 `DefaultAccounts` 对象

## 已完成历史里程碑

- [x] 网盘自动备份（随 1.0.7 发布）
- [x] UI 修复、账本命名统一、清除数据功能（随 1.0.7 发布）
- [x] 统计页支出构成分类下钻（随 1.0.3 发布）
- [x] 周期模板、首页快捷入口、还原预览与多周期预算（随 1.0.4 发布）
- [x] 智能导入、应用内更新、统计体验改进（随 1.0.5 发布）

## 代码审查

2026-05-01 完成标准代码审查（37 个源文件），修复 2 个 Critical、5 个 Warning、3 个 Info 问题。详见 `.planning/REVIEW.md`。

## 工作区提醒

2026-05-02 的工作区包含未提交改动，主要覆盖：

- 账户体系：`EnsureDefaultAccountForLedgerUseCase`（新建）、`HomeViewModel`/`AssetViewModel`/`LedgerManagementViewModel` 调用链、删除 `DefaultAccounts` 与 `InitializeDefaultAccountsUseCase`；
- 账户 UI：`AccountEditScreen`（Clay hero card + 变更检测）、`AddAccountScreen`（Clay 卡片）、`AssetScreen`（空态”立即添加”按钮）；
- 分类管理：`CategoriesScreen` 移除左滑删除；
- DAV 密码修复：`BackupRepository.restoreEnvelope()` 事务外读取 DAV 配置。

这些改动应视为”待验证本地工作”，发版或交接前需要运行标准验证并阅读 `git diff`。

## 阻塞项

无已知硬阻塞。

## 备注

- 每个账本现在至少有一个默认”现金”账户：新建账本时自动创建，已有账本在 HomeViewModel 初始化时补建。
- `EnsureDefaultAccountForLedgerUseCase` 按 `ledgerId` 检查（非全局），解决了旧版 `InitializeDefaultAccountsUseCase` 只为 `ledgerId=1` 创建默认账户的问题。
- 自动备份失败静默记录在 DAV 设置页，不弹窗、不 Snackbar。
- 自动备份只上传本地完整备份，不做自动恢复或双向同步。
- 统计页下钻已落地为 `CategoryExpenseDetailScreen` / `CategoryExpenseDetailViewModel` 与 `Screen.CategoryExpenseDetail` 路由。2026-05-01 重新设计为 Claymorphism 卡片风格，并新增记录详情查看、编辑和删除功能。
- `DavClient` 和 `DavSettingsViewModel` 的所有 `Log.d()` 调用已用 `BuildConfig.DEBUG` 守卫。
- `DavConfig` 新增 `isInsecure()` 方法，DAV 设置页对 HTTP URL 显示安全警告。
