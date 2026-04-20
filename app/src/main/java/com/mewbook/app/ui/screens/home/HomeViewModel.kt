package com.mewbook.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.preferences.HomePreferencesRepository
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.HomeRecordSearchPolicy
import com.mewbook.app.domain.policy.RecentNoteHistory
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.BudgetRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.category.InitializeDefaultCategoriesUseCase
import com.mewbook.app.domain.usecase.ledger.InitializeDefaultLedgerUseCase
import com.mewbook.app.domain.usecase.record.AddRecordUseCase
import com.mewbook.app.domain.usecase.record.DeleteRecordUseCase
import com.mewbook.app.domain.usecase.record.GetRecordsUseCase
import com.mewbook.app.domain.usecase.record.UpdateRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import com.mewbook.app.util.PeriodDateRange

private data class HomePeriodState(
    val selectedPeriodType: BudgetPeriodType,
    val anchorDate: LocalDate
)

private data class HomeSummaryState(
    val records: List<Record>,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalBudget: Double,
    val budgetRemaining: Double
)

private data class HomeOverlayState(
    val isLoading: Boolean,
    val error: String?,
    val showAddEditSheet: Boolean,
    val editingRecord: Record?
)

private data class HomeSearchState(
    val isSearchMode: Boolean,
    val searchQuery: String
)

private data class HomeContextState(
    val categories: List<Category>,
    val activeLedger: Ledger?,
    val allRecords: List<Record>,
    val accounts: List<Account>
)

data class HomeUiState(
    val records: List<Record> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val activeLedger: Ledger? = null,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetProgress: Float = 0f,
    val selectedPeriodType: BudgetPeriodType = BudgetPeriodType.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddEditSheet: Boolean = false,
    val editingRecord: Record? = null,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val searchResultCount: Int = 0,
    val recentNotesByCategory: Map<Long, List<String>> = emptyMap(),
    val defaultAccountId: Long? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecordsUseCase: GetRecordsUseCase,
    private val addRecordUseCase: AddRecordUseCase,
    private val updateRecordUseCase: UpdateRecordUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val initializeDefaultCategoriesUseCase: InitializeDefaultCategoriesUseCase,
    private val initializeDefaultLedgerUseCase: InitializeDefaultLedgerUseCase,
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val homePreferencesRepository: HomePreferencesRepository
) : ViewModel() {

    private val _selectedPeriodType = MutableStateFlow(BudgetPeriodType.MONTH)
    private val _anchorDate = MutableStateFlow(LocalDate.now())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)
    private val _editingRecord = MutableStateFlow<Record?>(null)
    private val _isSearchMode = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _records = MutableStateFlow<List<Record>>(emptyList())
    private val _totalIncome = MutableStateFlow(0.0)
    private val _totalExpense = MutableStateFlow(0.0)
    private val _totalBudget = MutableStateFlow(0.0)
    private val _budgetRemaining = MutableStateFlow(0.0)
    private val periodState = combine(_selectedPeriodType, _anchorDate) { selectedPeriodType, anchorDate ->
        HomePeriodState(selectedPeriodType = selectedPeriodType, anchorDate = anchorDate)
    }
    private val summaryState = combine(_records, _totalIncome, _totalExpense, _totalBudget, _budgetRemaining) { records, totalIncome, totalExpense, totalBudget, budgetRemaining ->
        HomeSummaryState(
            records = records,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            budgetRemaining = budgetRemaining
        )
    }
    private val overlayState = combine(_isLoading, _error, _showAddEditSheet, _editingRecord) { isLoading, error, showAddEditSheet, editingRecord ->
        HomeOverlayState(
            isLoading = isLoading,
            error = error,
            showAddEditSheet = showAddEditSheet,
            editingRecord = editingRecord
        )
    }
    private val searchState = combine(_isSearchMode, _searchQuery) { isSearchMode, searchQuery ->
        HomeSearchState(
            isSearchMode = isSearchMode,
            searchQuery = searchQuery
        )
    }
    private val contextState = combine(
        getCategoriesUseCase.getAll(),
        ledgerRepository.getDefaultLedgerFlow(),
        getRecordsUseCase.getAll(),
        accountRepository.getAllAccounts()
    ) { categories, activeLedger, allRecords, accounts ->
        HomeContextState(
            categories = categories,
            activeLedger = activeLedger,
            allRecords = allRecords,
            accounts = accounts
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(periodState, summaryState, overlayState, contextState, searchState) { period, summary, overlay, context, search ->
        val activeLedgerId = context.activeLedger?.id ?: 1L
        val categoriesById = context.categories.associateBy { it.id }
        val accountsById = context.accounts.associateBy { it.id }
        val ledgerAccounts = context.accounts.filter { it.ledgerId == activeLedgerId }
        val (periodStart, periodEnd) = PeriodDateRange.dateRange(period.selectedPeriodType, period.anchorDate)
        val canGoNext = PeriodDateRange.canGoToNextPeriod(period.selectedPeriodType, period.anchorDate)
        val searchResults = HomeRecordSearchPolicy.search(
            query = search.searchQuery,
            activeLedgerId = activeLedgerId,
            records = context.allRecords,
            categoriesById = categoriesById,
            accountsById = accountsById
        )
        val displayedRecords = when {
            search.isSearchMode && search.searchQuery.isBlank() -> emptyList()
            search.isSearchMode -> searchResults
            else -> summary.records
        }

        HomeUiState(
            records = displayedRecords,
            categories = categoriesById,
            accounts = ledgerAccounts,
            activeLedger = context.activeLedger,
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            totalBudget = summary.totalBudget,
            budgetRemaining = summary.budgetRemaining,
            budgetProgress = if (summary.totalBudget > 0) (summary.totalExpense / summary.totalBudget).toFloat().coerceIn(0f, 1f) else 0f,
            selectedPeriodType = period.selectedPeriodType,
            anchorDate = period.anchorDate,
            periodLabel = PeriodDateRange.formatPeriodLabel(period.selectedPeriodType, periodStart, periodEnd),
            canGoNext = canGoNext,
            isLoading = overlay.isLoading,
            error = overlay.error,
            showAddEditSheet = overlay.showAddEditSheet,
            editingRecord = overlay.editingRecord,
            isSearchMode = search.isSearchMode,
            searchQuery = search.searchQuery,
            searchResultCount = searchResults.size,
            recentNotesByCategory = RecentNoteHistory.notesByCategory(
                records = context.allRecords,
                ledgerId = activeLedgerId
            ),
            defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(ledgerAccounts)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    init {
        initializeData()
        restoreSelectedPeriodType()
        observePeriodData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            initializeDefaultLedgerUseCase()
            initializeDefaultCategoriesUseCase()
        }
    }

    private fun restoreSelectedPeriodType() {
        viewModelScope.launch {
            homePreferencesRepository.selectedHomePeriod.collectLatest { savedPeriodType ->
                _selectedPeriodType.update { savedPeriodType }
            }
        }
    }

    private fun observePeriodData() {
        viewModelScope.launch {
            combine(_selectedPeriodType, _anchorDate, ledgerRepository.getDefaultLedgerFlow()) { periodType, anchorDate, ledger ->
                Triple(periodType, anchorDate, ledger?.id ?: 1L)
            }.collectLatest { (periodType, anchorDate, ledgerId) ->
                _isLoading.update { true }
                _error.update { null }
                try {
                    val (periodStart, periodEnd) = PeriodDateRange.dateRange(periodType, anchorDate)
                    val periodKey = PeriodDateRange.periodKey(periodType, anchorDate)

                    combine(
                        getRecordsUseCase.getByLedgerAndDateRange(ledgerId, periodStart, periodEnd),
                        budgetRepository.getBudgetsByPeriod(ledgerId, periodType, periodKey)
                    ) { recordList, budgets ->
                        val income = recordList.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
                        val expense = recordList.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
                        val totalBudgetAmount = resolveDisplayedBudgetAmount(budgets)
                        Triple(recordList, income to expense, totalBudgetAmount)
                    }.collectLatest { (recordList, totals, totalBudgetAmount) ->
                        _records.update { recordList }
                        _totalIncome.update { totals.first }
                        _totalExpense.update { totals.second }
                        _totalBudget.update { totalBudgetAmount }
                        _budgetRemaining.update { totalBudgetAmount - totals.second }
                        _isLoading.update { false }
                    }
                } catch (e: Exception) {
                    _error.update { e.message }
                    _isLoading.update { false }
                }
            }
        }
    }

    fun selectPeriodType(periodType: BudgetPeriodType) {
        if (_selectedPeriodType.value == periodType) {
            return
        }
        _selectedPeriodType.update { periodType }
        viewModelScope.launch {
            homePreferencesRepository.setSelectedHomePeriod(periodType)
        }
    }

    fun previousPeriod() {
        val periodType = _selectedPeriodType.value
        _anchorDate.update { current -> PeriodDateRange.shiftAnchor(periodType, current, -1) }
    }

    fun nextPeriod() {
        val periodType = _selectedPeriodType.value
        _anchorDate.update { current ->
            if (PeriodDateRange.canGoToNextPeriod(periodType, current)) {
                PeriodDateRange.shiftAnchor(periodType, current, 1)
            } else {
                current
            }
        }
    }

    fun showAddSheet() {
        _editingRecord.update { null }
        _showAddEditSheet.update { true }
    }

    fun enterSearchMode() {
        _isSearchMode.update { true }
    }

    fun exitSearchMode() {
        _isSearchMode.update { false }
        _searchQuery.update { "" }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

    fun showEditSheet(record: Record) {
        _editingRecord.update { record }
        _showAddEditSheet.update { true }
    }

    fun hideAddEditSheet() {
        _showAddEditSheet.update { false }
        _editingRecord.update { null }
    }

    fun saveRecord(
        amount: Double,
        type: RecordType,
        categoryId: Long,
        note: String?,
        date: LocalDate,
        accountId: Long?
    ) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val editing = _editingRecord.value
            val activeLedgerId = uiState.value.activeLedger?.id ?: 1L

            if (editing != null) {
                // 如果有账户，先还原旧记录的账户余额
                val oldAccountId = editing.accountId
                if (oldAccountId != null) {
                    val oldAccount = accountRepository.getAccountById(oldAccountId)
                    if (oldAccount != null) {
                        val oldBalanceChange = if (editing.type == RecordType.INCOME) -editing.amount else editing.amount
                        accountRepository.updateBalance(oldAccountId, oldAccount.balance + oldBalanceChange)
                    }
                }

                val updatedRecord = editing.copy(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    note = note,
                    date = date,
                    updatedAt = now,
                    accountId = accountId
                )
                updateRecordUseCase(updatedRecord)

                // 如果有新账户，同步新账户余额
                if (accountId != null) {
                    val newAccount = accountRepository.getAccountById(accountId)
                    if (newAccount != null) {
                        val newBalanceChange = if (type == RecordType.INCOME) amount else -amount
                        accountRepository.updateBalance(accountId, newAccount.balance + newBalanceChange)
                    }
                }
            } else {
                val newRecord = Record(
                    id = 0,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    note = note,
                    date = date,
                    createdAt = now,
                    updatedAt = now,
                    syncId = UUID.randomUUID().toString(),
                    ledgerId = activeLedgerId,
                    accountId = accountId
                )
                addRecordUseCase(newRecord)

                // 新增记录，同步账户余额
                if (accountId != null) {
                    val account = accountRepository.getAccountById(accountId)
                    if (account != null) {
                        val balanceChange = if (type == RecordType.INCOME) amount else -amount
                        accountRepository.updateBalance(accountId, account.balance + balanceChange)
                    }
                }
            }
            hideAddEditSheet()
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            // 删除记录前，先还原账户余额
            val record = _editingRecord.value ?: uiState.value.records.find { rec -> rec.id == id }
            if (record?.accountId != null) {
                val accId = record.accountId
                val account = accountRepository.getAccountById(accId)
                if (account != null) {
                    val balanceChange = if (record.type == RecordType.INCOME) -record.amount else record.amount
                    accountRepository.updateBalance(accId, account.balance + balanceChange)
                }
            }
            deleteRecordUseCase(id)
        }
    }

    private fun resolveDisplayedBudgetAmount(budgets: List<Budget>): Double {
        return budgets.find { it.categoryId == null }?.amount
            ?: budgets.filter { it.categoryId != null }.sumOf { it.amount }
    }
}
