package com.mewbook.app.ui.screens.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.AccountDefaultsPolicy
import com.mewbook.app.domain.policy.RecentNoteHistory
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.record.AddRecordUseCase
import com.mewbook.app.domain.usecase.record.DeleteRecordUseCase
import com.mewbook.app.domain.usecase.record.GetRecordsUseCase
import com.mewbook.app.domain.usecase.record.UpdateRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class CategoryExpenseDetailUiState(
    val categoryName: String = "",
    val categoryIcon: String = "more_horiz",
    val categoryColor: Long = 0xFF808080,
    val periodSubtitle: String = "",
    val records: List<Record> = emptyList(),
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true,
    val browsingRecord: Record? = null,
    val editingRecord: Record? = null,
    val showAddEditSheet: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recentNotesByCategory: Map<Long, List<String>> = emptyMap(),
    val defaultAccountId: Long? = null
)

private data class DetailBaseState(
    val records: List<Record>,
    val categories: List<Category>,
    val activeLedger: Ledger?,
    val accounts: List<Account>
)

@HiltViewModel
class CategoryExpenseDetailViewModel @Inject constructor(
    private val getRecordsUseCase: GetRecordsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    ledgerRepository: LedgerRepository,
    private val accountRepository: AccountRepository,
    private val addRecordUseCase: AddRecordUseCase,
    private val updateRecordUseCase: UpdateRecordUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<Long>("categoryId")
        ?: error("categoryId required")
    private val periodStart: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("startEpoch") ?: error("startEpoch required")
    )
    private val periodEnd: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("endEpoch") ?: error("endEpoch required")
    )

    private val _browsingRecord = MutableStateFlow<Record?>(null)
    private val _editingRecord = MutableStateFlow<Record?>(null)
    private val _showAddEditSheet = MutableStateFlow(false)

    private val baseState: StateFlow<DetailBaseState> = combine(
        getRecordsUseCase.getExpenseByCategoryAndDateRange(categoryId, periodStart, periodEnd),
        getCategoriesUseCase.getAll(),
        ledgerRepository.getDefaultLedgerFlow(),
        accountRepository.getAllAccounts()
    ) { records, categories, activeLedger, accounts ->
        DetailBaseState(records, categories, activeLedger, accounts)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DetailBaseState(emptyList(), emptyList(), null, emptyList())
    )

    val uiState: StateFlow<CategoryExpenseDetailUiState> = combine(
        baseState,
        _browsingRecord,
        _editingRecord,
        _showAddEditSheet
    ) { base, browsingRecord, editingRecord, showAddEditSheet ->
        val cat = base.categories.find { it.id == categoryId }
        val name = cat?.name ?: "未知"
        val subtitle = formatPeriodSubtitle(periodStart, periodEnd)
        val activeLedgerId = base.activeLedger?.id ?: 1L
        val filteredRecords = base.records.filter { it.ledgerId == activeLedgerId }
        val ledgerAccounts = base.accounts.filter { it.ledgerId == activeLedgerId }
        CategoryExpenseDetailUiState(
            categoryName = name,
            categoryIcon = cat?.icon ?: "more_horiz",
            categoryColor = cat?.color ?: 0xFF808080,
            periodSubtitle = subtitle,
            records = filteredRecords,
            totalExpense = filteredRecords.sumOf { it.amount },
            isLoading = false,
            browsingRecord = browsingRecord,
            editingRecord = editingRecord,
            showAddEditSheet = showAddEditSheet,
            accounts = base.accounts,
            categories = base.categories,
            recentNotesByCategory = RecentNoteHistory.notesByCategory(filteredRecords, activeLedgerId),
            defaultAccountId = AccountDefaultsPolicy.resolveDefaultAccountId(ledgerAccounts)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CategoryExpenseDetailUiState(isLoading = true)
    )

    fun showRecordDetail(record: Record) {
        _showAddEditSheet.update { false }
        _editingRecord.update { null }
        _browsingRecord.update { record }
    }

    fun hideRecordDetail() {
        _browsingRecord.update { null }
    }

    fun editRecordFromDetail(record: Record) {
        _browsingRecord.update { null }
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
            val activeLedgerId = uiState.value.records.firstOrNull()?.ledgerId ?: 1L

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
            hideAddEditSheet()
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
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
            _browsingRecord.update { current -> current?.takeIf { it.id != id } }
        }
    }

    private fun formatPeriodSubtitle(start: LocalDate, end: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        return if (start == end) {
            start.format(fmt)
        } else {
            "${start.format(fmt)} — ${end.format(fmt)}"
        }
    }
}
