# Account Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the account module into an overview-first experience with asset/liability grouping, direct-save account editing, and a compact validated add-account flow.

**Architecture:** Keep account persistence and schema unchanged. Add small domain policies for grouping and form validation, keep repository access in the existing ViewModels, and split screen-specific Compose pieces out of the oversized account detail file. Preserve the pending account usage summary and read-only record detail work already present in the working tree.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Kotlin Flow, JUnit 4

---

## File Structure

- Create `app/src/main/java/com/mewbook/app/domain/policy/AccountPresentationPolicy.kt`
  - Pure grouping and liability presentation rules.
- Create `app/src/test/java/com/mewbook/app/domain/policy/AccountPresentationPolicyTest.kt`
  - Locks asset/liability partitioning and absolute liability amounts.
- Modify `app/src/main/java/com/mewbook/app/domain/policy/AccountEditDraftPolicy.kt`
  - Shared name and balance input validation for add and edit flows.
- Modify `app/src/test/java/com/mewbook/app/domain/policy/AccountEditDraftPolicyTest.kt`
  - Covers incomplete balance text and resolved values.
- Modify `app/src/main/java/com/mewbook/app/domain/policy/AccountNamingPolicy.kt`
  - Reuse the existing `excludeAccountId` support without changing its API.
- Modify `app/src/test/java/com/mewbook/app/domain/policy/AccountNamingPolicyTest.kt`
  - Proves edit-time duplicate checks exclude the current account.
- Modify `app/src/main/java/com/mewbook/app/ui/screens/asset/AssetScreen.kt`
  - Render asset and liability sections with explicit liability copy.
- Create `app/src/main/java/com/mewbook/app/ui/screens/asset/AccountDetailComponents.kt`
  - Host the account overview card, edit sheet, usage card, metrics, and recent record rows.
- Modify `app/src/main/java/com/mewbook/app/ui/screens/asset/AccountEditScreen.kt`
  - Keep ViewModel/state orchestration, direct-save behavior, snackbar feedback, menu, and record detail dialog.
- Modify `app/src/main/java/com/mewbook/app/ui/screens/asset/AddAccountScreen.kt`
  - Compact type grid, inline field errors, and single-submit flow.
- Modify `app/src/main/java/com/mewbook/app/ui/screens/home/RecordDetailDialog.kt`
  - Preserve the pending optional edit action used by read-only account record details.
- Keep `app/src/main/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicy.kt`
  - Preserve pending account usage aggregation.
- Keep `app/src/test/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicyTest.kt`
  - Preserve pending usage aggregation coverage.

### Task 1: Lock Account Presentation And Form Rules

**Files:**
- Create: `app/src/main/java/com/mewbook/app/domain/policy/AccountPresentationPolicy.kt`
- Create: `app/src/test/java/com/mewbook/app/domain/policy/AccountPresentationPolicyTest.kt`
- Modify: `app/src/main/java/com/mewbook/app/domain/policy/AccountEditDraftPolicy.kt`
- Modify: `app/src/test/java/com/mewbook/app/domain/policy/AccountEditDraftPolicyTest.kt`
- Modify: `app/src/test/java/com/mewbook/app/domain/policy/AccountNamingPolicyTest.kt`

- [ ] **Step 1: Write failing grouping and liability tests**

```kotlin
class AccountPresentationPolicyTest {
    @Test
    fun groupAccounts_separatesCreditCardsFromAssetAccounts() {
        val cash = account(id = 1L, type = AccountType.CASH, balance = 200.0)
        val bank = account(id = 2L, type = AccountType.BANK, balance = 800.0)
        val creditCard = account(id = 3L, type = AccountType.CREDIT_CARD, balance = -350.0)

        val groups = AccountPresentationPolicy.groupAccounts(listOf(cash, creditCard, bank))

        assertEquals(listOf(cash, bank), groups.assetAccounts)
        assertEquals(listOf(creditCard), groups.liabilityAccounts)
    }

    @Test
    fun liabilityAmount_returnsAbsoluteBalance() {
        assertEquals(350.0, AccountPresentationPolicy.liabilityAmount(-350.0), 0.0)
        assertEquals(350.0, AccountPresentationPolicy.liabilityAmount(350.0), 0.0)
    }
}
```

- [ ] **Step 2: Extend failing form and naming tests**

```kotlin
@Test
fun isBalanceInputComplete_rejectsSignAndDecimalPointOnly() {
    assertFalse(AccountEditDraftPolicy.isBalanceInputComplete(""))
    assertFalse(AccountEditDraftPolicy.isBalanceInputComplete("-"))
    assertFalse(AccountEditDraftPolicy.isBalanceInputComplete("."))
    assertTrue(AccountEditDraftPolicy.isBalanceInputComplete("0"))
    assertTrue(AccountEditDraftPolicy.isBalanceInputComplete("-12.50"))
}

@Test
fun hasDuplicateNameInLedger_excludesEditedAccount() {
    val accounts = listOf(account(id = 1L, name = "工资卡", ledgerId = 1L))

    assertFalse(
        AccountNamingPolicy.hasDuplicateNameInLedger(
            accounts = accounts,
            ledgerId = 1L,
            candidateName = " 工资卡 ",
            excludeAccountId = 1L
        )
    )
}
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.mewbook.app.domain.policy.AccountPresentationPolicyTest --tests com.mewbook.app.domain.policy.AccountEditDraftPolicyTest --tests com.mewbook.app.domain.policy.AccountNamingPolicyTest
```

Expected: FAIL because `AccountPresentationPolicy` and `isBalanceInputComplete` do not exist.

- [ ] **Step 4: Implement minimal pure policies**

```kotlin
data class AccountGroups(
    val assetAccounts: List<Account>,
    val liabilityAccounts: List<Account>
)

object AccountPresentationPolicy {
    fun groupAccounts(accounts: List<Account>): AccountGroups {
        return AccountGroups(
            assetAccounts = accounts.filterNot { it.type == AccountType.CREDIT_CARD },
            liabilityAccounts = accounts.filter { it.type == AccountType.CREDIT_CARD }
        )
    }

    fun liabilityAmount(balance: Double): Double = kotlin.math.abs(balance)
}
```

Add to `AccountEditDraftPolicy`:

```kotlin
fun isBalanceInputComplete(value: String): Boolean {
    return value.toDoubleOrNull() != null && isBalanceInputAllowed(value)
}

fun resolveAddBalance(value: String): Double? {
    if (value.isBlank()) return 0.0
    return value.toDoubleOrNull()?.takeIf { isBalanceInputAllowed(value) }
}
```

- [ ] **Step 5: Run tests and verify GREEN**

Run the Task 1 test command again.

Expected: PASS.

- [ ] **Step 6: Commit the policy layer**

```powershell
git add app/src/main/java/com/mewbook/app/domain/policy/AccountPresentationPolicy.kt app/src/main/java/com/mewbook/app/domain/policy/AccountEditDraftPolicy.kt app/src/test/java/com/mewbook/app/domain/policy/AccountPresentationPolicyTest.kt app/src/test/java/com/mewbook/app/domain/policy/AccountEditDraftPolicyTest.kt app/src/test/java/com/mewbook/app/domain/policy/AccountNamingPolicyTest.kt
git commit -m "Make account grouping and form rules explicit" -m "Confidence: high" -m "Scope-risk: narrow" -m "Tested: Account presentation, edit draft, and naming policy unit tests"
```

### Task 2: Group Asset And Liability Accounts

**Files:**
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/asset/AssetScreen.kt`

- [ ] **Step 1: Derive groups from the existing account list**

Inside `AssetScreen`, compute:

```kotlin
val accountGroups = remember(uiState.accounts) {
    AccountPresentationPolicy.groupAccounts(uiState.accounts)
}
```

- [ ] **Step 2: Replace the single list with conditional sections**

Add a private lazy-list section builder:

```kotlin
private fun LazyListScope.accountSection(
    title: String,
    accounts: List<Account>,
    liability: Boolean,
    onAccountClick: (Account) -> Unit
) {
    if (accounts.isEmpty()) return

    item(key = "header-$title") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${accounts.size} 个",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    items(
        items = accounts,
        key = { account -> account.id },
        contentType = { "account" }
    ) { account ->
        AccountItem(account = account, isLiability = liability, onClick = onAccountClick)
    }
}
```

Render “资产账户” and “负债账户” only when their lists are non-empty.

- [ ] **Step 3: Make liability wording explicit**

Change `AccountItem` to accept `isLiability`. Use:

```kotlin
val amountText = if (isLiability) {
    "欠款 ${formatCurrency(AccountPresentationPolicy.liabilityAmount(account.balance))}"
} else {
    formatCurrency(account.balance)
}
```

Use the expense semantic color only for liability rows. Keep the text label as the primary signal.

- [ ] **Step 4: Build the screen**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit the grouped asset screen**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/asset/AssetScreen.kt
git commit -m "Clarify assets and liabilities at a glance" -m "Confidence: high" -m "Scope-risk: narrow" -m "Tested: Debug assembly"
```

### Task 3: Add Direct-Save Account Editing State

**Files:**
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/asset/AccountEditScreen.kt`

- [ ] **Step 1: Expand the UI state**

Add:

```kotlin
val saveSucceeded: Boolean = false,
val nameError: String? = null,
val balanceError: String? = null,
val saveError: String? = null
```

- [ ] **Step 2: Validate and save in the ViewModel**

Change `saveChanges` to accept text input:

```kotlin
fun saveChanges(name: String, balanceText: String, iconName: String) {
    viewModelScope.launch {
        val account = _uiState.value.account ?: return@launch
        val normalizedName = name.trim()
        val balance = balanceText.toDoubleOrNull()

        if (normalizedName.isBlank()) {
            _uiState.update {
                it.copy(nameError = "账户名称不能为空", balanceError = null, saveError = null)
            }
            return@launch
        }
        if (balance == null || !AccountEditDraftPolicy.isBalanceInputComplete(balanceText)) {
            _uiState.update {
                it.copy(nameError = null, balanceError = "请输入正确的账户余额", saveError = null)
            }
            return@launch
        }

        val duplicate = AccountNamingPolicy.hasDuplicateNameInLedger(
            accounts = accountRepository.getAllAccounts().first(),
            ledgerId = account.ledgerId,
            candidateName = normalizedName,
            excludeAccountId = account.id
        )
        if (duplicate) {
            _uiState.update {
                it.copy(nameError = "账户名称已存在，请修改", balanceError = null, saveError = null)
            }
            return@launch
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                nameError = null,
                balanceError = null,
                saveError = null,
                saveSucceeded = false
            )
        }
        val updated = account.copy(
            name = normalizedName,
            balance = balance,
            icon = normalizeAccountIconName(iconName, account.type)
        )
        runCatching {
            accountRepository.updateAccount(updated)
            updated
        }.onSuccess { persisted ->
            _uiState.update {
                it.copy(account = persisted, isSaving = false, saveSucceeded = true)
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(isSaving = false, saveError = error.message ?: "保存失败")
            }
        }
    }
}
```

Wrap repository update inside `runCatching` rather than only wrapping object creation.

- [ ] **Step 3: Add a feedback acknowledgement**

```kotlin
fun clearSaveFeedback() {
    _uiState.update {
        it.copy(
            saveSucceeded = false,
            nameError = null,
            balanceError = null,
            saveError = null
        )
    }
}
```

- [ ] **Step 4: Compile the ViewModel changes**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit direct-save state**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/asset/AccountEditScreen.kt
git commit -m "Make account edits save as one clear action" -m "Constraint: Keep account type immutable" -m "Confidence: medium" -m "Scope-risk: moderate" -m "Tested: Debug Kotlin compilation" -m "Not-tested: Interactive save failure path"
```

### Task 4: Rebuild Account Detail As Overview-First

**Files:**
- Create: `app/src/main/java/com/mewbook/app/ui/screens/asset/AccountDetailComponents.kt`
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/asset/AccountEditScreen.kt`
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/home/RecordDetailDialog.kt`
- Keep: `app/src/main/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicy.kt`
- Keep: `app/src/test/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicyTest.kt`

- [ ] **Step 1: Move screen-specific components out of the orchestration file**

Move these composables without behavior changes:

```kotlin
AccountOverviewCard
AccountInfoEditSheet
AccountUsageCard
UsageMetric
RecentAccountRecordRow
```

`AccountDetailComponents.kt` stays in `ui/screens/asset` because the components are not shared outside this screen.

- [ ] **Step 2: Replace the edit dialog with a direct-save bottom sheet**

Use `ModalBottomSheet`. Pass:

```kotlin
account: Account
isSaving: Boolean
nameError: String?
balanceError: String?
saveError: String?
onDismiss: () -> Unit
onSave: (name: String, balanceText: String, iconName: String) -> Unit
```

Show `AccountIconPicker` only when `account.type.supportsCustomIcon()`. The save button remains disabled for a blank name or incomplete balance.

- [ ] **Step 3: Make the page overview-first**

In `AccountEditScreen`:

- Change title to `账户详情`.
- Remove the inline icon picker and bottom save button.
- Render only overview card, usage summary, and recent records.
- Open the edit sheet from the overview card.
- Keep the pending read-only `RecordDetailDialog(showEditAction = false)` behavior.

- [ ] **Step 4: Move deletion into the overflow menu**

Use `Icons.Filled.MoreVert`, `DropdownMenu`, and `DropdownMenuItem`. The only menu item is `删除账户`; selecting it opens the existing confirmation dialog.

- [ ] **Step 5: Close the sheet only after successful persistence**

Use:

```kotlin
LaunchedEffect(uiState.saveSucceeded) {
    if (uiState.saveSucceeded) {
        showAccountInfoSheet = false
        snackbarHostState.showSnackbar("账户信息已更新")
        viewModel.clearSaveFeedback()
    }
}
```

Use `MewSnackbarHost` in the Scaffold. Render `nameError` and `balanceError` under their fields, keep the sheet open, and render `saveError` near the save button when persistence fails.

- [ ] **Step 6: Run focused policy tests and assemble**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.mewbook.app.domain.policy.AccountUsageSummaryPolicyTest --tests com.mewbook.app.domain.policy.AccountEditDraftPolicyTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit the overview-first detail**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/asset/AccountEditScreen.kt app/src/main/java/com/mewbook/app/ui/screens/asset/AccountDetailComponents.kt app/src/main/java/com/mewbook/app/ui/screens/home/RecordDetailDialog.kt app/src/main/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicy.kt app/src/test/java/com/mewbook/app/domain/policy/AccountUsageSummaryPolicyTest.kt
git commit -m "Let account details lead with usage instead of forms" -m "Constraint: Recent records remain read-only from account details" -m "Confidence: high" -m "Scope-risk: moderate" -m "Tested: Usage and form policy tests plus debug assembly"
```

### Task 5: Compact And Validate Add Account

**Files:**
- Modify: `app/src/main/java/com/mewbook/app/ui/screens/asset/AddAccountScreen.kt`

- [ ] **Step 1: Split field-specific errors in state**

Replace the single error field with:

```kotlin
val nameError: String? = null,
val balanceError: String? = null,
val generalError: String? = null
```

- [ ] **Step 2: Pass balance text to the ViewModel**

Change `saveAccount` to receive `balanceText: String`. Resolve:

```kotlin
val balance = AccountEditDraftPolicy.resolveAddBalance(balanceText)
if (balance == null) {
    _uiState.update { it.copy(isSaving = false, balanceError = "请输入正确的账户余额") }
    return@launch
}
```

Map blank-name and duplicate-name failures to `nameError`; reserve `generalError` for repository failures.

- [ ] **Step 3: Replace chips with a stable four-column type grid**

Implement a private `AccountTypeGrid` using `AccountType.entries.chunked(4)` and weighted Rows. Each 48dp-minimum cell contains `AccountTypeIconBadge` and `toDisplayName()`. Selection changes color/border without changing dimensions.

- [ ] **Step 4: Fix name suggestions**

Do not inject default text into the field value. Keep:

```kotlin
value = accountName
placeholder = { Text(defaultName.ifBlank { "输入账户名称" }) }
```

The ViewModel continues resolving a blank value to the type default where one exists.

- [ ] **Step 5: Reduce card hierarchy**

Use section labels plus:

- One account type grid.
- One information card containing name and balance.
- One icon picker card only for `OTHER`.
- One full-width `添加账户` button.

Show `nameError` and `balanceError` through each field's `isError` and `supportingText`. Show `generalError` below the submit button.

- [ ] **Step 6: Run policy tests and assemble**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.mewbook.app.domain.policy.AccountEditDraftPolicyTest --tests com.mewbook.app.domain.policy.AccountNamingPolicyTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit the compact add flow**

```powershell
git add app/src/main/java/com/mewbook/app/ui/screens/asset/AddAccountScreen.kt
git commit -m "Make adding an account faster and easier to correct" -m "Confidence: high" -m "Scope-risk: moderate" -m "Tested: Form and naming policy tests plus debug assembly"
```

### Task 6: Verify The Complete Account Experience

**Files:**
- Verify all modified account files.

- [ ] **Step 1: Run the full automated verification**

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug :app:lintDebug
git diff --check
```

Expected: BUILD SUCCESSFUL and no diff-check errors.

- [ ] **Step 2: Install the debug APK on the connected device**

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' devices
& 'D:\Android\Sdk\platform-tools\adb.exe' -s 2bd66eb9 install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: the device is listed and installation returns `Success`.

- [ ] **Step 3: Manually verify the asset list**

Confirm:

- Net asset card renders in light and dark mode.
- Asset and liability sections appear only when non-empty.
- Credit cards show `欠款` with an absolute amount.
- Account rows remain at least 48dp tall and navigate to detail.

- [ ] **Step 4: Manually verify account detail**

Confirm:

- Title is `账户详情`.
- Overview card opens the edit sheet.
- Normal accounts edit name and balance.
- `OTHER` accounts also edit the custom icon.
- Successful save closes the sheet and refreshes the card.
- Invalid or duplicate name keeps the sheet open with an inline error.
- Overflow menu opens delete confirmation.
- Recent records open read-only details.

- [ ] **Step 5: Manually verify add account**

Confirm:

- Type grid does not shift when selection changes.
- Default-name suggestions remain placeholders.
- Blank balance saves as zero.
- Invalid balance and duplicate name show beside their fields.
- Only `OTHER` displays custom icon choices.
- Submit button prevents duplicate taps while saving.

- [ ] **Step 6: Capture screenshots for visual review**

Capture:

- Asset list with both groups.
- Account detail overview.
- Edit sheet for a normal account.
- Edit sheet for an `OTHER` account.
- Add account screen.
- At least one dark-mode screen.

- [ ] **Step 7: Commit any verification-only corrections**

Use a Lore-format commit describing the observed issue, correction, tests, and any remaining manual gaps. Do not commit screenshots or generated build outputs.
