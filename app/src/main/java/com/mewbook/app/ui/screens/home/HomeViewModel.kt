package com.mewbook.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.BudgetRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val records: List<Record> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetProgress: Float = 0f,
    val selectedMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddEditSheet: Boolean = false,
    val editingRecord: Record? = null
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
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)
    private val _editingRecord = MutableStateFlow<Record?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedMonth,
        _isLoading,
        _error,
        _showAddEditSheet,
        _editingRecord,
        getCategoriesUseCase.getAll(),
        accountRepository.getAllAccounts()
    ) { flows ->
        val month = flows[0] as YearMonth
        val isLoading = flows[1] as Boolean
        val error = flows[2] as String?
        val showSheet = flows[3] as Boolean
        val editingRecord = flows[4] as Record?
        val categories = flows[5] as List<Category>
        val accounts = flows[6] as List<Account>

        HomeUiState(
            selectedMonth = month,
            isLoading = isLoading,
            error = error,
            showAddEditSheet = showSheet,
            editingRecord = editingRecord,
            categories = categories.associateBy { it.id },
            accounts = accounts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    private val _records = MutableStateFlow<List<Record>>(emptyList())
    val records: StateFlow<List<Record>> = _records.asStateFlow()

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense.asStateFlow()

    private val _totalBudget = MutableStateFlow(0.0)
    val totalBudget: StateFlow<Double> = _totalBudget.asStateFlow()

    private val _budgetRemaining = MutableStateFlow(0.0)
    val budgetRemaining: StateFlow<Double> = _budgetRemaining.asStateFlow()

    init {
        initializeData()
        loadRecords()
    }

    private fun initializeData() {
        viewModelScope.launch {
            initializeDefaultLedgerUseCase()
            initializeDefaultCategoriesUseCase()
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                val startDate = _selectedMonth.value.atDay(1)
                val endDate = _selectedMonth.value.atEndOfMonth()

                // Load budget info first
                val monthStr = _selectedMonth.value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                val totalBudgetAmount = budgetRepository.getTotalBudgetAmountByMonth(monthStr, 1L)
                _totalBudget.update { totalBudgetAmount }

                getRecordsUseCase(startDate, endDate).collect { recordList ->
                    _records.update { recordList }
                    val income = recordList.filter { it.type == RecordType.INCOME }.sumOf { r -> r.amount }
                    val expense = recordList.filter { it.type == RecordType.EXPENSE }.sumOf { r -> r.amount }
                    _totalIncome.update { income }
                    _totalExpense.update { expense }
                    _budgetRemaining.update { totalBudgetAmount - expense }

                    _isLoading.update { false }
                }
            } catch (e: Exception) {
                _error.update { e.message }
                _isLoading.update { false }
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.update { month }
        loadRecords()
    }

    fun showAddSheet() {
        _editingRecord.update { null }
        _showAddEditSheet.update { true }
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
                    ledgerId = 1L,
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
            val record = _records.value.find { rec -> rec.id == id }
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
}
