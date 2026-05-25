# MewBook — 项目状态

## 产品

MewBook：Android 本地记账应用（Kotlin、Jetpack Compose、Room、Hilt）。核心能力包括流水、单层分类、账本、资产、预算、周期模板、统计下钻、删除找回、WebDAV 同步、自动备份、备份导出、智能导入与应用内更新。

## 当前里程碑：v1.0.11 UX 打磨与交互优化

**Goal:** 提升记账、分类管理和设置等核心操作的交互体验

**Target features:**
- 分类数字键盘添加震动反馈，设置中增加震动开关
- 打开备注编辑时自动弹出输入法
- 彻底删除代码中残留的旧二级分类相关代码，分类管理支持长按拖拽排序
- 点击版本号可查看 GitHub 仓库地址

## 当前状态（2026-05-26）

上次里程碑 **分类扁平化与删除找回收尾** 已于 2026-05-24 完成，存在未提交代码变更。发版前必须重新运行标准验证并阅读 `git diff`。

新里程碑 v1.0.11 启动于 2026-05-26。

## 历史里程碑

- 网盘自动备份、UI 修复、账本命名统一、清除数据功能 — 已实现并随 1.0.7 发布。
- 分类扁平化与本地 30 天回收站 — 已实现于当前未提交工作区。
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
*Last updated: 2026-05-26 after milestone v1.0.11 started*
