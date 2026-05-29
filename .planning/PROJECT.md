# MewBook — 项目状态

## 产品

MewBook：Android 本地记账应用（Kotlin、Jetpack Compose、Room、Hilt）。核心能力包括流水、单层分类、账本、资产、预算、周期模板、统计下钻、删除找回、WebDAV 同步、自动备份、备份导出、智能导入与应用内更新。

## 当前里程碑：v1.2.0 快速记账与预算预警

里程碑文档：`.planning/milestones/v1.2.0.md`

最近完成并归档：v1.1.0 发布打磨与交互优化（归档见 `.planning/archive/milestones/v1.1.0.md`）。

**Goal:** 让高频记账路径更少点击，并把预算从事后查看升级为提前预警

**Target features:**
- 快速记账面板提供常用金额和常用场景/分类一键入口
- 根据最近成功记录和时间段自动预填快速记账的账户、分类和金额建议
- 首页 FAB 支持双击直达快捷记账，同时保留单击完整记账和长按快捷菜单
- 总预算和分类预算提供 50%、80%、100% 使用率预警
- 基于当前日均支出预测月末是否超支
- 对分类预算或分类支出的异常波动给出可解释提示

## 当前状态（2026-05-29）

v1.2.0 已完成本地实现：Phase 8 快速记账提速新增常用金额、常用场景、时间段记忆和双击 FAB；Phase 9 预算预警新增阈值提醒、月末预测和分类异常波动提示。完整回归曾在最终边角修复前通过；最后修复“记忆分类不在常用候选中仍应预选”的改动后，复跑被沙箱 C 盘空间不足阻塞。后续发布前需清理环境后复跑验证，并按版本策略决定是否升级版本号、提交并推送。

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
*Last updated: 2026-05-29 after v1.2.0 Phase 8 and Phase 9 implementation*
