# Roadmap

**Milestone:** v1.2.0 -- 快速记账与预算预警 (active, created 2026-05-29)

## Phases

- [x] **Phase 8: 快速记账提速** -- 常用金额/场景一键入口、上次输入记忆、双击 FAB 快捷记账 (completed 2026-05-29)
- [x] **Phase 9: 预算预警系统** -- 阈值提醒、月末超支预测、分类异常波动提示 (completed 2026-05-29)
- [x] **Phase 4: 震动反馈** -- 数字键盘按键震动反馈与设置开关
- [x] **Phase 5: 分类清理与拖拽排序** -- 删除旧二级分类残留代码，分类列表支持长按拖拽排序
- [x] **Phase 6: 输入与设置收尾** -- 备注自动弹输入法，版本号入口可点击 (completed 2026-05-26)
- [x] **Phase 7: v1.1.0 发布打磨** -- 分类管理 UI/图标/一致性、版本详情弹窗、触感反馈扩展、版本与 release 文案 (completed 2026-05-29)

## Phase Details

### Phase 8: 快速记账提速
**Goal:** 用户在首页通过更少点击完成日常高频记账，快捷记账能记住常用账户、分类、金额和场景

**Depends on:** v1.1.0 发布打磨完成

**Requirements:** QE-01, QE-02, QE-03, QE-04, QE-05

**Success Criteria** (what must be TRUE):
  1. 快速记账面板展示常用金额按钮，用户点击后金额立即填入并保留触感反馈
  2. 快速记账面板展示常用场景/分类候选，候选与当前账本可见分类兼容
  3. 用户在相近时间段再次打开快速记账时，账户和分类能按最近成功记录自动预填
  4. 双击首页 FAB 直接进入快捷记账，不破坏单击完整记账和长按快捷菜单
  5. 快速记账记忆策略有可单元测试的 policy 层，清除数据时不会留下孤立偏好

**Plans:** 1/1 plans complete

Plans:
- [x] 08-01-PLAN.md -- Design and implement quick-entry acceleration policy, UI, FAB gesture handling, and tests

### Phase 9: 预算预警系统
**Goal:** 用户在超预算前看到可解释、克制的风险提示，并能定位到具体预算或分类

**Depends on:** Phase 8

**Requirements:** BWA-01, BWA-02, BWA-03, BWA-04, BWA-05

**Success Criteria** (what must be TRUE):
  1. 总预算和分类预算能产生 50%、80%、100% 使用率状态，且同一周期不重复刷屏
  2. 月度预算能根据当前日均支出预测月底是否超支，并展示预测依据
  3. 分类支出能与上周或上月同类数据比较，识别明显异常波动
  4. 预算页展示可解释的预警摘要，避免重复弹窗刷屏
  5. 无预算或历史样本不足时，预警系统稳定降级为不提示

**Plans:** 1/1 plans complete

Plans:
- [x] 09-01-PLAN.md -- Design and implement budget alert calculators, UI surfaces, and tests

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
| 8. 快速记账提速 | 1/1 | Complete | 2026-05-29 |
| 9. 预算预警系统 | 1/1 | Complete | 2026-05-29 |
| 4. 震动反馈 | 1/1 | Complete   | 2026-05-26 |
| 5. 分类清理与拖拽排序 | 2/2 | Complete | 2026-05-26 |
| 6. 输入与设置收尾 | 1/1 | Complete   | 2026-05-26 |
| 7. v1.1.0 发布打磨 | ad hoc polish | Complete | 2026-05-29 |

## Archive

- Milestone archive: `.planning/archive/milestones/v1.1.0.md`
- Status: implementation complete, locally verified, locally tagged, remote publication pending valid GitHub credentials or SSH host-key access.

## Active Milestone Document

- v1.2.0 milestone: `.planning/milestones/v1.2.0.md`
