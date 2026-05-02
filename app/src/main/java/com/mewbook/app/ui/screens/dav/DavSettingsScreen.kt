package com.mewbook.app.ui.screens.dav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.SettingsPageScaffold
import com.mewbook.app.ui.components.SettingsRowCard
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.components.SettingsSurfaceCard
import com.mewbook.app.ui.components.SettingsSwitchRowCard
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DavSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DavSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog) {
        DavConfigDialog(
            serverUrl = uiState.serverUrl,
            username = uiState.username,
            password = uiState.password,
            remotePath = uiState.remotePath,
            isTesting = uiState.isTesting,
            onServerUrlChange = viewModel::updateServerUrl,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onRemotePathChange = viewModel::updateRemotePath,
            onSave = {
                viewModel.saveConfig()
                showConfigDialog = false
            },
            onTestConnection = { viewModel.testConnection() },
            onDismiss = { showConfigDialog = false }
        )
    }

    if (uiState.showBackupFilePicker) {
        BackupFilePickerDialog(
            backupFiles = uiState.backupFiles,
            onSelect = viewModel::previewImportData,
            onDismiss = viewModel::dismissBackupFilePicker
        )
    }

    if (uiState.showExportFileNameDialog) {
        ExportFileNameDialog(
            fileName = uiState.exportFileNameInput,
            onFileNameChange = viewModel::updateExportFileName,
            onConfirm = viewModel::exportData,
            onDismiss = viewModel::dismissExportFileNameDialog
        )
    }

    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportPreview() },
            title = { Text("确认 DAV 导入") },
            text = {
                Column {
                    uiState.selectedImportBackupFile?.let { backupFile ->
                        Text(
                            text = "备份文件：${backupFile.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("导入会覆盖当前本地数据，建议先执行一次本地备份。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前数据：记录 ${preview.current.records}、分类 ${preview.current.categories}、账户 ${preview.current.accounts}、预算 ${preview.current.budgets}、模板 ${preview.current.templates}、账本 ${preview.current.ledgers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "导入数据：记录 ${preview.incoming.records}、分类 ${preview.incoming.categories}、账户 ${preview.incoming.accounts}、预算 ${preview.incoming.budgets}、模板 ${preview.incoming.templates}、账本 ${preview.incoming.ledgers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "可能冲突：记录 ${preview.conflicts.records}、分类 ${preview.conflicts.categories}、账户 ${preview.conflicts.accounts}、预算 ${preview.conflicts.budgets}、模板 ${preview.conflicts.templates}、账本 ${preview.conflicts.ledgers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmImportData()
                    }
                ) {
                    Text("继续导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearImportPreview() }) {
                    Text("取消")
                }
            }
        )
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "DAV同步设置",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SettingsPageScaffold(paddingValues = paddingValues) {
            SettingsSummaryCard(
                icon = Icons.Filled.CloudUpload,
                title = "云端备份与恢复",
                subtitle = "配置 WebDAV 后，可以手动导出、自选备份导入，也可以每天首次打开 App 自动备份。"
            )

            SettingsSectionHeader(
                title = "服务器",
                description = "先保存配置，再执行连接测试或同步操作。"
            )

            ConfigSummaryCard(
                serverUrl = uiState.serverUrl,
                isConfigured = uiState.serverUrl.isNotBlank(),
                onClick = { showConfigDialog = true }
            )

            if (uiState.serverUrl.startsWith("http://") &&
                !uiState.serverUrl.startsWith("http://localhost")
            ) {
                SettingsSurfaceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = "使用 HTTP 连接，密码将以明文传输。建议改用 HTTPS。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            SettingsSectionHeader(
                title = "同步操作",
                description = "自动备份状态和手动导入导出集中在这里。"
            )

            // Last sync status
            SettingsSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.lastSyncTime != null) {
                            "上次同步: ${uiState.lastSyncTime!!.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))}"
                        } else {
                            "尚未同步"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Auto-backup toggle
            SettingsSwitchRowCard(
                icon = Icons.Filled.CloudSync,
                title = "打开 App 自动备份",
                subtitle = "每天首次进入前台自动上传一次，并只清理旧自动备份",
                checked = uiState.isEnabled,
                onCheckedChange = viewModel::updateIsEnabled
            )

            // Auto-backup status
            SettingsSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AutoBackupStatusBlock(
                        status = uiState.autoBackupStatus,
                        enabled = uiState.isEnabled,
                        isRetrying = uiState.isRetrying,
                        onRetry = viewModel::retryAutoBackup
                    )
                }
            }

            // Export / Import actions
            SettingsSurfaceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showExportFileNameDialog() },
                        enabled = !uiState.isExporting && !uiState.isImporting && !uiState.isPreviewingImport,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出到DAV")
                    }

                    OutlinedButton(
                        onClick = { viewModel.previewImportData() },
                        enabled = !uiState.isExporting &&
                            !uiState.isImporting &&
                            !uiState.isPreviewingImport &&
                            !uiState.isLoadingBackupFiles,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isPreviewingImport || uiState.isImporting || uiState.isLoadingBackupFiles) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when {
                                uiState.isLoadingBackupFiles -> "加载中"
                                uiState.isPreviewingImport -> "预览中"
                                else -> "从DAV导入"
                            }
                        )
                    }
                }
            }

            // Usage instructions
            SettingsSurfaceCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 点击上方配置卡片设置 WebDAV 服务器信息\n" +
                                "2. 支持 Nextcloud、群晖等 WebDAV 服务\n" +
                                "3. 开启\"打开 App 自动备份\"后，每天首次进入前台自动备份\n" +
                                "4. 点击\"导出到DAV\"可自定义文件名，留空使用默认文件名\n" +
                                "5. 点击\"从DAV导入\"可手动选择服务器上的备份恢复数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSummaryCard(
    serverUrl: String,
    isConfigured: Boolean,
    onClick: () -> Unit
) {
    SettingsRowCard(
        icon = Icons.Filled.Settings,
        title = "服务器配置",
        subtitle = if (isConfigured) serverUrl else "未配置",
        onClick = onClick
    )
}

@Composable
private fun DavConfigDialog(
    serverUrl: String,
    username: String,
    password: String,
    remotePath: String,
    isTesting: Boolean,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRemotePathChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 服务器配置") },
        text = {
            Column {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remotePath,
                    onValueChange = onRemotePathChange,
                    label = { Text("远程路径") },
                    placeholder = { Text("/MewBook") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onTestConnection, enabled = !isTesting) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("测试连接")
                    }
                }
                TextButton(onClick = onSave) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ExportFileNameDialog(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出到 DAV") },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                label = { Text("文件名（可选）") },
                placeholder = { Text("留空使用默认文件名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun BackupFilePickerDialog(
    backupFiles: List<DavBackupFile>,
    onSelect: (DavBackupFile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择 DAV 备份") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                backupFiles.forEach { backupFile ->
                    TextButton(
                        onClick = { onSelect(backupFile) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backupFile.displayName,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AutoBackupStatusBlock(
    status: DavAutoBackupStatus,
    enabled: Boolean,
    isRetrying: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm") }
    val enabledText = if (enabled) {
        "自动备份：已开启（每日首次打开 App）"
    } else {
        "自动备份：未开启"
    }
    Text(
        text = enabledText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    status.lastAttemptTime?.let { time ->
        Text(
            text = "最近尝试：${time.format(formatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    status.lastSuccessTime?.let { time ->
        Text(
            text = "最近成功：${time.format(formatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    status.lastMessage?.takeIf { it.isNotBlank() }?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (status.lastMessageIsError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
    if (status.lastMessageIsError) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onRetry,
            enabled = !isRetrying,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            if (isRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.height(14.dp).width(14.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试中…", style = MaterialTheme.typography.bodySmall)
            } else {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.height(14.dp).width(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新备份", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
