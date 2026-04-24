package com.mewbook.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.BuildConfig
import com.mewbook.app.data.update.AppUpdateRepository
import com.mewbook.app.domain.model.AppUpdateRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val availableRelease: AppUpdateRelease? = null,
    val downloadedApkPath: String? = null,
    val showUpdateDialog: Boolean = false,
    val showInstallDialog: Boolean = false,
    val showInstallPermissionDialog: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean) {
        val currentState = _uiState.value
        if (currentState.isChecking || currentState.isDownloading) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    infoMessage = null,
                    errorMessage = null
                )
            }
            runCatching {
                appUpdateRepository.checkLatestRelease(BuildConfig.VERSION_NAME)
            }.onSuccess { release ->
                _uiState.update {
                    if (release != null) {
                        it.copy(
                            isChecking = false,
                            availableRelease = release,
                            showUpdateDialog = true
                        )
                    } else {
                        it.copy(
                            isChecking = false,
                            infoMessage = if (silent) null else "当前已是最新版本"
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        errorMessage = if (silent) null else (throwable.message ?: "检查更新失败")
                    )
                }
            }
        }
    }

    fun downloadAvailableUpdate() {
        val release = _uiState.value.availableRelease ?: return
        if (_uiState.value.isDownloading) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showUpdateDialog = false,
                    showInstallDialog = false,
                    isDownloading = true,
                    downloadProgressPercent = null,
                    infoMessage = null,
                    errorMessage = null
                )
            }
            runCatching {
                appUpdateRepository.downloadReleaseApk(release) { progress ->
                    _uiState.update { state ->
                        state.copy(downloadProgressPercent = progress.percent)
                    }
                }
            }.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgressPercent = 100,
                        downloadedApkPath = apkFile.absolutePath,
                        showInstallDialog = true
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgressPercent = null,
                        errorMessage = throwable.message ?: "下载更新失败"
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissInstallDialog() {
        _uiState.update { it.copy(showInstallDialog = false) }
    }

    fun showInstallPermissionDialog() {
        _uiState.update {
            it.copy(
                showInstallDialog = false,
                showInstallPermissionDialog = true
            )
        }
    }

    fun dismissInstallPermissionDialog() {
        _uiState.update { it.copy(showInstallPermissionDialog = false) }
    }

    fun showInstallError(message: String) {
        _uiState.update {
            it.copy(
                showInstallDialog = false,
                showInstallPermissionDialog = false,
                errorMessage = message
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}
