# MewBook — 项目状态

## 产品

MewBook：Android 本地记账应用（Kotlin、Jetpack Compose、Room、Hilt）。核心能力包括流水、分类、账本、资产、预算、WebDAV 同步与备份导出。

## 当前里程碑（进行中）

**名称**：统计页支出构成分类下钻

**目标**：在「统计」页「支出构成」中，用户点击某一分类后，可进入该分类在当前统计周期（周/月/年）内的**具体支出流水**列表，便于核对每一笔支出。

**状态**：需求与路线图已写入 `.planning/REQUIREMENTS.md`、`.planning/ROADMAP.md`；执行前见 `.planning/STATE.md`。

## 历史里程碑

- （初版）多周期首页与多周期预算 — 已实现（代码库）。

## 技术栈摘要

见 `.planning/codebase/STACK.md`。

## 约束

- 不修改用户未要求的文档与无关模块；新功能与现有导航、统计周期状态保持一致。
