# Requirements: MewBook

**Defined:** 2026-05-26
**Core Value:** 本地记账应用，核心操作（记账、分类、设置）交互流畅无阻碍

## v1.0.11 Requirements

Requirements for milestone v1.0.11: UX 打磨与交互优化.

### 交互反馈 (HAPT)

- [ ] **HAPT-01**: 数字键盘按键按下时触发震动反馈
- [ ] **HAPT-02**: 设置页面提供震动开关，可控制键盘震动开关行为

### 输入体验 (INPT)

- [ ] **INPT-01**: 进入备注编辑框时自动弹出输入法，无需用户手动点击

### 分类管理 (CAT)

- [ ] **CAT-01**: 彻底删除代码中残留的旧二级分类相关代码和数据结构
- [ ] **CAT-02**: 分类列表支持长按拖拽上下移动来调整排序

### 设置 (SETT)

- [ ] **SETT-01**: 点击设置页版本号跳转 GitHub 仓库页面

## Out of Scope

| Feature | Reason |
|---------|--------|
| 震动强度调节 | v1.0.11 仅做开关，不做强度分级 |
| 批量分类排序/自动排序 | 手动长按拖拽已满足需求 |
| 备注自动弹输入法后自动收起 | 交给系统默认行为处理 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| HAPT-01 | — | Pending |
| HAPT-02 | — | Pending |
| INPT-01 | — | Pending |
| CAT-01 | — | Pending |
| CAT-02 | — | Pending |
| SETT-01 | — | Pending |

**Coverage:**
- v1.0.11 requirements: 6 total
- Mapped to phases: 0
- Unmapped: 6 ⚠️

---
*Requirements defined: 2026-05-26*
*Last updated: 2026-05-26 after initial definition*
