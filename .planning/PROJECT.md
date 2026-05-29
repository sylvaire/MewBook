# MewBook — 项目状态

## 产品

MewBook：Android 本地记账应用（Kotlin、Jetpack Compose、Room、Hilt）。核心能力包括流水、单层分类、账本、资产、预算、周期模板、统计下钻、删除找回、WebDAV 同步、自动备份、备份导出、智能导入与应用内更新。

## 当前里程碑：无活动里程碑

最近完成并归档：v1.1.0 发布打磨与交互优化（归档见 `.planning/archive/milestones/v1.1.0.md`）。

**Goal:** 提升记账、分类管理和设置等核心操作的交互体验

**Target features:**
- 触感反馈覆盖记账键盘、设置、周期选择和快捷入口，设置中保留总开关
- 打开备注编辑时自动弹出输入法
- 删除分类管理中残留的旧二级默认分类，分类管理与记账页新增记录的分类选择保持一致
- 分类管理补充常用图标并优化 Clay 风格 UI，移除颜色圆点和右侧编辑图标
- 点击版本号打开应用详情弹窗，弹窗中提供项目 GitHub 仓库链接和手动检查更新

## 当前状态（2026-05-29）

v1.1.0 发布打磨已完成本地实现、验证、知识库收尾、GSD 归档与本地 tag。标准验证通过：`testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`。

本地 `v1.1.0` tag 当前指向最新本地 HEAD；应用实现验证提交为：`06c7556`，其后追加了知识库收尾提交。远端发布尚未完成，因为当前环境的 GitHub token 无效且 sandbox 内 SSH 访问 `known_hosts` 被拒绝，需要在可用凭据/SSH 环境中推送分支和 tag。

## 历史里程碑

- 网盘自动备份、UI 修复、账本命名统一、清除数据功能 — 已实现并随 1.0.7 发布。
- 分类扁平化与本地 30 天回收站 — 已实现。
- v1.0.11 UX 打磨（震动反馈、备注自动聚焦、分类拖拽排序、版本入口）— 已实现并被 v1.1.0 发布打磨进一步完善。
- 统计页支出构成分类下钻 — 已实现并随 1.0.3 发布。
- 周期模板、首页快捷入口、还原预览与多周期预算 — 已实现并随 1.0.4 发布。
- 智能导入、应用内更新、统计体验改进 — 已实现并随 1.0.5 发布。
- （初版）多周期首页与多周期预算 — 已实现。

## 技术栈摘要

见 `.planning/codebase/STACK.md`。

## 约束

- 不修改用户未要求的文档与无关模块；新功能与现有导航、统计周期状态保持一致。
- `.planning/` 当前被 `.gitignore` 忽略，内容面向本地 agent 交接，不代表远端仓库文档一定同步。

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone:**
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-29 after v1.1.0 milestone archive*
