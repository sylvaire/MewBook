# Deferred Items — Phase 06

## Pre-existing Build Failures (Out of Scope)

**1. CategoriesScreen.kt:264 — Unresolved reference: draggableHandle**
- **Found during:** Phase-level build verification (`./gradlew assembleDebug`)
- **Issue:** `CategoriesScreen.kt` references `draggableHandle` which no longer exists (likely removed in Phase 5 drag-reorder work but not cleaned up)
- **Scope:** Not caused by Phase 06 changes — pre-existing, out of scope per deviation rules
- **Recommendation:** Remove the `draggableHandle` reference from `CategoriesScreen.kt:264` in a future plan or as a standalone fix
