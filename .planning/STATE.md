# GSD 状态 — 2026-05-26

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-05-26 — Milestone v1.0.11 started

## 当前里程碑

v1.0.11: UX 打磨与交互优化

## 已完成（2026-05-24 会话）

- [x] 旧二级分类设计清理为单层分类模型，`CategoryEntity` / `Category` / `BackupCategory` 不再保存父子层级字段
- [x] 删除记录移入本地 `deleted_records` 回收站，30 天内可恢复或永久删除
- [x] 设置页新增回收站入口，`RecycleBinScreen` / `RecycleBinViewModel` 展示删除记录、剩余天数、恢复与永久删除操作
- [x] 首页与统计下钻删除流程改为可找回文案和“已移入回收站”提示
- [x] 记账页支出分类显示恢复旧版常用主类，底层分类仍保持单层

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

2026-05-24 的工作区包含未提交改动，主要覆盖：

- 分类扁平化：分类实体、领域模型、默认分类、导入/备份映射、预算/周期模板/统计/记账选择相关调用链；
- 回收站：`deleted_records` Room 表、DAO、Repository、UseCases、设置页入口、回收站 UI、删除/恢复账户余额联动；
- UI 调整：删除后提示条样式优化、回收站提示圆角与边框修正、记账页支出分类显示恢复旧版常用主类。

这些改动应视为”待验证本地工作”，发版或交接前需要运行标准验证并阅读 `git diff`。

## 阻塞项

无已知硬阻塞。

## 备注

- 每个账本现在至少有一个默认”现金”账户：新建账本时自动创建，已有账本在 HomeViewModel 初始化时补建。
- 回收站记录只本地保留 30 天，不参与普通备份导出；完整恢复和清除数据会清空 `deleted_records`。
- `EnsureDefaultAccountForLedgerUseCase` 按 `ledgerId` 检查（非全局），解决了旧版 `InitializeDefaultAccountsUseCase` 只为 `ledgerId=1` 创建默认账户的问题。
- 自动备份失败静默记录在 DAV 设置页，不弹窗、不 Snackbar。
- 自动备份只上传本地完整备份，不做自动恢复或双向同步。
- 统计页下钻已落地为 `CategoryExpenseDetailScreen` / `CategoryExpenseDetailViewModel` 与 `Screen.CategoryExpenseDetail` 路由。2026-05-01 重新设计为 Claymorphism 卡片风格，并新增记录详情查看、编辑和删除功能。
- `DavClient` 和 `DavSettingsViewModel` 的所有 `Log.d()` 调用已用 `BuildConfig.DEBUG` 守卫。
- `DavConfig` 新增 `isInsecure()` 方法，DAV 设置页对 HTTP URL 显示安全警告。
