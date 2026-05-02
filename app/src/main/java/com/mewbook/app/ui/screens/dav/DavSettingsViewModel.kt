package com.mewbook.app.ui.screens.dav

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mewbook.app.BuildConfig
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.repository.DavAutoBackupStatusRepository
import com.mewbook.app.domain.usecase.dav.ExportDataUseCase
import com.mewbook.app.domain.usecase.dav.GetDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.ImportDataUseCase
import com.mewbook.app.domain.usecase.dav.ListBackupFilesUseCase
import com.mewbook.app.domain.usecase.dav.PreviewImportDataUseCase
import com.mewbook.app.domain.usecase.dav.SaveDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DavSettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/MewBook",
    val isEnabled: Boolean = false,
    val lastSyncTime: LocalDateTime? = null,
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,
    val isExporting: Boolean = false,
    val showExportFileNameDialog: Boolean = false,
    val exportFileNameInput: String = "",
    val isLoadingBackupFiles: Boolean = false,
    val isPreviewingImport: Boolean = false,
    val isImporting: Boolean = false,
    val backupFiles: List<DavBackupFile> = emptyList(),
    val showBackupFilePicker: Boolean = false,
    val selectedImportBackupFile: DavBackupFile? = null,
    val importPreview: BackupRestorePreview? = null,
    val autoBackupStatus: DavAutoBackupStatus = DavAutoBackupStatus(),
    val message: String? = null
)

@HiltViewModel
class DavSettingsViewModel @Inject constructor(
    private val getDavConfigUseCase: GetDavConfigUseCase,
    private val saveDavConfigUseCase: SaveDavConfigUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val listBackupFilesUseCase: ListBackupFilesUseCase,
    private val previewImportDataUseCase: PreviewImportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val davAutoBackupStatusRepository: DavAutoBackupStatusRepository
) : ViewModel() {

    private companion object {
        const val TAG = "DavSettingsVM"
    }

    private val _uiState = MutableStateFlow(DavSettingsUiState())
    val uiState: StateFlow<DavSettingsUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
        observeAutoBackupStatus()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val config = getDavConfigUseCase.getOnce()
            if (config != null) {
                _uiState.update {
                    it.copy(
                        serverUrl = config.serverUrl,
                        username = config.username,
                        password = config.password,
                        remotePath = config.remotePath,
                        isEnabled = config.isEnabled,
                        lastSyncTime = config.lastSyncTime,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, message = null) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, message = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, message = null) }
    }

    fun updateRemotePath(path: String) {
        _uiState.update { it.copy(remotePath = path, message = null) }
    }

    fun updateIsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled, message = null) }
    }

    private fun observeAutoBackupStatus() {
        viewModelScope.launch {
            davAutoBackupStatusRepository.status.collect { status ->
                _uiState.update { it.copy(autoBackupStatus = status) }
            }
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath,
                isEnabled = state.isEnabled,
                lastSyncTime = state.lastSyncTime
            )
            saveDavConfigUseCase(config)
            _uiState.update { it.copy(message = "配置已保存") }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, message = null) }
            val state = _uiState.value
            if (BuildConfig.DEBUG) Log.d(TAG, "testConnection serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = testConnectionUseCase(config)
            _uiState.update {
                it.copy(
                    isTesting = false,
                    message = if (result.isSuccess) "连接成功！" else "连接失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun showExportFileNameDialog() {
        _uiState.update {
            it.copy(
                showExportFileNameDialog = true,
                exportFileNameInput = "",
                message = null
            )
        }
    }

    fun updateExportFileName(fileName: String) {
        _uiState.update { it.copy(exportFileNameInput = fileName, message = null) }
    }

    fun dismissExportFileNameDialog() {
        _uiState.update { it.copy(showExportFileNameDialog = false, exportFileNameInput = "") }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    showExportFileNameDialog = false,
                    message = null
                )
            }
            val state = _uiState.value
            if (BuildConfig.DEBUG) Log.d(TAG, "exportData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )
            val requestedFileName = state.exportFileNameInput.trim().takeIf { it.isNotEmpty() }

            val result = exportDataUseCase(config, requestedFileName)
            _uiState.update {
                val syncedAt = if (result.isSuccess) LocalDateTime.now() else it.lastSyncTime
                it.copy(
                    isExporting = false,
                    exportFileNameInput = "",
                    lastSyncTime = syncedAt,
                    message = if (result.isSuccess) "导出成功！" else "导出失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun previewImportData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingBackupFiles = true,
                    message = null,
                    importPreview = null,
                    selectedImportBackupFile = null,
                    backupFiles = emptyList(),
                    showBackupFilePicker = false
                )
            }
            val state = _uiState.value
            if (BuildConfig.DEBUG) Log.d(TAG, "previewImportData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = listBackupFilesUseCase(config)
            _uiState.update {
                it.copy(
                    isLoadingBackupFiles = false,
                    backupFiles = result.getOrNull().orEmpty(),
                    showBackupFilePicker = result.isSuccess,
                    message = if (result.isSuccess) null else "备份列表加载失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun previewImportData(backupFile: DavBackupFile) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreviewingImport = true,
                    showBackupFilePicker = false,
                    selectedImportBackupFile = backupFile,
                    message = null,
                    importPreview = null
                )
            }
            val state = _uiState.value
            if (BuildConfig.DEBUG) Log.d(TAG, "previewImportData selected=${backupFile.fileUrl}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = previewImportDataUseCase(config, backupFile)
            _uiState.update {
                it.copy(
                    isPreviewingImport = false,
                    importPreview = result.getOrNull(),
                    message = if (result.isSuccess) null else "导入预览失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun importData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            val state = _uiState.value
            if (BuildConfig.DEBUG) Log.d(TAG, "importData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val selectedBackupFile = state.selectedImportBackupFile
            val result = if (selectedBackupFile == null) {
                importDataUseCase(config)
            } else {
                importDataUseCase(config, selectedBackupFile)
            }
            _uiState.update {
                val syncedAt = if (result.isSuccess) LocalDateTime.now() else it.lastSyncTime
                it.copy(
                    isImporting = false,
                    lastSyncTime = syncedAt,
                    selectedImportBackupFile = if (result.isSuccess) null else selectedBackupFile,
                    message = if (result.isSuccess) "导入成功！" else "导入失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun confirmImportData() {
        _uiState.update { it.copy(importPreview = null) }
        importData()
    }

    fun clearImportPreview() {
        _uiState.update {
            it.copy(
                importPreview = null,
                isPreviewingImport = false,
                selectedImportBackupFile = null
            )
        }
    }

    fun dismissBackupFilePicker() {
        _uiState.update { it.copy(showBackupFilePicker = false, isLoadingBackupFiles = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
