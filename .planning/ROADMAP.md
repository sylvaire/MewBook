# Roadmap

**Milestone:** v1.0.11 -- UX 打磨与交互优化

## Phases

- [ ] **Phase 4: 震动反馈** -- 数字键盘按键震动反馈与设置开关
- [ ] **Phase 5: 分类清理与拖拽排序** -- 删除旧二级分类残留代码，分类列表支持长按拖拽排序
- [ ] **Phase 6: 输入与设置收尾** -- 备注自动弹输入法，版本号可点击跳转 GitHub

## Phase Details

### Phase 4: 震动反馈
**Goal:** 用户在数字键盘每次按键时获得触觉确认，并可在设置中控制该行为

**Depends on:** Nothing (milestone start)

**Requirements:** HAPT-01, HAPT-02

**Success Criteria** (what must be TRUE):
  1. 用户在数字键盘上点击任意数字键或运算符键时，设备产生短暂震动反馈
  2. 用户可在设置页面找到震动反馈开关，并切换开启/关闭状态
  3. 当震动开关关闭时，数字键盘按键不再产生震动
  4. 震动开关状态在应用重启后保持不变

**Plans:** 1 plan

Plans:
- [ ] 04-01-PLAN.md -- Create HapticPreferencesRepository, add settings toggle, wire haptic feedback into KeyboardKey

### Phase 5: 分类清理与拖拽排序
**Goal:** 分类相关代码彻底清除旧二级分类残留，用户可通过长按拖拽对分类列表进行排序

**Depends on:** Phase 4

**Requirements:** CAT-01, CAT-02

**Success Criteria** (what must be TRUE):
  1. 代码库中不再存在旧二级分类的父子层级字段、表结构、迁移逻辑或相关引用
  2. 所有分类相关界面（管理、选择、统计下钻）在单层分类模型下正常工作
  3. 用户在分类列表中长按某一分类可触发拖拽模式
  4. 用户可将分类拖拽至列表中新的位置，松手后分类排在目标位置
  5. 拖拽排序后的顺序持久保存，并在所有分类选择界面上保持一致显示

**Plans:** TBD

**UI hint:** yes

### Phase 6: 输入与设置收尾
**Goal:** 备注输入体验更流畅，设置页版本号提供快捷仓库访问

**Depends on:** Phase 5

**Requirements:** INPT-01, SETT-01

**Success Criteria** (what must be TRUE):
  1. 用户进入记账页备注编辑区域时，系统输入法自动弹出，无需手动点击输入框
  2. 用户在设置页面点击版本号，系统浏览器打开 GitHub 仓库页面

**Plans:** TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 4. 震动反馈 | 0/1 | Not started | - |
| 5. 分类清理与拖拽排序 | 0/1 | Not started | - |
| 6. 输入与设置收尾 | 0/1 | Not started | - |
