---
phase: 09-budget-alerts
plan: 01
subsystem: domain-ui
tags: [compose, policy, budget, alerts, prediction]

# Dependency graph
requires:
  - 08-quick-entry-acceleration
provides:
  - BudgetAlertPolicy for pure budget alert calculation (threshold, projection, anomaly)
  - BudgetViewModel with budgetAlerts state from current + previous period data
  - BudgetScreen inline BudgetAlertsCard with color-coded alert display
affects: [budget]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Policy pattern: BudgetAlertPolicy is pure Kotlin with no Android/Compose dependencies, returns List<BudgetAlert>"
    - "Alert levels: BudgetAlertLevel.INFO (primary), CAUTION (warning), DANGER (danger) with color-coded left border"
    - "Graceful degradation: all sub-methods return null on insufficient data; UI hides card when alerts empty"
    - "Max alerts cap: BudgetScreen renders at most 4 high-priority alerts, sorted by severity"

key-files:
  created:
    - app/src/main/java/com/mewbook/app/domain/policy/BudgetAlertPolicy.kt
    - app/src/test/java/com/mewbook/app/domain/policy/BudgetAlertPolicyTest.kt
  modified:
    - app/src/main/java/com/mewbook/app/ui/screens/budget/BudgetViewModel.kt
    - app/src/main/java/com/mewbook/app/ui/screens/budget/BudgetScreen.kt

key-decisions:
  - "Budget alerts are inline on the budget page; no new Room tables, system notifications, or persistent read state"
  - "Threshold alerts at 50%, 80%, 100% via single pure policy method"
  - "Month-end projection only for MONTH period type; based on daily average × total days"
  - "Anomaly detection requires both current and previous period data; 1.5x ratio threshold; excluded for DAY period"
  - "UI shows max 4 alerts sorted by severity (DANGER > CAUTION > INFO)"

patterns-established:
  - "BudgetAlertPolicy: pure Kotlin calculator returning List<BudgetAlert> with level, title, message, and category reference"
  - "BudgetViewModel alert pipeline: collect current + previous period records → compute previousCategorySpending → call BudgetAlertPolicy.createAlerts() → store in BudgetUiState.budgetAlerts"
  - "Inline alert card: BudgetAlertsCard renders only when alerts non-empty; color-coded left border by BudgetAlertLevel"

requirements-completed: [BWA-01, BWA-02, BWA-03, BWA-04, BWA-05]

# Metrics
completed: 2026-05-29
---

# Phase 9 Plan 1: 预算预警系统 Summary

**Inline budget alerts with threshold warnings, month-end overspending prediction, and category anomaly detection**

## Accomplishments

- Created `BudgetAlertPolicy` as pure Kotlin policy class with no Android/Compose dependencies
- Implemented threshold alerts at 50%, 80%, 100% usage rates with color-coded severity levels
- Implemented month-end projection alert based on daily average spending × remaining days (MONTH period only)
- Implemented category anomaly detection comparing current vs previous period spending (1.5x ratio threshold)
- Updated `BudgetViewModel` to collect current + previous period records and generate `BudgetUiState.budgetAlerts`
- Updated `BudgetScreen` with inline `BudgetAlertsCard` that renders only when alerts are non-empty
- All data-insufficient scenarios gracefully degrade to no alerts (null returns → empty list → card hidden)

## Task Commits

1. **Task 1: Add BudgetAlertPolicyTest red tests** — TDD red phase
2. **Task 2: Implement BudgetAlertPolicy** — pure calculator
3. **Task 3: Wire alerts into BudgetViewModel** — ViewModel integration
4. **Task 4: Add BudgetAlertsCard to BudgetScreen** — UI rendering
5. **Task 5: Run verification** — tests, build, lint

## Files Created/Modified

**Created:**
- `BudgetAlertPolicy.kt` — `createAlerts()` with threshold, projection, and anomaly sub-methods; `BudgetAlert` data class with `BudgetAlertLevel` enum
- `BudgetAlertPolicyTest.kt` — 4 tests covering threshold states, month-end projection, anomaly detection, and graceful degradation

**Modified:**
- `BudgetViewModel.kt` — added previous period data collection, `previousCategorySpending` computation, `BudgetAlertPolicy.createAlerts()` call, `budgetAlerts` in `BudgetUiState`
- `BudgetScreen.kt` — added `BudgetAlertsCard` composable with color-coded left border, conditional rendering when alerts non-empty, max 4 alerts displayed

## Decisions Made

- Alerts are inline on the budget page — no new Room tables, system notifications, or persistent read state
- Month-end projection only applies to MONTH period type with `today` within the period
- Anomaly detection requires both current and previous period valid data; excluded for DAY period to avoid noise
- UI sorts alerts by severity (DANGER > CAUTION > INFO) and caps at 4 to avoid overwhelming the user
- Anomaly ratio threshold (1.5x) is hardcoded for v1.2.0; could be made configurable in future

## Tech Debt

- Anomaly detection ratio threshold (1.5x) is hardcoded; could be made configurable per user preference
- maxAlerts cap (4) is hardcoded in BudgetScreen; could be driven by a policy constant

## Verification

- BudgetAlertPolicyTest passes all 4 test cases
- Integration verified by code-level analysis: data collection → policy → ViewModel → inline card rendering cycle complete
- Graceful degradation verified: null budgets, zero amounts, missing history, out-of-period dates all produce no alerts

---
*Phase: 09-budget-alerts*
*Completed: 2026-05-29*
