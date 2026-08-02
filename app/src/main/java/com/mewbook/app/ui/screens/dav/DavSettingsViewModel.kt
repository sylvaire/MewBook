package com.mewbook.app.ui.screens.dav

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mewbook.app.BuildConfig
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConflictStrategy
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.model.DavSyncSuccessDetails
import com.mewbook.app.domain.policy.DavConflictPolicy
import com.mewbook.app.domain.policy.DavImportDecision
import com.mewbook.app.domain.repository.DavAutoBackupStatusRepository
import com.mewbook.app.domain.usecase.dav.ExportDataUseCase
import com.mewbook.app.domain.usecase.dav.GetDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.ImportDataUseCase
import com.mewbook.app.domain.usecase.dav.ListBackupFilesUseCase
import com.mewbook.app.domain.usecase.dav.PreviewImportDataUseCase
import com.mewbook.app.domain.usecase.dav.SaveDavConfigUseCase
import com.mewbook.app.domain.usecase.dav.TestConnectionUseCase
import com.mewbook.app.domain.policy.DavAutoBackupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DavSyncOperationSummary(
    val title: String,
    val status: String,
    val time: LocalDateTime,
    val fileName: String? = null,
    val remoteBackupCount: Int? = null,
    val conflictCount: Int? = null,
    val detail: String? = null,
    val isError: Boolean = false
)

data class DavSettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/MewBook",
    val isEnabled: Boolean = false,
    val lastSyncTime: LocalDateTime? = null,
    val lastSyncDetails: DavSyncSuccessDetails? = null,
    val conflictStrategy: DavConflictStrategy = DavConflictStrategy.MANUAL,
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
    val isRetrying: Boolean = false,
    val lastOperation: DavSyncOperationSummary? = null,
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
    private val davAutoBackupStatusRepository: DavAutoBackupStatusRepository,
    private val davAutoBackupCoordinator: DavAutoBackupCoordinator
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
                        lastSyncDetails = config.lastSyncDetails,
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
        viewModelScope.launch {
            val existing = getDavConfigUseCase.getOnce() ?: return@launch
            saveDavConfigUseCase(existing.copy(isEnabled = enabled))
        }
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
                lastSyncTime = state.lastSyncTime,
                lastSyncDetails = state.lastSyncDetails
            )
            saveDavConfigUseCase(config)
            _uiState.update {
                it.copy(
                    lastOperation = DavSyncOperationSummary(
                        title = "服务器配置",
                        status = "已保存",
                        time = LocalDateTime.now(),
                        detail = "远程路径：${it.remotePath}"
                    ),
                    message = "配置已保存"
                )
            }
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
            val errorMessage = result.exceptionOrNull()?.message
            _uiState.update {
                it.copy(
                    isTesting = false,
                    lastOperation = DavSyncOperationSummary(
                        title = "连接测试",
                        status = if (result.isSuccess) "连接成功" else "连接失败",
                        time = LocalDateTime.now(),
                        detail = if (result.isSuccess) "服务器可访问，认证通过" else errorMessage,
                        isError = result.isFailure
                    ),
                    message = if (result.isSuccess) "连接成功！" else "连接失败: $errorMessage"
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

    fun updateConflictStrategy(strategy: DavConflictStrategy) {
        _uiState.update { it.copy(conflictStrategy = strategy, message = null) }
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

            val result = exportDataUseCase.withDetails(config, requestedFileName)
            val errorMessage = result.exceptionOrNull()?.message
            _uiState.update {
                val details = result.getOrNull()
                it.copy(
                    isExporting = false,
                    exportFileNameInput = "",
                    lastSyncTime = details?.syncedAt ?: it.lastSyncTime,
                    lastSyncDetails = details ?: it.lastSyncDetails,
                    lastOperation = DavSyncOperationSummary(
                        title = "手动导出",
                        status = if (result.isSuccess) "导出成功" else "导出失败",
                        time = LocalDateTime.now(),
                        fileName = details?.fileName ?: requestedFileName?.let(::displayManualFileName),
                        detail = if (result.isSuccess) "已上传到远程路径 ${state.remotePath}" else errorMessage,
                        isError = result.isFailure
                    ),
                    message = if (result.isSuccess) "导出成功！" else "导出失败: $errorMessage"
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
            val backupFiles = result.getOrNull().orEmpty()
            val errorMessage = result.exceptionOrNull()?.message
            _uiState.update {
                it.copy(
                    isLoadingBackupFiles = false,
                    backupFiles = backupFiles,
                    showBackupFilePicker = result.isSuccess,
                    lastOperation = DavSyncOperationSummary(
                        title = "备份列表",
                        status = if (result.isSuccess) "已加载" else "加载失败",
                        time = LocalDateTime.now(),
                        remoteBackupCount = if (result.isSuccess) backupFiles.size else null,
                        detail = if (result.isSuccess) "请选择要恢复的远程备份" else errorMessage,
                        isError = result.isFailure
                    ),
                    message = if (result.isSuccess) null else "备份列表加载失败: $errorMessage"
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
            val preview = result.getOrNull()
            val errorMessage = result.exceptionOrNull()?.message
            _uiState.update {
                it.copy(
                    isPreviewingImport = false,
                    importPreview = preview,
                    lastOperation = DavSyncOperationSummary(
                        title = "导入预览",
                        status = if (result.isSuccess) "预览完成" else "预览失败",
                        time = LocalDateTime.now(),
                        fileName = backupFile.displayName,
                        conflictCount = preview?.conflicts?.totalConflicts,
                        detail = if (result.isSuccess) "请选择冲突处理方式" else errorMessage,
                        isError = result.isFailure
                    ),
                    message = if (result.isSuccess) null else "导入预览失败: $errorMessage"
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
                importDataUseCase.withDetails(config)
            } else {
                importDataUseCase.withDetails(config, selectedBackupFile)
            }
            val errorMessage = result.exceptionOrNull()?.message
            _uiState.update {
                val details = result.getOrNull()
                it.copy(
                    isImporting = false,
                    lastSyncTime = details?.syncedAt ?: it.lastSyncTime,
                    lastSyncDetails = details ?: it.lastSyncDetails,
                    selectedImportBackupFile = if (result.isSuccess) null else selectedBackupFile,
                    lastOperation = DavSyncOperationSummary(
                        title = "手动导入",
                        status = if (result.isSuccess) "导入成功" else "导入失败",
                        time = LocalDateTime.now(),
                        fileName = details?.fileName ?: selectedBackupFile?.displayName,
                        detail = if (result.isSuccess) "本地数据已按远端备份完成恢复" else errorMessage,
                        isError = result.isFailure
                    ),
                    message = if (result.isSuccess) "导入成功！" else "导入失败: $errorMessage"
                )
            }
        }
    }

    fun confirmImportData() {
        val state = _uiState.value
        val preview = state.importPreview
        if (preview == null) {
            importData()
            return
        }

        when (DavConflictPolicy.decide(state.conflictStrategy, preview)) {
            DavImportDecision.IMPORT_REMOTE -> confirmRemoteImport()
            DavImportDecision.KEEP_LOCAL -> keepLocalImport("已按本地优先保留本地数据")
            DavImportDecision.REQUIRE_MANUAL_CHOICE -> {
                _uiState.update { it.copy(message = "请手动选择保留本地或远端覆盖") }
            }
        }
    }

    fun confirmRemoteImport() {
        _uiState.update { it.copy(importPreview = null) }
        importData()
    }

    fun keepLocalImport(message: String = "已保留本地数据") {
        _uiState.update {
            it.copy(
                importPreview = null,
                isPreviewingImport = false,
                selectedImportBackupFile = null,
                message = message
            )
        }
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

    fun retryAutoBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, message = null) }
            davAutoBackupCoordinator.retry()
            _uiState.update {
                it.copy(
                    isRetrying = false,
                    lastOperation = DavSyncOperationSummary(
                        title = "自动备份重试",
                        status = "已触发",
                        time = LocalDateTime.now(),
                        detail = "最新结果会同步到自动备份状态"
                    )
                )
            }
        }
    }

    private fun displayManualFileName(fileName: String): String {
        val normalized = if (fileName.endsWith(".json", ignoreCase = true)) fileName else "$fileName.json"
        return if (normalized.startsWith("manual_", ignoreCase = true)) normalized else "manual_$normalized"
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
