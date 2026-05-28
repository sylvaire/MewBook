---
phase: 05-category-cleanup-reorder
plan: 01
subsystem: domain-model
tags: [kotlin, category, refactor, legacy-cleanup]

# Dependency graph
requires: []
provides:
  - Unified flat expense category model in DefaultCategories
  - 2026-05-29 follow-up: retired default secondary categories are filtered from current visible/default lists while import compatibility remains
  - Removal of flatExpenseAdditions, legacyExpenseSubCategoryNames, recordEntryExpenseCategories
affects: [05-02-category-drag-reorder]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DefaultCategories uses single expenseCategories derived from merged baseExpenseCategories"
    - "CategorySelectionPolicy treats all RecordTypes uniformly (no expense-only special case)"

key-files:
  created: []
  modified:
    - app/src/main/java/com/mewbook/app/domain/model/Category.kt
    - app/src/main/java/com/mewbook/app/domain/policy/CategorySelectionPolicy.kt

key-decisions:
  - "Merged 51 flatExpenseAdditions directly into baseExpenseCategories instead of keeping separate lists"
  - "Retained selectedCategoryId parameter in recordSelectionCandidates signature for backward compatibility"
  - "Kept normalizeSortOrder function unchanged (dedup by type+name, then renumber sortOrder)"

patterns-established:
  - "Single-source-of-truth: current visible categories are flat; retired legacy names remain only for compatibility/editing edge cases"

requirements-completed: [CAT-01]

# Metrics
duration: 10min
completed: 2026-05-26
---

# Phase 5 Plan 1: 删除二级分类残留代码 Summary

**Removed legacy subcategory code — merged 87 expense categories into unified list, eliminated type-specific filtering in CategorySelectionPolicy**

> 2026-05-29 follow-up: default legacy secondary categories such as "早餐", "地铁", "打车", and "房租" were retired from current defaults/visible lists. `CategorySelectionPolicy` now keeps a selected retired category visible only while editing older records, and新增记录的分类选择与分类管理保持一致。

## Performance

- **Duration:** 10min
- **Started:** 2026-05-26
- **Completed:** 2026-05-26
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Merged 51 flatExpenseAdditions into baseExpenseCategories (36+51=87 total expense categories)
- Removed legacy fields: flatExpenseAdditions, legacyExpenseSubCategoryNames, recordEntryExpenseCategories
- Simplified CategorySelectionPolicy.recordSelectionCandidates to uniform behavior across all RecordTypes
- All callers (AddEditRecordSheet, HomeQuickEntryCategoryPolicy, InitializeDefaultCategoriesUseCase) remain compatible

## Task Commits

Each task was committed atomically:

1. **Task 1: Merge flatExpenseAdditions into baseExpenseCategories and remove legacy fields** - `591739d` (feat)
2. **Task 2: Simplify recordSelectionCandidates to remove legacy subcategory filtering** - `e680e14` (feat)

## Files Created/Modified
- `app/src/main/java/com/mewbook/app/domain/model/Category.kt` - Merged expense categories into single baseExpenseCategories list; removed flatExpenseAdditions, legacyExpenseSubCategoryNames, recordEntryExpenseCategories
- `app/src/main/java/com/mewbook/app/domain/policy/CategorySelectionPolicy.kt` - Removed RecordEntryExpenseCategoryNames and LegacyExpenseSubCategoryNames caches; recordSelectionCandidates now returns visibleCategories uniformly

## Decisions Made
- Retained `selectedCategoryId` parameter in `recordSelectionCandidates` signature even though unused — avoids breaking caller code in AddEditRecordSheet.kt and HomeQuickEntryCategoryPolicy.kt
- Kept `normalizeSortOrder` function as-is — its `distinctBy { it.type to it.name }` dedup logic handles the merged 87-item list correctly
- Compatibility files (BackupImportPolicy, SmartImportPolicy, BackupMigration) intentionally left untouched per CONTEXT.md decisions

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Gradle compile verification (`./gradlew :app:compileDebugKotlin`) could not be run due to execution environment restrictions. All structural acceptance criteria (grep-based checks) pass for both tasks.
- gsd-tools state commands (state advance-plan, state update-progress, roadmap update-plan-progress, requirements mark-complete) could not be run due to the same restriction. STATE.md updated manually.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- CAT-01 complete. DefaultCategories provides a clean, unified expenseCategories list.
- Ready for CAT-02: drag-to-reorder category sorting with sh.calvin.reorderable library.
- Current follow-up state: CategorySelectionPolicy filters retired default secondary categories for new records; custom categories with the same names remain visible, and selected retired categories remain visible while editing older records.

---
*Phase: 05-category-cleanup-reorder*
*Completed: 2026-05-26*
