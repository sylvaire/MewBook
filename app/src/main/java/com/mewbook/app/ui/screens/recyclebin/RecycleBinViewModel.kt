package com.mewbook.app.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DeletedRecord
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
import javax.inject.Inject

data class RecycleBinUiState(
    val deletedRecords: List<DeletedRecord> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val accountsById: Map<Long, Account> = emptyMap(),
    val isLoading: Boolean = true,
    val busyRecordId: Long? = null,
    val message: String? = null,
    val error: String? = null
)

private data class RecycleBinData(
    val deletedRecords: List<DeletedRecord>,
    val categories: List<Category>,
    val accounts: List<Account>
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
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

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

    val uiState: StateFlow<RecycleBinUiState> = combine(
        recycleBinData,
        busyRecordId,
        message,
        error
    ) { data, busyId, currentMessage, currentError ->
        RecycleBinUiState(
            deletedRecords = data.deletedRecords,
            categoriesById = data.categories.associateBy { it.id },
            accountsById = data.accounts.associateBy { it.id },
            isLoading = false,
            busyRecordId = busyId,
            message = currentMessage,
            error = currentError
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
            busyRecordId.update { null }
        }
    }

    fun clearMessage() {
        message.update { null }
    }

    fun clearError() {
        error.update { null }
    }
}
