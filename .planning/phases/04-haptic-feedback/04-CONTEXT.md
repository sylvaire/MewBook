# Phase 4: 震动反馈 - Context

**Gathered:** 2026-05-26
**Status:** Ready for planning

<domain>
## Phase Boundary

为记账页自定义数字键盘的每个按键添加震动反馈，并在设置页提供震动开关。仅涉及 `AddEditRecordSheet.kt` 的 `KeyboardPanel` / `KeyboardKey` 和设置模块。

</domain>

<decisions>
## Implementation Decisions

### 震动触发范围
- **仅记账页自定义数字键盘**（`AddEditRecordSheet.kt` 中的 `KeyboardPanel`）
- 所有按键均触发震动：0-9 数字、`.`、`+`、`-`、删除、清空、保存/完成
- 不涉及系统输入法或其他页面的键盘

### 震动类型
- Android 14+ (API 34+): `HapticFeedbackType.KeyboardTap`
- Android < 34: 降级为 `HapticFeedbackType.LongPress`
- 通过 `LocalHapticFeedback.current` 获取 haptic 实例

### 设置开关
- 放在设置页"偏好"分区，使用 `SettingsSwitchRowCard` 组件
- 位置：在 `showHomeOverviewCards` 开关之后、"首页显示周期"之前
- 标题：「按键震动」，副标题：「记账键盘按键时触发震动反馈」

### 持久化
- 新增 `HapticPreferencesRepository`（DataStore-backed），或复用现有 `HomePreferencesRepository`
- 默认值：开启（true）
- 重启后开关状态保持

### Claude's Discretion
- 是否新建独立 Repository 还是扩展已有的 `HomePreferencesRepository`
- `KeyboardKey` 中 haptic 的具体注入方式（参数传递 vs CompositionLocal）
- 震动开关关闭时是否传递到键盘（通过 ViewModel/StateFlow）

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 核心修改文件
- `app/src/main/java/com/mewbook/app/ui/screens/add/AddEditRecordSheet.kt` — `KeyboardKey` 和 `KeyboardPanel` composable
- `app/src/main/java/com/mewbook/app/ui/screens/settings/SettingsScreen.kt` — 设置页添加开关
- `app/src/main/java/com/mewbook/app/ui/screens/settings/SettingsViewModel.kt` — 设置 ViewModel（可能需要新增 StateFlow）

### 参考模式
- `app/src/main/java/com/mewbook/app/ui/components/SettingsSwitchRowCard.kt` — 设置开关组件
- `app/src/main/java/com/mewbook/app/data/preferences/HomePreferencesRepository.kt` — DataStore 偏好存储参考模式
- `app/src/main/java/com/mewbook/app/ui/screens/settings/SettingsScreen.kt` — 现有设置页结构

</canonical_refs>

<specifics>
## Specific Ideas

无需额外设计说明。
</specifics>

<deferred>
## Deferred Ideas

- 震动强度调节
- 其他页面的震动反馈
</deferred>

---

*Phase: 04-haptic-feedback*
*Context gathered: 2026-05-26 via discuss-phase*
