package com.mewbook.app.ui.screens.dav

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.usecase.dav.ExportDataUseCase
import com.mewbook.app.domain.usecase.dav.GetDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.ImportDataUseCase
import com.mewbook.app.domain.usecase.dav.PreviewImportDataUseCase
import com.mewbook.app.domain.usecase.dav.SaveDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val isPreviewingImport: Boolean = false,
    val isImporting: Boolean = false,
    val importPreview: BackupRestorePreview? = null,
    val message: String? = null
)

@HiltViewModel
class DavSettingsViewModel @Inject constructor(
    private val getDavConfigUseCase: GetDavConfigUseCase,
    private val saveDavConfigUseCase: SaveDavConfigUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val previewImportDataUseCase: PreviewImportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private companion object {
        const val TAG = "DavSettingsVM"
    }

    private val _uiState = MutableStateFlow(DavSettingsUiState())
    val uiState: StateFlow<DavSettingsUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
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
            Log.d(TAG, "testConnection serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
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

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            val state = _uiState.value
            Log.d(TAG, "exportData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = exportDataUseCase(config)
            _uiState.update {
                val syncedAt = if (result.isSuccess) LocalDateTime.now() else it.lastSyncTime
                it.copy(
                    isExporting = false,
                    lastSyncTime = syncedAt,
                    message = if (result.isSuccess) "导出成功！" else "导出失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun previewImportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreviewingImport = true, message = null, importPreview = null) }
            val state = _uiState.value
            Log.d(TAG, "previewImportData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = previewImportDataUseCase(config)
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
            Log.d(TAG, "importData serverUrl=${state.serverUrl} remotePath=${state.remotePath}")
            val config = DavConfig(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                remotePath = state.remotePath
            )

            val result = importDataUseCase(config)
            _uiState.update {
                val syncedAt = if (result.isSuccess) LocalDateTime.now() else it.lastSyncTime
                it.copy(
                    isImporting = false,
                    lastSyncTime = syncedAt,
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
        _uiState.update { it.copy(importPreview = null, isPreviewingImport = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
