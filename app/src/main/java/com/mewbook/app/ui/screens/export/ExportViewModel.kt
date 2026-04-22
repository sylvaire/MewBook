package com.mewbook.app.ui.screens.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.backup.BackupRecordImportPreview
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.data.backup.BackupSnapshotSummary
import com.mewbook.app.data.repository.BackupRepository
import com.mewbook.app.data.repository.ExportRepository
import com.mewbook.app.domain.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val isBackingUpLocally: Boolean = false,
    val isRestoringLocally: Boolean = false,
    val isPreviewingRestore: Boolean = false,
    val isPreviewingRecordImport: Boolean = false,
    val isImportingRecords: Boolean = false,
    val currentSnapshotSummary: BackupSnapshotSummary? = null,
    val restorePreview: BackupRestorePreview? = null,
    val recordImportPreview: BackupRecordImportPreview? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRecordImportUri: Uri? = null,
    val exportedUri: Uri? = null,
    val error: String? = null,
    val message: String? = null,
    val restoreRefreshToken: Long? = null,
    val exportType: ExportType = ExportType.CSV
)

enum class ExportType {
    CSV, JSON
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val exportRepository: ExportRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSnapshotSummary()
    }

    private fun loadCurrentSnapshotSummary() {
        viewModelScope.launch {
            val result = runCatching { backupRepository.getCurrentSnapshotSummary() }
            result.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(currentSnapshotSummary = summary)
            }
        }
    }

    fun export(type: ExportType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportType = type, error = null, message = null)
            val activeLedgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L

            val result = when (type) {
                ExportType.CSV -> exportRepository.exportToCsv(activeLedgerId)
                ExportType.JSON -> exportRepository.exportToJson()
            }

            result.fold(
                onSuccess = { uri ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedUri = uri
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = e.message ?: "导出失败"
                    )
                }
            )
        }
    }

    fun suggestedBackupFileName(): String {
        return backupRepository.generateBackupFileName()
    }

    fun previewRestoreFromLocal(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPreviewingRestore = true,
                error = null,
                message = null,
                restorePreview = null,
                pendingRestoreUri = uri
            )

            val result = backupRepository.previewRestoreFromUri(uri)
            result.fold(
                onSuccess = { preview ->
                    _uiState.value = _uiState.value.copy(
                        isPreviewingRestore = false,
                        restorePreview = preview
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isPreviewingRestore = false,
                        pendingRestoreUri = null,
                        error = e.message ?: "本地还原预览失败"
                    )
                }
            )
        }
    }

    fun restoreFromLocal(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestoringLocally = true,
                error = null,
                message = null,
                restoreRefreshToken = null,
                restorePreview = null,
                pendingRestoreUri = null
            )

            val result = backupRepository.importFromUri(uri)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isRestoringLocally = false,
                        message = "本地还原成功，正在刷新页面状态",
                        restoreRefreshToken = System.currentTimeMillis()
                    )
                    loadCurrentSnapshotSummary()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRestoringLocally = false,
                        error = e.message ?: "本地还原失败"
                    )
                }
            )
        }
    }

    fun clearExportedUri() {
        _uiState.value = _uiState.value.copy(exportedUri = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearRestorePreview() {
        _uiState.value = _uiState.value.copy(
            restorePreview = null,
            pendingRestoreUri = null,
            isPreviewingRestore = false
        )
    }

    fun clearRecordImportPreview() {
        _uiState.value = _uiState.value.copy(
            recordImportPreview = null,
            pendingRecordImportUri = null,
            isPreviewingRecordImport = false
        )
    }

    fun consumeRestoreRefreshToken() {
        _uiState.value = _uiState.value.copy(restoreRefreshToken = null)
    }

    fun backupToLocal(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUpLocally = true, error = null, message = null)

            val result = backupRepository.exportToUri(uri)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isBackingUpLocally = false,
                        message = "本地备份成功"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isBackingUpLocally = false,
                        error = e.message ?: "本地备份失败"
                    )
                }
            )
        }
    }

    fun previewImportRecords(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPreviewingRecordImport = true,
                error = null,
                message = null,
                recordImportPreview = null,
                pendingRecordImportUri = uri
            )

            val result = backupRepository.previewImportRecordsFromUri(uri)
            result.fold(
                onSuccess = { preview ->
                    _uiState.value = _uiState.value.copy(
                        isPreviewingRecordImport = false,
                        recordImportPreview = preview
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isPreviewingRecordImport = false,
                        pendingRecordImportUri = null,
                        error = e.message ?: "导入预览失败"
                    )
                }
            )
        }
    }

    fun importRecords(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingRecords = true,
                error = null,
                message = null,
                recordImportPreview = null,
                pendingRecordImportUri = null
            )

            val result = backupRepository.importRecordsFromUri(uri)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isImportingRecords = false,
                        message = "导入成功，已合并到当前数据"
                    )
                    loadCurrentSnapshotSummary()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isImportingRecords = false,
                        error = e.message ?: "导入失败"
                    )
                }
            )
        }
    }
}
