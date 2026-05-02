package com.mewbook.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.mewbook.app.data.local.database.MewBookDatabase
import com.mewbook.app.data.preferences.HomePreferencesRepository
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.HomeQuickEntryCategoryPolicy
import com.mewbook.app.domain.policy.HomeRecordSearchPolicy
import com.mewbook.app.domain.policy.HomeRecordOrderingPolicy
import com.mewbook.app.domain.policy.RecentNoteHistory
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.BudgetRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.repository.RecurringTemplateRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.category.InitializeDefaultCategoriesUseCase
import com.mewbook.app.domain.usecase.account.EnsureDefaultAccountForLedgerUseCase
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
import java.time.YearMonth
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
    val editingRecord: Record?,
    val newRecordType: RecordType?,
    val addEntryMode: HomeAddEntryMode,
    val browsingRecord: Record?
)

private data class HomeEditorState(
    val showAddEditSheet: Boolean,
    val editingRecord: Record?,
    val newRecordType: RecordType?,
    val addEntryMode: HomeAddEntryMode,
    val browsingRecord: Record?
)

private data class HomeSearchState(
    val isSearchMode: Boolean,
    val searchQuery: String
)

private data class HomeInteractionState(
    val overlay: HomeOverlayState,
    val search: HomeSearchState,
    val showHomeOverviewCards: Boolean
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
    val calendarMonth: YearMonth = YearMonth.now(),
    val datesWithRecords: Set<LocalDate> = emptySet(),
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddEditSheet: Boolean = false,
    val editingRecord: Record? = null,
    val newRecordType: RecordType? = null,
    val addEntryMode: HomeAddEntryMode = HomeAddEntryMode.FULL,
    val browsingRecord: Record? = null,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val searchResultCount: Int = 0,
    val recentNotesByCategory: Map<Long, List<String>> = emptyMap(),
    val defaultAccountId: Long? = null,
    val showHomeOverviewCards: Boolean = true,
    val quickCategories: List<Category> = emptyList()
)

enum class HomeAddEntryMode {
    FULL,
    QUICK
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecordsUseCase: GetRecordsUseCase,
    private val addRecordUseCase: AddRecordUseCase,
    private val updateRecordUseCase: UpdateRecordUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val initializeDefaultCategoriesUseCase: InitializeDefaultCategoriesUseCase,
    private val initializeDefaultLedgerUseCase: InitializeDefaultLedgerUseCase,
    private val ensureDefaultAccountForLedgerUseCase: EnsureDefaultAccountForLedgerUseCase,
    private val ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringTemplateRepository: RecurringTemplateRepository,
    private val homePreferencesRepository: HomePreferencesRepository,
    private val database: MewBookDatabase
) : ViewModel() {

    private val _selectedPeriodType = MutableStateFlow(BudgetPeriodType.MONTH)
    private val _anchorDate = MutableStateFlow(LocalDate.now())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)
    private val _editingRecord = MutableStateFlow<Record?>(null)
    private val _newRecordType = MutableStateFlow<RecordType?>(null)
    private val _addEntryMode = MutableStateFlow(HomeAddEntryMode.FULL)
    private val _browsingRecord = MutableStateFlow<Record?>(null)
    private val _lastRecordType = MutableStateFlow<RecordType?>(null)
    private val _isSearchMode = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _records = MutableStateFlow<List<Record>>(emptyList())
    private val _totalIncome = MutableStateFlow(0.0)
    private val _totalExpense = MutableStateFlow(0.0)
    private val _totalBudget = MutableStateFlow(0.0)
    private val _budgetRemaining = MutableStateFlow(0.0)
    private val _showHomeOverviewCards = MutableStateFlow(true)
    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    private val _datesWithRecords = MutableStateFlow<Set<LocalDate>>(emptySet())
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
    private val editorState = combine(_showAddEditSheet, _editingRecord, _newRecordType, _addEntryMode, _browsingRecord) { showAddEditSheet, editingRecord, newRecordType, addEntryMode, browsingRecord ->
        HomeEditorState(
            showAddEditSheet = showAddEditSheet,
            editingRecord = editingRecord,
            newRecordType = newRecordType,
            addEntryMode = addEntryMode,
            browsingRecord = browsingRecord
        )
    }
    private val overlayState = combine(_isLoading, _error, editorState) { isLoading, error, editorState ->
        HomeOverlayState(
            isLoading = isLoading,
            error = error,
            showAddEditSheet = editorState.showAddEditSheet,
            editingRecord = editorState.editingRecord,
            newRecordType = editorState.newRecordType,
            addEntryMode = editorState.addEntryMode,
            browsingRecord = editorState.browsingRecord
        )
    }
    private val searchState = combine(_isSearchMode, _searchQuery) { isSearchMode, searchQuery ->
        HomeSearchState(
            isSearchMode = isSearchMode,
            searchQuery = searchQuery
        )
    }
    private val interactionState = combine(overlayState, searchState, _showHomeOverviewCards) { overlay, search, showHomeOverviewCards ->
        HomeInteractionState(
            overlay = overlay,
            search = search,
            showHomeOverviewCards = showHomeOverviewCards
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

    val uiState: StateFlow<HomeUiState> = combine(
        combine(periodState, summaryState, contextState, interactionState) { period, summary, context, interaction ->
            period to Triple(summary, context, interaction)
        },
        _datesWithRecords
    ) { (period, triple), datesWithRecords ->
        val (summary, context, interaction) = triple
        val overlay = interaction.overlay
        val search = interaction.search
        val activeLedgerId = context.activeLedger?.id ?: 1L
        val categoriesById = context.categories.associateBy { it.id }
        val accountsById = context.accounts.associateBy { it.id }
        val ledgerAccounts = context.accounts.filter { it.ledgerId == activeLedgerId }
        val (periodStart, periodEnd) = PeriodDateRange.dateRange(period.selectedPeriodType, period.anchorDate)
        val canGoNext = PeriodDateRange.canGoToNextPeriod(period.selectedPeriodType, period.anchorDate)
        val browsingRecord = overlay.browsingRecord?.let { selected ->
            context.allRecords.firstOrNull { it.id == selected.id }
        }
        val searchResults = HomeRecordSearchPolicy.search(
            query = search.searchQuery,
            activeLedgerId = activeLedgerId,
            records = context.allRecords,
            categoriesById = categoriesById,
            accountsById = accountsById
        )
        val displayedRecords = when {
            search.isSearchMode && search.searchQuery.isBlank() -> emptyList()
            search.isSearchMode -> HomeRecordOrderingPolicy.newestFirst(searchResults)
            else -> HomeRecordOrderingPolicy.newestFirst(summary.records)
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
            newRecordType = overlay.newRecordType,
            addEntryMode = overlay.addEntryMode,
            browsingRecord = browsingRecord,
            isSearchMode = search.isSearchMode,
            searchQuery = search.searchQuery,
            searchResultCount = searchResults.size,
            recentNotesByCategory = RecentNoteHistory.notesByCategory(
                records = context.allRecords,
                ledgerId = activeLedgerId
            ),
            defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(ledgerAccounts),
            showHomeOverviewCards = interaction.showHomeOverviewCards,
            quickCategories = HomeQuickEntryCategoryPolicy.suggest(
                categories = context.categories,
                records = context.allRecords,
                ledgerId = activeLedgerId,
                type = overlay.newRecordType ?: RecordType.EXPENSE
            ),
            calendarMonth = _calendarMonth.value,
            datesWithRecords = datesWithRecords
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    init {
        initializeData()
        restoreSelectedPeriodType()
        restoreHomeOverviewVisibility()
        observePeriodData()
        observeCalendarMonthData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            initializeDefaultLedgerUseCase()
            val ledgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L
            ensureDefaultAccountForLedgerUseCase(ledgerId)
            initializeDefaultCategoriesUseCase()
            recurringTemplateRepository.autoCloseDueTemplates()
        }
    }

    private fun restoreSelectedPeriodType() {
        viewModelScope.launch {
            homePreferencesRepository.selectedHomePeriod.collectLatest { savedPeriodType ->
                _selectedPeriodType.update { savedPeriodType }
            }
        }
    }

    private fun restoreHomeOverviewVisibility() {
        viewModelScope.launch {
            homePreferencesRepository.showHomeOverviewCards.collectLatest { show ->
                _showHomeOverviewCards.update { show }
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

    fun selectAnchorDate(date: LocalDate) {
        _anchorDate.update { date }
    }

    fun setCalendarMonth(month: YearMonth) {
        _calendarMonth.update { month }
    }

    private fun observeCalendarMonthData() {
        viewModelScope.launch {
            combine(_calendarMonth, ledgerRepository.getDefaultLedgerFlow()) { month, ledger ->
                month to (ledger?.id ?: 1L)
            }.collectLatest { (month, ledgerId) ->
                val start = month.atDay(1)
                val end = month.atEndOfMonth()
                getRecordsUseCase.getDatesWithRecords(ledgerId, start, end).collect { dates ->
                    _datesWithRecords.update { dates }
                }
            }
        }
    }

    fun showAddSheet(initialType: RecordType? = null) {
        _browsingRecord.update { null }
        _editingRecord.update { null }
        _newRecordType.update { initialType ?: _lastRecordType.value ?: RecordType.EXPENSE }
        _addEntryMode.update { HomeAddEntryMode.FULL }
        _showAddEditSheet.update { true }
    }

    fun showQuickAddSheet(initialType: RecordType) {
        _browsingRecord.update { null }
        _editingRecord.update { null }
        _newRecordType.update { initialType }
        _addEntryMode.update { HomeAddEntryMode.QUICK }
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
        _browsingRecord.update { null }
        _editingRecord.update { record }
        _newRecordType.update { record.type }
        _addEntryMode.update { HomeAddEntryMode.FULL }
        _showAddEditSheet.update { true }
    }

    fun showRecordDetail(record: Record) {
        _showAddEditSheet.update { false }
        _editingRecord.update { null }
        _newRecordType.update { null }
        _addEntryMode.update { HomeAddEntryMode.FULL }
        _browsingRecord.update { record }
    }

    fun hideRecordDetail() {
        _browsingRecord.update { null }
    }

    fun editRecordFromDetail(record: Record) {
        _browsingRecord.update { null }
        showEditSheet(record)
    }

    fun expandQuickAddSheet() {
        if (_showAddEditSheet.value) {
            _addEntryMode.update { HomeAddEntryMode.FULL }
        }
    }

    fun hideAddEditSheet() {
        _showAddEditSheet.update { false }
        _editingRecord.update { null }
        _newRecordType.update { null }
        _addEntryMode.update { HomeAddEntryMode.FULL }
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
            database.withTransaction {
                val now = LocalDateTime.now()
                val editing = _editingRecord.value
                val activeLedgerId = uiState.value.activeLedger?.id ?: 1L

                if (editing != null) {
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

                    if (accountId != null) {
                        val account = accountRepository.getAccountById(accountId)
                        if (account != null) {
                            val balanceChange = if (type == RecordType.INCOME) amount else -amount
                            accountRepository.updateBalance(accountId, account.balance + balanceChange)
                        }
                    }
                }
            }
            _lastRecordType.update { type }
            hideAddEditSheet()
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            database.withTransaction {
                val record = _editingRecord.value?.takeIf { it.id == id }
                    ?: _browsingRecord.value?.takeIf { it.id == id }
                    ?: uiState.value.records.find { rec -> rec.id == id }
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
            _browsingRecord.update { current -> current?.takeIf { it.id != id } }
        }
    }

    private fun resolveDisplayedBudgetAmount(budgets: List<Budget>): Double {
        return budgets.find { it.categoryId == null }?.amount
            ?: budgets.filter { it.categoryId != null }.sumOf { it.amount }
    }
}
