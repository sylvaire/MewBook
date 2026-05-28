# Roadmap

**Milestone:** v1.1.0 -- 发布打磨与交互优化

## Phases

- [x] **Phase 4: 震动反馈** -- 数字键盘按键震动反馈与设置开关
- [x] **Phase 5: 分类清理与拖拽排序** -- 删除旧二级分类残留代码，分类列表支持长按拖拽排序
- [x] **Phase 6: 输入与设置收尾** -- 备注自动弹输入法，版本号入口可点击 (completed 2026-05-26)
- [x] **Phase 7: v1.1.0 发布打磨** -- 分类管理 UI/图标/一致性、版本详情弹窗、触感反馈扩展、版本与 release 文案 (completed 2026-05-29)

## Phase Details

### Phase 4: 震动反馈
**Goal:** 用户在数字键盘每次按键时获得触觉确认，并可在设置中控制该行为；v1.1.0 进一步扩展为“触感反馈”总开关

**Depends on:** Nothing (milestone start)

**Requirements:** HAPT-01, HAPT-02, HAPT-03

**Success Criteria** (what must be TRUE):
  1. 用户在数字键盘上点击任意数字键或运算符键时，设备产生短暂震动反馈
  2. 用户可在设置页面找到“触感反馈”开关，并切换开启/关闭状态
  3. 当触感反馈关闭时，数字键盘、设置操作、周期选择和快捷入口等受控入口不再产生震动
  4. 震动开关状态在应用重启后保持不变

**Plans:** 1/1 plans complete

Plans:
- [x] 04-01-PLAN.md -- Create HapticPreferencesRepository, add settings toggle, wire haptic feedback into KeyboardKey

### Phase 5: 分类清理与拖拽排序
**Goal:** 分类相关代码彻底清除旧二级分类残留，用户可通过长按拖拽对分类列表进行排序

**Depends on:** Phase 4

**Requirements:** CAT-01, CAT-02

**Success Criteria** (what must be TRUE):
  1. 当前 Room/领域/备份模型中不再存在旧二级分类的父子层级字段，导入兼容路径保留对旧外部数据的解析
  2. 所有分类相关界面（管理、记账选择、统计下钻）在单层分类模型下正常工作
  3. 用户在分类列表中长按某一分类可触发拖拽模式
  4. 用户可将分类拖拽至列表中新的位置，松手后分类排在目标位置
  5. 拖拽排序后的顺序持久保存，并在所有分类选择界面上保持一致显示
  6. 新增记录时，记账页分类选择与分类管理显示的可见分类一致；编辑旧记录时保留当前已选退休分类

**Plans:** 2/2 plans complete
- [x] 05-01-PLAN.md -- Clean up legacy subcategory code: merge flatExpenseAdditions, remove filtering in CategorySelectionPolicy (CAT-01)
- [x] 05-02-PLAN.md -- Add drag reorder to category list: reorderable library, DragHandle, batch sortOrder persistence (CAT-02)


### Phase 6: 输入与设置收尾
**Goal:** 备注输入体验更流畅，设置页版本号成为应用详情入口

**Depends on:** Phase 5

**Requirements:** INPT-01, SETT-01

**Success Criteria** (what must be TRUE):
  1. 用户进入记账页备注编辑区域时，系统输入法自动弹出，无需手动点击输入框
  2. 用户在设置页面点击版本号，打开应用详情弹窗；项目仓库链接在弹窗内展示

**Plans:** 1/1 plans complete

### Phase 7: v1.1.0 发布打磨
**Goal:** 把分类、设置、触感反馈和发布信息整理成可发布的 1.1.0 版本

**Depends on:** Phase 6

**Requirements:** HAPT-02, HAPT-03, CAT-03, CAT-04, CAT-05, SETT-01, SETT-02, SETT-03

**Success Criteria** (what must be TRUE):
  1. 分类管理不再展示默认旧二级分类残留项，图标选项覆盖汽车/交通等常见场景
  2. 分类管理移除颜色圆点和右侧编辑图标，分段控件圆角与外层卡片协调
  3. 版本详情弹窗展示应用、版本、构建号、更新状态和项目 GitHub 仓库链接
  4. 设置页没有独立手动“检查更新”行，手动检查入口保留在版本详情弹窗
  5. 合适的点击/触摸事件通过统一 haptic policy 控制
  6. `versionName`/`versionCode`、README、release workflow 文案与 1.1.0 一致

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 4. 震动反馈 | 1/1 | Complete   | 2026-05-26 |
| 5. 分类清理与拖拽排序 | 2/2 | Complete | 2026-05-26 |
| 6. 输入与设置收尾 | 1/1 | Complete   | 2026-05-26 |
| 7. v1.1.0 发布打磨 | ad hoc polish | Complete | 2026-05-29 |
