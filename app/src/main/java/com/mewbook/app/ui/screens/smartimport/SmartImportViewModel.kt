package com.mewbook.app.ui.screens.smartimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.data.backup.BackupEnvelope
import com.mewbook.app.data.backup.BackupRecordImportPreview
import com.mewbook.app.data.repository.BackupRepository
import com.mewbook.app.data.smartimport.SmartImportApiPolicy
import com.mewbook.app.data.smartimport.SmartImportConfig
import com.mewbook.app.data.smartimport.SmartImportConfigRepository
import com.mewbook.app.data.smartimport.SmartImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class SmartImportUiState(
    val baseUrl: String = SmartImportConfig.DEFAULT_BASE_URL,
    val model: String = SmartImportConfig.DEFAULT_MODEL,
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
    val secureStorageAvailable: Boolean = true,
    val inputText: String = "",
    val selectedFileName: String? = null,
    val selectedFileDescription: String? = null,
    val isLoadingConfig: Boolean = false,
    val isSavingConfig: Boolean = false,
    val isReadingFile: Boolean = false,
    val isConverting: Boolean = false,
    val isImporting: Boolean = false,
    val recordImportPreview: BackupRecordImportPreview? = null,
    val pendingEnvelope: BackupEnvelope? = null,
    val error: String? = null,
    val message: String? = null
) {
    val hasSelectedFile: Boolean
        get() = selectedFileName != null

    val canStartConvert: Boolean
        get() = !isLoadingConfig &&
            !isSavingConfig &&
            !isReadingFile &&
            !isConverting &&
            !isImporting &&
            (inputText.isNotBlank() || hasSelectedFile)
}

@HiltViewModel
class SmartImportViewModel @Inject constructor(
    private val configRepository: SmartImportConfigRepository,
    private val smartImportRepository: SmartImportRepository,
    private val backupRepository: BackupRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SmartImportUiState(
            baseUrl = savedStateHandle[KEY_BASE_URL] ?: SmartImportConfig.DEFAULT_BASE_URL,
            model = savedStateHandle[KEY_MODEL] ?: SmartImportConfig.DEFAULT_MODEL,
            inputText = savedStateHandle[KEY_INPUT_TEXT] ?: ""
        )
    )
    val uiState: StateFlow<SmartImportUiState> = _uiState.asStateFlow()
    private var selectedImportFile: SelectedImportFile? = null

    init {
        loadConfig()
    }

    fun updateBaseUrl(value: String) {
        savedStateHandle[KEY_BASE_URL] = value
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun updateModel(value: String) {
        savedStateHandle[KEY_MODEL] = value
        _uiState.value = _uiState.value.copy(model = value)
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKeyInput = value)
    }

    fun updateInputText(value: String) {
        selectedImportFile = null
        savedStateHandle[KEY_INPUT_TEXT] = value
        _uiState.value = _uiState.value.copy(
            inputText = value,
            selectedFileName = null,
            selectedFileDescription = null
        )
    }

    fun saveConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isSavingConfig = true, error = null, message = null)
            runCatching {
                configRepository.saveConfig(
                    baseUrl = state.baseUrl,
                    model = state.model,
                    apiKey = state.apiKeyInput
                )
                configRepository.loadConfig()
            }.fold(
                onSuccess = { config ->
                    savedStateHandle[KEY_BASE_URL] = config.baseUrl
                    savedStateHandle[KEY_MODEL] = config.model
                    _uiState.value = _uiState.value.copy(
                        baseUrl = config.baseUrl,
                        model = config.model,
                        apiKeyInput = "",
                        hasSavedApiKey = config.hasApiKey,
                        secureStorageAvailable = config.secureStorageAvailable,
                        isSavingConfig = false,
                        message = "智能导入接口配置已保存"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSavingConfig = false,
                        error = error.message ?: "保存配置失败"
                    )
                }
            )
        }
    }

    fun selectImportFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReadingFile = true, error = null, message = null)
            runCatching {
                readImportFile(uri)
            }.fold(
                onSuccess = { file ->
                    selectedImportFile = file
                    savedStateHandle[KEY_INPUT_TEXT] = ""
                    _uiState.value = _uiState.value.copy(
                        inputText = "",
                        selectedFileName = file.fileName,
                        selectedFileDescription = "${file.mimeType ?: "未知类型"} · ${formatFileSize(file.bytes.size)}",
                        isReadingFile = false,
                        message = "已选择文件，将通过接口直接上传转换"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isReadingFile = false,
                        error = error.message ?: "读取文件失败"
                    )
                }
            )
        }
    }

    fun convertWithAi() {
        viewModelScope.launch {
            val state = _uiState.value
            val file = selectedImportFile
            if (file == null && state.inputText.isBlank()) {
                _uiState.value = state.copy(error = "请输入文本或选择 CSV/JSON 文件")
                return@launch
            }
            _uiState.value = state.copy(
                isConverting = true,
                error = null,
                message = null,
                recordImportPreview = null,
                pendingEnvelope = null
            )

            val conversionResult = if (file != null) {
                smartImportRepository.convertFileToEnvelope(
                    fileName = file.fileName,
                    mimeType = file.mimeType,
                    fileBytes = file.bytes
                )
            } else {
                smartImportRepository.convertTextToEnvelope(state.inputText)
            }

            conversionResult.fold(
                onSuccess = { envelope ->
                    backupRepository.previewImportRecordsFromEnvelope(envelope).fold(
                        onSuccess = { preview ->
                            _uiState.value = _uiState.value.copy(
                                isConverting = false,
                                recordImportPreview = preview,
                                pendingEnvelope = envelope
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isConverting = false,
                                error = error.message ?: "生成导入预览失败"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isConverting = false,
                        error = error.message ?: "智能转换失败"
                    )
                }
            )
        }
    }

    fun importPendingEnvelope() {
        viewModelScope.launch {
            val envelope = _uiState.value.pendingEnvelope ?: return@launch
            _uiState.value = _uiState.value.copy(isImporting = true, error = null, message = null)
            backupRepository.importRecordsFromEnvelope(envelope).fold(
                onSuccess = {
                    selectedImportFile = null
                    savedStateHandle[KEY_INPUT_TEXT] = ""
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        recordImportPreview = null,
                        pendingEnvelope = null,
                        inputText = "",
                        selectedFileName = null,
                        selectedFileDescription = null,
                        message = "智能导入成功，已合并到当前数据"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = error.message ?: "智能导入失败"
                    )
                }
            )
        }
    }

    fun clearPreview() {
        _uiState.value = _uiState.value.copy(recordImportPreview = null, pendingEnvelope = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingConfig = true)
            runCatching { configRepository.loadConfig() }.fold(
                onSuccess = { config ->
                    savedStateHandle[KEY_BASE_URL] = config.baseUrl
                    savedStateHandle[KEY_MODEL] = config.model
                    _uiState.value = _uiState.value.copy(
                        baseUrl = config.baseUrl,
                        model = config.model,
                        hasSavedApiKey = config.hasApiKey,
                        secureStorageAvailable = config.secureStorageAvailable,
                        isLoadingConfig = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        secureStorageAvailable = false,
                        error = error.message ?: "读取智能导入配置失败"
                    )
                }
            )
        }
    }

    private suspend fun readImportFile(uri: Uri): SelectedImportFile = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "smart-import-data.csv"
        val mimeType = context.contentResolver.getType(uri)
        require(SmartImportApiPolicy.isSupportedImportFile(fileName, mimeType)) {
            "智能导入文件仅支持 TXT、CSV 或 JSON 格式"
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
        requireNotNull(bytes) { "无法读取文件" }
        require(bytes.isNotEmpty()) { "文件内容为空" }
        SelectedImportFile(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }

    private fun formatFileSize(byteCount: Int): String {
        if (byteCount < 1024) {
            return "${byteCount}B"
        }
        val kib = byteCount / 1024.0
        if (kib < 1024) {
            return String.format(Locale.US, "%.1fKB", kib)
        }
        return String.format(Locale.US, "%.1fMB", kib / 1024.0)
    }

    private data class SelectedImportFile(
        val fileName: String,
        val mimeType: String?,
        val bytes: ByteArray
    )

    private companion object {
        const val KEY_BASE_URL = "smart_import_base_url"
        const val KEY_MODEL = "smart_import_model"
        const val KEY_INPUT_TEXT = "smart_import_input_text"
    }
}
