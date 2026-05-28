---
phase: 05-category-cleanup-reorder
plan: 02
subsystem: categories-ui
tags: [drag-reorder, reorderable-library, sortOrder, categories]
depends_on: [05-01]
provides: [category-drag-reorder]
affects: [categories-screen, categories-viewmodel, category-dao, category-repository]
tech-stack:
  added: [sh.calvin.reorderable:2.2.0]
  patterns: [drag-handle, batch-sortOrder-persistence, use-case-reorder]
key-files:
  created: []
  modified:
    - app/build.gradle.kts
    - app/src/main/java/com/mewbook/app/data/local/dao/CategoryDao.kt
    - app/src/main/java/com/mewbook/app/domain/repository/CategoryRepository.kt
    - app/src/main/java/com/mewbook/app/data/repository/CategoryRepositoryImpl.kt
    - app/src/main/java/com/mewbook/app/domain/usecase/category/CategoryUseCases.kt
    - app/src/main/java/com/mewbook/app/ui/screens/categories/CategoriesViewModel.kt
    - app/src/main/java/com/mewbook/app/ui/screens/categories/CategoriesScreen.kt
decisions:
  - "sortOrder uses continuous integer assignment (0, 1, 2...) per CONTEXT.md decision"
  - "moveCategoryUp/moveCategoryDown preserved as programmatic entry points, delegate to moveCategory"
  - "Original implementation used Modifier.draggableHandle(); 2026-05-29 follow-up removed the stale modifier reference while keeping the DragHandle affordance"
duration: "~20min"
completed-date: 2026-05-26
---

# Phase 5 Plan 2: Drag Reorder Summary

**One-liner:** Category list supports long-press drag reorder via sh.calvin.reorderable library, replacing arrow buttons with DragHandle icon and persisting sortOrder via batch Room updates.

> 2026-05-29 follow-up: current `CategoriesScreen.kt` no longer references `Modifier.draggableHandle()`. The earlier unresolved `draggableHandle` build issue is resolved; release verification passed `testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`.

## Tasks Executed

### Task 1: Add reorderable dependency and batch update data layer

**Commit:** `df8b22c`

Added the `sh.calvin.reorderable:reorderable:2.2.0` dependency and built the batch update data layer:
- `app/build.gradle.kts`: Added `implementation("sh.calvin.reorderable:reorderable:2.2.0")` after Material Icons Extended
- `CategoryDao.kt`: Added `@Update suspend fun updateCategories(categories: List<CategoryEntity>)` for Room batch updates
- `CategoryRepository.kt`: Added `suspend fun updateCategories(categories: List<Category>)` interface method
- `CategoryRepositoryImpl.kt`: Implemented `updateCategories` using the existing `toEntity()` extension
- `CategoryUseCases.kt`: Added `ReorderCategoriesUseCase` which receives `List<Pair<Long, Int>>` (id to new sortOrder), fetches latest categories, updates only sortOrder, and persists via batch update

### Task 2: Add moveCategory(fromIndex, toIndex, type) and refactor existing move methods

**Commit:** `3c90159`

- Injected `ReorderCategoriesUseCase` into `CategoriesViewModel` constructor
- Added `fun moveCategory(fromIndex: Int, toIndex: Int, type: RecordType)` which validates indices, reorders the list, assigns sequential sortOrder values (0, 1, 2...), and persists via `reorderCategoriesUseCase`
- Refactored `moveCategoryUp`/`moveCategoryDown` to delegate to `moveCategory` (no longer directly manipulating sortOrder via swap)
- Preserved `reorderableSiblings` private helper for the bridge methods
- Added index bounds validation per threat model T-05-02

### Task 3: Replace arrow buttons with drag handle in CategoriesScreen using reorderable library

**Commit:** `d8cda90`

- Removed `KeyboardArrowUp` and `KeyboardArrowDown` imports and usage
- Added `DragHandle`, `ReorderableItem`, `rememberReorderableLazyListState`, `reorderable`, and originally `draggableHandle` imports; the stale `draggableHandle` reference was removed in later polish
- Added `currentType` variable derived from selected tab index
- Created `rememberReorderableLazyListState` with `onMove` callback wired to `viewModel.moveCategory()`
- Updated LazyColumn to use `reorderableState.listState` and `Modifier.reorderable(reorderableState)`
- Updated subtitle from "右侧箭头调整排序" to "长按拖拽调整排序"
- Replaced `CategoryRowItem` call with `ReorderableItem` wrapping `CategoryItemCard` directly
- Removed `CategoryRowItem` composable function entirely
- Simplified `CategoryItemCard` signature from 6 params to 2 params (`category`, `onEditClick`)
- Added a DragHandle affordance on the left side of each card row
- Removed the right-side arrow button column

## Deviations from Plan

None - plan executed exactly as written.

## Verification

- All grep-based acceptance criteria verified for each task
- Compilation verification could not be run (Gradle execution requires Android SDK environment). Manual verification steps:
  1. Run `./gradlew :app:compileDebugKotlin` in the project root
  2. Verify no compilation errors related to reorderable imports or method signatures

## Known Stubs

None.

## Threat Flags

None. All threat model mitigations (T-05-02 index bounds checks, T-05-03 small category count) are implemented.

## Self-Check

### Commits Exist

- `df8b22c` feat(05-category-cleanup-reorder): add reorderable dependency and batch update data layer -- VERIFIED
- `3c90159` feat(05-category-cleanup-reorder): add moveCategory(fromIndex, toIndex, type) to CategoriesViewModel -- VERIFIED
- `d8cda90` feat(05-category-cleanup-reorder): replace arrow buttons with drag handle using reorderable library -- VERIFIED

### Files Exist

- `app/build.gradle.kts` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/data/local/dao/CategoryDao.kt` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/domain/repository/CategoryRepository.kt` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/data/repository/CategoryRepositoryImpl.kt` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/domain/usecase/category/CategoryUseCases.kt` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/ui/screens/categories/CategoriesViewModel.kt` -- VERIFIED (modified)
- `app/src/main/java/com/mewbook/app/ui/screens/categories/CategoriesScreen.kt` -- VERIFIED (modified)

## Self-Check: PASSED
