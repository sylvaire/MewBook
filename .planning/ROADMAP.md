# 路线图

## 里程碑：统计页支出构成分类下钻

### Phase 1 — 数据与导航契约

- 明确统计子页所需参数：`categoryId`、`TimeRange`、锚点日期或等价 `startDate`/`endDate`、`ledgerId`（默认账本）。
- 在 `RecordDao` / `RecordRepository`（或 UseCase）增加按「账本 + 日期区间 + 分类 + 支出类型」的查询（或复用组合过滤），返回 `Flow`/`List` 与现有模型一致。
- 在 `Screen` / `NavHost` 增加路由，例如 `statistics_category_expense/{categoryId}`，使用 `SavedStateHandle` 或导航参数传递周期（推荐 **Navigation Compose 类型安全参数** 或 **单一字符串编码周期**，避免歧义）。

### Phase 2 — UI：分类支出明细页

- 新建 `CategoryExpenseDetailScreen` + `CategoryExpenseDetailViewModel`（Hilt）。
- 顶栏：返回、标题（分类名 + 周期）；主体：`LazyColumn` + 复用 `RecordItem` 或精简行。
- `StatisticsScreen` 中 `ExpenseBreakdown` 为每行增加 `clickable` / `Modifier.clickable`，调用 `onCategoryClick(categoryId)`，由 `StatisticsScreen` 持有 `NavController` 或回调到 `NavHost` 导航。

### Phase 3 — 验证

- 单元测试：日期区间与分类过滤逻辑（若有独立函数）。
- 手动：周/月/年 + 切换周期 + 点击不同分类，核对笔数与合计。

## 后续（Backlog，非本里程碑）

- 明细页支持长按跳转编辑流水。
- 支出构成展示「全部 N 个分类」与折叠。

## 依赖关系

- Phase 2 依赖 Phase 1 的路由与查询就绪。
