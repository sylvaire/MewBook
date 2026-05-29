# Requirements: MewBook

**Defined:** 2026-05-26
**Core Value:** 本地记账应用，核心操作（记账、分类、设置）交互流畅无阻碍

## v1.2.0 Requirements (Active)

Requirements for milestone v1.2.0: 快速记账提速与预算预警系统。

### 快速记账增强 (QE)

- [x] **QE-01**: 快速记账面板提供常用金额一键按钮，第一版至少覆盖 `9.9`、`12`、`25` 等默认候选，并支持后续按历史使用排序
- [x] **QE-02**: 快速记账面板提供常用场景/分类一键入口，例如午餐、通勤；候选必须来自当前账本可用分类或能安全映射到可用分类
- [x] **QE-03**: 快速记账根据记录类型和时间段记忆最近成功输入，自动预填账户、分类和金额建议
- [x] **QE-04**: 首页 FAB 支持双击直接进入快捷记账，同时保留单击完整记账和长按快捷菜单
- [x] **QE-05**: 快速记账记忆数据仅保存在本地，保存成功后更新，清除数据时一并清理

### 预算预警 (BWA)

- [x] **BWA-01**: 总预算和分类预算支持 50%、80%、100% 使用率阈值状态，提醒不重复刷屏
- [x] **BWA-02**: 月度预算支持基于当前日均支出的月末超支预测，并展示预测依据
- [x] **BWA-03**: 分类支出支持与上周或上月同类数据比较，识别异常波动并给出提示
- [x] **BWA-04**: 预算页展示可解释预警摘要，作为 inline 状态呈现而不重复弹窗刷屏
- [x] **BWA-05**: 无预算、数据不足或历史样本不足时，预警策略必须稳定降级为不提示

## v1.1.0 Requirements (Archived)

Requirements for milestone v1.1.0: 发布打磨、分类管理一致性、设置入口收敛与触感反馈扩展。

### 交互反馈 (HAPT)

- [x] **HAPT-01**: 数字键盘按键按下时触发震动反馈
- [x] **HAPT-02**: 设置页面提供“触感反馈”开关，可控制键盘、设置操作、周期选择和快捷入口等合适点击事件的震动反馈
- [x] **HAPT-03**: 触感反馈策略集中在可测试 policy 中，关闭偏好后所有受控触感入口都不触发

### 输入体验 (INPT)

- [x] **INPT-01**: 进入备注编辑框时自动弹出输入法，无需用户手动点击

### 分类管理 (CAT)

- [x] **CAT-01**: 彻底删除代码中残留的旧二级分类相关代码和数据结构
- [x] **CAT-02**: 分类列表支持长按拖拽上下移动来调整排序
- [x] **CAT-03**: 删除默认分类中残留的旧二级分类项；分类管理与记账页新增记录的分类选择保持一致
- [x] **CAT-04**: 分类管理补充更多常用图标（含汽车/交通等），并用测试锁定关键图标选项
- [x] **CAT-05**: 分类管理 UI 与整体 Clay 风格匹配，移除颜色圆点和右侧编辑图标，修正分段控件内外圆角不协调

### 设置 (SETT)

- [x] **SETT-01**: 点击设置页版本号打开应用详情弹窗，而不是直接跳转网页
- [x] **SETT-02**: 设置列表删除独立“检查更新”入口，保留版本详情弹窗中的“检查更新”
- [x] **SETT-03**: 应用详情弹窗展示项目 GitHub 仓库链接

## Out of Scope

| Feature | Reason |
|---------|--------|
| 云端个性化推荐 | v1.2.0 只做本地历史记忆和规则策略 |
| 复杂机器学习模型 | 预算预警先使用可解释、可测试的规则算法 |
| 推送通知 | 先做应用内提示，避免权限、后台和打扰频率复杂化 |
| 自定义提醒规则编辑器 | 默认阈值和稳定体验优先 |
| 震动强度调节 | v1.1.0 仅做开关，不做强度分级 |
| 批量分类排序/自动排序 | 手动长按拖拽已满足需求 |
| 备注自动弹输入法后自动收起 | 交给系统默认行为处理 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| HAPT-01 | Phase 4 | Complete |
| HAPT-02 | Phase 4 + v1.1.0 polish | Complete |
| HAPT-03 | v1.1.0 polish | Complete |
| INPT-01 | Phase 6 | Complete |
| CAT-01 | Phase 5 | Complete |
| CAT-02 | Phase 5 | Complete |
| CAT-03 | v1.1.0 polish | Complete |
| CAT-04 | v1.1.0 polish | Complete |
| CAT-05 | v1.1.0 polish | Complete |
| SETT-01 | Phase 6 + v1.1.0 polish | Complete |
| SETT-02 | v1.1.0 polish | Complete |
| SETT-03 | v1.1.0 polish | Complete |
| QE-01 | Phase 8 | Complete |
| QE-02 | Phase 8 | Complete |
| QE-03 | Phase 8 | Complete |
| QE-04 | Phase 8 | Complete |
| QE-05 | Phase 8 | Complete |
| BWA-01 | Phase 9 | Complete |
| BWA-02 | Phase 9 | Complete |
| BWA-03 | Phase 9 | Complete |
| BWA-04 | Phase 9 | Complete |
| BWA-05 | Phase 9 | Complete |

**Coverage:**
- v1.1.0 requirements: 12 total
- Mapped to phases/polish: 12
- Unmapped: 0
- v1.2.0 requirements: 10 total
- Mapped to completed phases: 10
- Unmapped: 0

---
*Requirements defined: 2026-05-26*
*Last updated: 2026-05-29 after Phase 8 and Phase 9 implementation*
