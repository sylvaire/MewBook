# Home Record Swipe Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add smooth, single-row left-swipe edit and delete actions to records on the MewBook home screen without changing other `RecordItem` consumers.

**Architecture:** Keep `RecordItem` unchanged and wrap it with a home-only `SwipeableHomeRecordItem`. Because Material 3 `SwipeToDismissBox` in the project's Compose 1.6/Material 3 1.2 stack only models full dismiss anchors, use the standard Compose Foundation `anchoredDraggable` primitive to provide the approved fixed `144.dp` reveal anchor while retaining Material 3 surfaces, colors, semantics, and motion. `HomeRecordList` owns the single expanded record ID and routes edit/delete requests back to `HomeScreen`.

**Tech Stack:** Kotlin, Jetpack Compose Foundation anchored drag, Material 3, Compose UI tests, JUnit 4.

---

## File map

- Create `app/src/main/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicy.kt`: pure single-expanded-row state rules.
- Create `app/src/test/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicyTest.kt`: JVM tests for expansion and stale-record cleanup.
- Create `app/src/main/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItem.kt`: home-only swipe wrapper and clay action buttons.
- Create `app/src/androidTest/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItemTest.kt`: gesture/action/semantics Compose tests.
- Modify `app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt`: list coordination and existing edit/delete flow wiring.

### Task 1: Single-expanded-record policy

**Files:**
- Create: `app/src/main/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicy.kt`
- Test: `app/src/test/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicyTest.kt`

- [ ] **Step 1: Write the failing policy tests**

```kotlin
package com.mewbook.app.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRecordSwipeStatePolicyTest {
    @Test
    fun `opening a record replaces the previously expanded record`() {
        assertEquals(22L, HomeRecordSwipeStatePolicy.open(currentId = 11L, requestedId = 22L))
    }

    @Test
    fun `closing the expanded record clears it`() {
        assertNull(HomeRecordSwipeStatePolicy.close(currentId = 11L, requestedId = 11L))
    }

    @Test
    fun `closing another record preserves the expanded record`() {
        assertEquals(11L, HomeRecordSwipeStatePolicy.close(currentId = 11L, requestedId = 22L))
    }

    @Test
    fun `missing expanded record is removed after list refresh`() {
        assertNull(HomeRecordSwipeStatePolicy.retainVisible(currentId = 11L, visibleIds = setOf(22L)))
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.mewbook.app.ui.screens.home.HomeRecordSwipeStatePolicyTest
```

Expected: compilation fails because `HomeRecordSwipeStatePolicy` does not exist.

- [ ] **Step 3: Implement the minimal pure policy**

```kotlin
package com.mewbook.app.ui.screens.home

internal object HomeRecordSwipeStatePolicy {
    fun open(currentId: Long?, requestedId: Long): Long = requestedId

    fun close(currentId: Long?, requestedId: Long): Long? =
        currentId?.takeUnless { it == requestedId }

    fun retainVisible(currentId: Long?, visibleIds: Set<Long>): Long? =
        currentId?.takeIf(visibleIds::contains)
}
```

- [ ] **Step 4: Run the test and verify GREEN**

Run the Step 2 command. Expected: all four tests pass.

- [ ] **Step 5: Commit the policy slice**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicy.kt app/src/test/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicyTest.kt
git commit -m "test: define home record swipe state policy"
```

### Task 2: Home-only swipe wrapper

**Files:**
- Create: `app/src/main/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItem.kt`
- Create: `app/src/androidTest/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItemTest.kt`

- [ ] **Step 1: Write failing Compose tests**

Create a test fixture with a deterministic expense `Record`, render `SwipeableHomeRecordItem`, and add these tests:

```kotlin
@Test
fun swipeLeft_revealsLabeledActions_withoutInvokingEitherAction() {
    rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }
    rule.onNodeWithText("编辑").assertIsDisplayed()
    rule.onNodeWithText("删除").assertIsDisplayed()
    assertEquals(0, editCalls)
    assertEquals(0, deleteCalls)
}

@Test
fun delete_requiresButtonTap_afterSwipe() {
    rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }
    assertEquals(0, deleteCalls)
    rule.onNodeWithText("删除").performClick()
    assertEquals(1, deleteCalls)
}

@Test
fun editButton_routesTheRecord() {
    rule.onNodeWithTag("home-record-card-1").performTouchInput { swipeLeft() }
    rule.onNodeWithText("编辑").performClick()
    assertEquals(1L, editedRecord?.id)
}
```

The fixture must pass `hapticFeedbackEnabled = false`, use `createComposeRule()`, and supply `isOpen`, `onRequestOpen`, and `onRequestClose` with mutable Compose state so gestures can settle.

- [ ] **Step 2: Run the instrumentation test and verify RED**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mewbook.app.ui.screens.home.SwipeableHomeRecordItemTest
```

Expected: compilation fails because `SwipeableHomeRecordItem` does not exist.

- [ ] **Step 3: Implement the fixed-anchor wrapper**

Implement `SwipeableHomeRecordItem` with this public shape:

```kotlin
@Composable
internal fun SwipeableHomeRecordItem(
    record: Record,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    isOpen: Boolean,
    onRequestOpen: () -> Unit,
    onRequestClose: () -> Unit,
    onClick: () -> Unit,
    onEdit: (Record) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true
)
```

Use these implementation constraints:

```kotlin
private enum class SwipeAnchor { Closed, Open }
private val ActionRevealWidth = 144.dp

val anchors = DraggableAnchors {
    SwipeAnchor.Closed at 0f
    SwipeAnchor.Open at -revealWidthPx
}
```

- Configure `AnchoredDraggableState` with a 50% positional threshold, `125.dp` velocity threshold, and a spring animation with no bounce.
- Apply `anchoredDraggable(..., orientation = Orientation.Horizontal)` to the card layer only; fixed anchors prevent any full-swipe action.
- Synchronize `isOpen` through `LaunchedEffect` and report settled anchor changes through `onRequestOpen`/`onRequestClose`.
- Clip the complete background to `RoundedCornerShape(ClayDesign.CardRadius)`.
- Render two `Surface` buttons in a `144.dp`-wide trailing `Row`, each at least `48.dp`, with `Icons.Filled.Edit` + `Text("编辑")` and `Icons.Filled.DeleteOutline` + `Text("删除")`.
- Derive edit colors from `secondaryContainer/onSecondaryContainer` and delete colors from `errorContainer/onErrorContainer`; retain clay corner radius and subtle elevation.
- Drive action opacity and scale from normalized reveal progress; do not change the content card's measured width.
- Give the translated card `testTag("home-record-card-${record.id}")` and both buttons explicit button roles/content descriptions.
- A closed card click invokes `onClick`; an open card click invokes `onRequestClose` only.
- Button taps close first, perform the existing haptic policy action, then invoke `onEdit(record)` or `onDelete(record.id)`.

- [ ] **Step 4: Run the instrumentation tests and verify GREEN**

Run the Step 2 command. Expected: all three tests pass.

- [ ] **Step 5: Commit the component slice**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItem.kt app/src/androidTest/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItemTest.kt
git commit -m "feat: add clay swipe actions for home records"
```

### Task 3: Wire edit/delete and single-open coordination into HomeScreen

**Files:**
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt`
- Test: `app/src/test/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicyTest.kt`

- [ ] **Step 1: Add a failing stale-state regression test**

```kotlin
@Test
fun `empty list always clears expanded record`() {
    assertNull(HomeRecordSwipeStatePolicy.retainVisible(currentId = 11L, visibleIds = emptySet()))
}
```

- [ ] **Step 2: Run the policy test and verify RED when behavior is not yet wired**

First run the focused policy test to confirm the new case passes at the policy level. Then make the integration compile fail by changing the `HomeRecordList` call to pass the new `onEditRecord` and `onDeleteRecord` named arguments before adding those parameters to its signature.

Run:

```powershell
.\gradlew.bat compileDebugKotlin
```

Expected: compilation fails with unknown `onEditRecord`/`onDeleteRecord` arguments.

- [ ] **Step 3: Implement HomeScreen wiring**

At the `HomeRecordList` call, pass:

```kotlin
onEditRecord = { record -> viewModel.editRecordFromDetail(record) },
onDeleteRecord = { recordId ->
    pendingDeleteRecordId = recordId
    showDeleteConfirmDialog = true
}
```

Extend `HomeRecordList` with both callbacks, remember `expandedRecordId: Long?`, and reconcile it whenever visible record IDs change:

```kotlin
val visibleRecordIds = remember(records) { records.mapTo(mutableSetOf()) { it.id } }
LaunchedEffect(visibleRecordIds) {
    expandedRecordId = HomeRecordSwipeStatePolicy.retainVisible(expandedRecordId, visibleRecordIds)
}
```

Replace the home-only `RecordItem` call with `SwipeableHomeRecordItem`. Use the policy for open/close requests, and clear `expandedRecordId` before forwarding edit, delete, or normal card clicks. Do not modify `RecordItem.kt` or statistics-screen call sites.

- [ ] **Step 4: Run focused tests and compile**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.mewbook.app.ui.screens.home.HomeRecordSwipeStatePolicyTest
.\gradlew.bat compileDebugKotlin
```

Expected: policy tests pass and Kotlin compilation succeeds.

- [ ] **Step 5: Commit integration**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt app/src/test/java/com/mewbook/app/ui/screens/home/HomeRecordSwipeStatePolicyTest.kt
git commit -m "feat: wire home record swipe edit and delete"
```

### Task 4: Verification and polish

**Files:**
- Modify only if verification exposes an issue: `app/src/main/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItem.kt`
- Modify only if verification exposes an issue: `app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt`

- [ ] **Step 1: Run the complete automated verification suite**

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Expected: `BUILD SUCCESSFUL` with no test or lint failures.

- [ ] **Step 2: Run connected UI tests**

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mewbook.app.ui.screens.home.SwipeableHomeRecordItemTest
```

Expected: all swipe action tests pass on the connected emulator/device.

- [ ] **Step 3: Inspect the app on an emulator**

Verify home records in light and dark themes, long/empty notes, large amounts, rapid alternating swipes, diagonal scroll gestures, edit routing, delete cancel, and delete confirmation. Confirm only one row remains open and no row content changes layout while dragging.

- [ ] **Step 4: Review the final diff**

```powershell
git diff --check HEAD~3..HEAD
git status --short
```

Expected: no whitespace errors and no unrelated files in the worktree.

- [ ] **Step 5: Commit verification-only fixes if needed**

If Step 1–3 required source changes, rerun the affected verification commands and commit only those fixes:

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/home/SwipeableHomeRecordItem.kt app/src/main/java/com/mewbook/app/ui/screens/home/HomeScreen.kt
git commit -m "fix: polish home record swipe interactions"
```
