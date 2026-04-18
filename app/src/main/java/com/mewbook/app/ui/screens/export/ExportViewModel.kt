package com.mewbook.app.ui.screens.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val exportedUri: Uri? = null,
    val error: String? = null,
    val exportType: ExportType = ExportType.CSV
)

enum class ExportType {
    CSV, JSON
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun export(type: ExportType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportType = type, error = null)
            val activeLedgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L

            val result = when (type) {
                ExportType.CSV -> exportRepository.exportToCsv(activeLedgerId)
                ExportType.JSON -> exportRepository.exportToJson(activeLedgerId)
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

    fun clearExportedUri() {
        _uiState.value = _uiState.value.copy(exportedUri = null)
    }
}
