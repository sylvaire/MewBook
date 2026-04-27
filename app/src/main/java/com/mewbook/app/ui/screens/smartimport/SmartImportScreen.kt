package com.mewbook.app.ui.screens.smartimport

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.data.backup.BackupCategoryImportAction
import com.mewbook.app.data.backup.BackupRecordImportPreview
import com.mewbook.app.ui.components.MewCompactTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmartImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    val textFileContract = remember {
        GetContentWithMimeTypes(
            arrayOf(
                "text/plain",
                "text/*",
                "text/csv",
                "application/csv",
                "application/json",
                "text/json",
                "application/vnd.ms-excel",
                "application/octet-stream"
            )
        )
    }
    val textFileLauncher = rememberLauncherForActivityResult(textFileContract) { uri ->
        uri?.let(viewModel::selectImportFileFromUri)
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("确认发送到 AI 服务") },
            text = {
                Text(
                    text = "智能导入会把粘贴文本或选择的 TXT/CSV/JSON 文件发送到你配置的 OpenAI 兼容接口，用于转换为喵喵记账格式。选择文件时会通过接口直接上传文件，请确认内容不包含你不希望外发的信息。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPrivacyDialog = false
                        viewModel.convertWithAi()
                    }
                ) {
                    Text("开始转换")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    uiState.recordImportPreview?.let { preview ->
        SmartImportPreviewDialog(
            preview = preview,
            isImporting = uiState.isImporting,
            onDismiss = viewModel::clearPreview,
            onConfirm = viewModel::importPendingEnvelope
        )
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "智能导入",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IntroCard()
            ApiConfigCard(
                uiState = uiState,
                onBaseUrlChange = viewModel::updateBaseUrl,
                onModelChange = viewModel::updateModel,
                onApiKeyChange = viewModel::updateApiKey,
                onSave = viewModel::saveConfig
            )
            InputCard(
                uiState = uiState,
                onInputChange = viewModel::updateInputText,
                onChooseFile = { textFileLauncher.launch("*/*") },
                onConvert = { showPrivacyDialog = true }
            )

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "操作失败：$error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("把杂乱账单变成可预览导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "粘贴账单文本，或选择 TXT/CSV/JSON 文件直接上传转换。AI 只负责转换结构，最终仍会经过本地分类映射、重复检测和确认导入。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApiConfigCard(
    uiState: SmartImportUiState,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("OpenAI 兼容接口", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.model,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型名") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.apiKeyInput,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (uiState.hasSavedApiKey) "API Key（已保存，留空则不修改）" else "API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            Text(
                text = when {
                    !uiState.secureStorageAvailable -> "安全存储不可用，暂时无法保存 API Key"
                    uiState.hasSavedApiKey -> "API Key 已加密保存在本机"
                    else -> "请先保存 API Key 后再开始智能导入"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.secureStorageAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Button(
                onClick = onSave,
                enabled = uiState.secureStorageAvailable && !uiState.isSavingConfig && !uiState.isConverting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSavingConfig) {
                    ButtonLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("保存接口配置")
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    uiState: SmartImportUiState,
    onInputChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    onConvert: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("导入内容", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "粘贴文本会发送文本内容；选择 TXT/CSV/JSON 文件时会直接上传文件，文件和文本二选一。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text("粘贴其他记账 App 导出的文本") },
                supportingText = { Text("${uiState.inputText.length}/100000") }
            )
            uiState.selectedFileName?.let { fileName ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "已选择文件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        uiState.selectedFileDescription?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onChooseFile,
                    enabled = !uiState.isReadingFile && !uiState.isConverting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isReadingFile) {
                        ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("选择文件")
                    }
                }
                Button(
                    onClick = onConvert,
                    enabled = uiState.canStartConvert && uiState.hasSavedApiKey,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isConverting) {
                        ButtonLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("智能转换")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartImportPreviewDialog(
    preview: BackupRecordImportPreview,
    isImporting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = { Text("确认智能导入") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("AI 已转换完成。请确认分类映射和新增数据后再导入。")
                Text(
                    text = "将新增：记录 ${preview.recordsToImport}、分类 ${preview.categoriesToCreate}、账户 ${preview.accountsToCreate}、账本 ${preview.ledgersToCreate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "将跳过重复记录 ${preview.duplicateRecords} 条。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (preview.categoryMappings.isNotEmpty()) {
                    Text("分类映射", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    preview.categoryMappings.forEach { mapping ->
                        val actionText = when (mapping.action) {
                            BackupCategoryImportAction.REUSE_EXISTING -> "迁移到"
                            BackupCategoryImportAction.CREATE_NEW -> "新建"
                        }
                        Text(
                            text = "${mapping.sourceName} -> $actionText ${mapping.targetName}（${mapping.reason}，图标 ${mapping.icon}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isImporting
            ) {
                if (isImporting) {
                    ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("确认导入")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isImporting) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ButtonLoadingIndicator(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = color,
        strokeWidth = 2.dp
    )
}

private class GetContentWithMimeTypes(
    private val mimeTypes: Array<String>
) : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }
}
