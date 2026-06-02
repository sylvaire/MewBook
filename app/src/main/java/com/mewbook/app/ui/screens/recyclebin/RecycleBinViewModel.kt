package com.mewbook.app.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DeletedRecord
import com.mewbook.app.domain.policy.RecordTrashPolicy
import com.mewbook.app.domain.policy.RecycleBinAmountFilter
import com.mewbook.app.domain.policy.RecycleBinFilterPolicy
import com.mewbook.app.domain.repository.AccountRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.record.DeleteDeletedRecordForeverUseCase
import com.mewbook.app.domain.usecase.record.GetDeletedRecordsUseCase
import com.mewbook.app.domain.usecase.record.PurgeExpiredDeletedRecordsUseCase
import com.mewbook.app.domain.usecase.record.RestoreDeletedRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class RecycleBinUiState(
    val deletedRecords: List<DeletedRecord> = emptyList(),
    val visibleDeletedRecords: List<DeletedRecord> = emptyList(),
    val availableFilterCategories: List<Category> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val accountsById: Map<Long, Account> = emptyMap(),
    val selectedCategoryId: Long? = null,
    val amountFilter: RecycleBinAmountFilter = RecycleBinAmountFilter.ALL,
    val selectedRecordIds: Set<Long> = emptySet(),
    val expiringSoonCount: Int = 0,
    val isLoading: Boolean = true,
    val busyRecordId: Long? = null,
    val isBatchBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class RecycleBinData(
    val deletedRecords: List<DeletedRecord>,
    val categories: List<Category>,
    val accounts: List<Account>
)

private data class RecycleBinFilterState(
    val selectedCategoryId: Long?,
    val amountFilter: RecycleBinAmountFilter,
    val selectedRecordIds: Set<Long>
)

private data class RecycleBinOperationState(
    val busyRecordId: Long?,
    val isBatchBusy: Boolean,
    val message: String?,
    val error: String?
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    getDeletedRecordsUseCase: GetDeletedRecordsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    accountRepository: AccountRepository,
    private val restoreDeletedRecordUseCase: RestoreDeletedRecordUseCase,
    private val deleteDeletedRecordForeverUseCase: DeleteDeletedRecordForeverUseCase,
    private val purgeExpiredDeletedRecordsUseCase: PurgeExpiredDeletedRecordsUseCase
) : ViewModel() {

    private val busyRecordId = MutableStateFlow<Long?>(null)
    private val isBatchBusy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val amountFilter = MutableStateFlow(RecycleBinAmountFilter.ALL)
    private val selectedRecordIds = MutableStateFlow<Set<Long>>(emptySet())

    private val recycleBinData = combine(
        getDeletedRecordsUseCase(),
        getCategoriesUseCase.getAll(),
        accountRepository.getAllAccounts()
    ) { deletedRecords, categories, accounts ->
        RecycleBinData(
            deletedRecords = deletedRecords,
            categories = categories,
            accounts = accounts
        )
    }

    private val filterState = combine(
        selectedCategoryId,
        amountFilter,
        selectedRecordIds
    ) { categoryId, amountBucket, selectedIds ->
        RecycleBinFilterState(
            selectedCategoryId = categoryId,
            amountFilter = amountBucket,
            selectedRecordIds = selectedIds
        )
    }

    private val operationState = combine(
        busyRecordId,
        isBatchBusy,
        message,
        error
    ) { busyId, batchBusy, currentMessage, currentError ->
        RecycleBinOperationState(
            busyRecordId = busyId,
            isBatchBusy = batchBusy,
            message = currentMessage,
            error = currentError
        )
    }

    val uiState: StateFlow<RecycleBinUiState> = combine(
        recycleBinData,
        filterState,
        operationState
    ) { data, filters, operations ->
        val validRecordIds = data.deletedRecords.map { it.record.id }.toSet()
        val visibleRecords = RecycleBinFilterPolicy.filter(
            records = data.deletedRecords,
            categoryId = filters.selectedCategoryId,
            amountFilter = filters.amountFilter
        )
        val usedCategoryIds = data.deletedRecords.map { it.record.categoryId }.toSet()
        val now = LocalDateTime.now()

        RecycleBinUiState(
            deletedRecords = data.deletedRecords,
            visibleDeletedRecords = visibleRecords,
            availableFilterCategories = data.categories
                .filter { it.id in usedCategoryIds }
                .sortedWith(compareBy<Category> { it.type.name }.thenBy { it.sortOrder }.thenBy { it.name }),
            categoriesById = data.categories.associateBy { it.id },
            accountsById = data.accounts.associateBy { it.id },
            selectedCategoryId = filters.selectedCategoryId,
            amountFilter = filters.amountFilter,
            selectedRecordIds = filters.selectedRecordIds.intersect(validRecordIds),
            expiringSoonCount = data.deletedRecords.count { deletedRecord ->
                RecordTrashPolicy.isExpiringSoon(deletedRecord.deletedAt, now)
            },
            isLoading = false,
            busyRecordId = operations.busyRecordId,
            isBatchBusy = operations.isBatchBusy,
            message = operations.message,
            error = operations.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecycleBinUiState()
    )

    init {
        viewModelScope.launch {
            purgeExpiredDeletedRecordsUseCase()
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        selectedCategoryId.update { categoryId }
        selectedRecordIds.update { emptySet() }
    }

    fun setAmountFilter(filter: RecycleBinAmountFilter) {
        amountFilter.update { filter }
        selectedRecordIds.update { emptySet() }
    }

    fun toggleSelection(recordId: Long) {
        selectedRecordIds.update { current ->
            if (recordId in current) {
                current - recordId
            } else {
                current + recordId
            }
        }
    }

    fun selectAllVisible() {
        val visibleIds = uiState.value.visibleDeletedRecords.map { it.record.id }.toSet()
        if (visibleIds.isEmpty()) {
            return
        }
        selectedRecordIds.update { current ->
            if (visibleIds.all { it in current }) {
                current - visibleIds
            } else {
                current + visibleIds
            }
        }
    }

    fun clearSelection() {
        selectedRecordIds.update { emptySet() }
    }

    fun restore(recordId: Long) {
        viewModelScope.launch {
            busyRecordId.update { recordId }
            runCatching { restoreDeletedRecordUseCase(recordId) }
                .onSuccess { restored ->
                    message.update { if (restored) "记录已恢复" else "记录不存在或已过期" }
                }
                .onFailure { throwable ->
                    error.update { throwable.message ?: "恢复记录失败" }
                }
            selectedRecordIds.update { it - recordId }
            busyRecordId.update { null }
        }
    }

    fun deleteForever(recordId: Long) {
        viewModelScope.launch {
            busyRecordId.update { recordId }
            runCatching { deleteDeletedRecordForeverUseCase(recordId) }
                .onSuccess {
                    message.update { "记录已永久删除" }
                }
                .onFailure { throwable ->
                    error.update { throwable.message ?: "永久删除失败" }
                }
            selectedRecordIds.update { it - recordId }
            busyRecordId.update { null }
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val ids = uiState.value.selectedRecordIds.toList()
            if (ids.isEmpty()) {
                return@launch
            }

            isBatchBusy.update { true }
            var restoredCount = 0
            var failedCount = 0
            ids.forEach { recordId ->
                runCatching { restoreDeletedRecordUseCase(recordId) }
                    .onSuccess { restored ->
                        if (restored) {
                            restoredCount += 1
                        } else {
                            failedCount += 1
                        }
                    }
                    .onFailure {
                        failedCount += 1
                    }
            }
            selectedRecordIds.update { emptySet() }
            isBatchBusy.update { false }
            if (failedCount == 0) {
                message.update { "已恢复 $restoredCount 条记录" }
            } else {
                error.update { "已恢复 $restoredCount 条，$failedCount 条失败" }
            }
        }
    }

    fun deleteSelectedForever() {
        viewModelScope.launch {
            val ids = uiState.value.selectedRecordIds.toList()
            if (ids.isEmpty()) {
                return@launch
            }

            isBatchBusy.update { true }
            var deletedCount = 0
            var failedCount = 0
            ids.forEach { recordId ->
                runCatching { deleteDeletedRecordForeverUseCase(recordId) }
                    .onSuccess {
                        deletedCount += 1
                    }
                    .onFailure {
                        failedCount += 1
                    }
            }
            selectedRecordIds.update { emptySet() }
            isBatchBusy.update { false }
            if (failedCount == 0) {
                message.update { "已永久删除 $deletedCount 条记录" }
            } else {
                error.update { "已永久删除 $deletedCount 条，$failedCount 条失败" }
            }
        }
    }

    fun clearMessage() {
        message.update { null }
    }

    fun clearError() {
        error.update { null }
    }
}
