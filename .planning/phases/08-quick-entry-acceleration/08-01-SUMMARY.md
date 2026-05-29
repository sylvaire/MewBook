---
phase: 08-quick-entry-acceleration
plan: 01
subsystem: domain-ui
tags: [compose, datastore, hilt, quick-entry, gesture, policy]

# Dependency graph
requires:
  - v1.1.0
provides:
  - QuickEntryPreferencesRepository with DataStore-backed quick-entry memory
  - QuickEntryDefaultsPolicy for time-slot-based default resolution
  - QuickEntryAmountSuggestionsPolicy for default + history-based amount suggestions
  - QuickEntryFabGesturePolicy for single/double/long press gesture decisions
  - QuickAddRecordSheet with common amount chips and scene/category chips
  - HomeScreen FAB double-tap to open quick entry directly
  - BackupRepository integration to clear quick-entry memory on data reset
affects: [home, add-record, backup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DataStore serialized preference: QuickEntryPreferencesRepository stores serialized List<QuickEntryMemory> in single string key"
    - "Policy pattern: pure Kotlin classes (QuickEntryDefaultsPolicy, QuickEntryAmountSuggestionsPolicy, QuickEntryFabGesturePolicy) with no Android/Compose dependencies"
    - "FAB gesture gate: remembered click job with 280ms delay window; double-tap cancels pending single-tap"
    - "Memory lifecycle: save on quick-entry success → DataStore → Flow → policy → UI defaults; clear on clearAllData"

key-files:
  created:
    - app/src/main/java/com/mewbook/app/data/preferences/QuickEntryPreferencesRepository.kt
    - app/src/main/java/com/mewbook/app/domain/policy/QuickEntryDefaultsPolicy.kt
    - app/src/main/java/com/mewbook/app/domain/policy/QuickEntryAmountSuggestionsPolicy.kt
    - app/src/main/java/com/mewbook/app/domain/policy/QuickEntryFabGesturePolicy.kt
    - app/src/test/java/com/mewbook/app/domain/policy/QuickEntryDefaultsPolicyTest.kt
    - app/src/test/java/com/mewbook/app/domain/policy/QuickEntryAmountSuggestionsPolicyTest.kt
    - app/src/test/java/com/mewbook/app/domain/policy/QuickEntryFabGesturePolicyTest.kt
  modified:
    - app/src/main/java/com/mewbook/app/ui/screens/home/HomeViewModel.kt
    - app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt
    - app/src/main/java/com/mewbook/app/ui/screens/add/QuickAddRecordSheet.kt
    - app/src/main/java/com/mewbook/app/data/repository/BackupRepository.kt

key-decisions:
  - "Quick-entry memory stored as serialized JSON string in DataStore (not Room) — keeps phase scope small, no schema migration needed"
  - "Time slots are coarse local-only buckets (morning/noon/afternoon/evening/night) used only for default resolution, not displayed to user"
  - "Amount suggestions combine fixed defaults [9.9, 12.0, 25.0] with recent quick-entry amounts, de-duplicated at cents level, capped at 6"
  - "FAB double-tap uses remembered coroutine job with 280ms window rather than Compose combinedClickable for cleaner gesture separation"
  - "Quick-entry memory capped at 24 recent records to prevent unbounded preference growth"

patterns-established:
  - "QuickEntryPreferencesRepository: DataStore-backed serialized preference with Flow + suspend setter + one-shot clear, matching HapticPreferencesRepository pattern"
  - "Policy-first architecture: pure Kotlin policy classes tested independently, wired into ViewModel and Compose separately"
  - "FAB gesture resolution: QuickEntryFabGesturePolicy.resolveTap() returns SingleTap/DoubleTap/LongPress; UI uses remembered click gate"

requirements-completed: [QE-01, QE-02, QE-03, QE-04, QE-05]

# Metrics
completed: 2026-05-29
---

# Phase 8 Plan 1: 快速记账提速 Summary

**Quick-entry panel with common amount chips, scene/category chips, time-slot memory pre-fill, and FAB double-tap shortcut**

## Accomplishments

- Created `QuickEntryPreferencesRepository` with DataStore-backed quick-entry memory (serialized `List<QuickEntryMemory>`, capped at 24 records)
- Implemented `QuickEntryDefaultsPolicy` for time-slot-based default resolution: prefers exact ledger+type+slot memory, falls back to type-level latest, then current defaults
- Implemented `QuickEntryAmountSuggestionsPolicy` merging fixed defaults `[9.9, 12.0, 25.0]` with recent quick-entry amounts, de-duplicated and capped at 6
- Implemented `QuickEntryFabGesturePolicy` with 280ms double-tap window; returns `SingleTap`, `DoubleTap`, or `LongPress`
- Extended `HomeUiState` with `quickAmountSuggestions`, `quickDefaultCategoryId`, `quickDefaultAccountId`, `quickDefaultAmount`
- Updated `QuickAddRecordSheet` with common amount chips ("常用金额") and scene/category chips with haptic feedback
- Wired FAB double-tap in `HomeScreen.HomeFloatingAddButton` using remembered click gate pattern
- Integrated quick-entry memory clear into `BackupRepository.clearAllData()`
- Added haptic feedback for amount chips, category chips, account chips, cancel, save, and full editor actions

## Task Commits

1. **Task 1: Add quick-entry policy tests and policy classes** — policy-first TDD
2. **Task 2: Add local quick-entry memory repository** — DataStore persistence
3. **Task 3: Wire quick-entry defaults into HomeViewModel** — ViewModel integration
4. **Task 4: Update QuickAddRecordSheet UI** — amount and scene chips
5. **Task 5: Add FAB double-tap behavior** — gesture wiring

## Files Created/Modified

**Created:**
- `QuickEntryPreferencesRepository.kt` — DataStore-backed memory with `rememberQuickEntry()`, `clearQuickEntryMemory()`, `memories` Flow
- `QuickEntryDefaultsPolicy.kt` — `QuickEntryMemory` data class, `QuickEntryTimeSlot` enum, `resolve()` method
- `QuickEntryAmountSuggestionsPolicy.kt` — `suggest()` merging defaults with history
- `QuickEntryFabGesturePolicy.kt` — `resolveTap()` with 280ms window
- `QuickEntryDefaultsPolicyTest.kt` — 6 tests (time-slot, fallback, stale IDs, ledger/type separation)
- `QuickEntryAmountSuggestionsPolicyTest.kt` — 4 tests (ordering, dedup, filtering, limit)
- `QuickEntryFabGesturePolicyTest.kt` — 5 tests (single/double/long, boundary, pending window)

**Modified:**
- `HomeViewModel.kt` — injected `QuickEntryPreferencesRepository`, extended `HomeUiState`, wired memory save on quick-entry success
- `HomeScreen.kt` — FAB `onDoubleClick` wiring with gesture policy
- `QuickAddRecordSheet.kt` — added amount chips, scene chips, haptic feedback
- `BackupRepository.kt` — added `clearQuickEntryMemory()` call in `clearAllData()`

## Decisions Made

- Used serialized JSON string in DataStore rather than Room schema — keeps phase scope small, no migration needed
- Time slots are coarse (5 buckets) and local-only — not exposed as user-facing feature
- FAB uses remembered coroutine job with 280ms delay rather than `combinedClickable` — cleaner separation of single/double/long press
- Quick-entry memory capped at 24 records to prevent unbounded DataStore growth

## Tech Debt

- No unit tests for `QuickEntryPreferencesRepository` persistence (requires Android context/DataStore instrumentation test)
- `restoreEnvelope()` does not explicitly clear quick-entry memory; relies on policy-level stale ID validation

## Verification

- All policy tests pass (QuickEntryDefaultsPolicyTest, QuickEntryAmountSuggestionsPolicyTest, QuickEntryFabGesturePolicyTest)
- Integration verified by code-level analysis: save → memory → DataStore → Flow → policy → UI defaults cycle complete
- FAB gesture wiring verified: single/double/long press resolved without navigation conflicts

---
*Phase: 08-quick-entry-acceleration*
*Completed: 2026-05-29*
