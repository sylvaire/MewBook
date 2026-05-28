# Requirements: MewBook

**Defined:** 2026-05-26
**Core Value:** 本地记账应用，核心操作（记账、分类、设置）交互流畅无阻碍

## v1.1.0 Requirements

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

**Coverage:**
- v1.1.0 requirements: 12 total
- Mapped to phases/polish: 12
- Unmapped: 0

---
*Requirements defined: 2026-05-26*
*Last updated: 2026-05-29 after local v1.1.0 release prep*
