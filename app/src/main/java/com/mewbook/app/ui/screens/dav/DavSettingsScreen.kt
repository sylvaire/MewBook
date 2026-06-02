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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.data.backup.BackupSnapshotSummary
import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConflictStrategy
import com.mewbook.app.domain.model.DavSyncDirection
import com.mewbook.app.domain.model.DavSyncSuccessDetails
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.SettingsPageScaffold
import com.mewbook.app.ui.components.SettingsRowCard
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.components.SettingsSurfaceCard
import com.mewbook.app.ui.components.SettingsSwitchRowCard
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        ImportPreviewDialog(
            preview = preview,
            backupFile = uiState.selectedImportBackupFile,
            conflictStrategy = uiState.conflictStrategy,
            onStrategyChange = viewModel::updateConflictStrategy,
            onConfirmByStrategy = viewModel::confirmImportData,
            onRemoteImport = viewModel::confirmRemoteImport,
            onKeepLocal = viewModel::keepLocalImport,
            onDismiss = viewModel::clearImportPreview
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
            SettingsSectionHeader(
                title = "服务器",
                description = "配置 WebDAV 地址和远程路径，保存后即可连接测试或同步。"
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
                description = "查看最近成功详情，选择冲突策略，或手动导入导出。"
            )

            LastSyncDetailsCard(
                details = uiState.lastSyncDetails,
                fallbackTime = uiState.lastSyncTime
            )

            ConflictStrategyCard(
                selectedStrategy = uiState.conflictStrategy,
                onStrategyChange = viewModel::updateConflictStrategy
            )

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

            DavTransferActionsCard(
                exportBusy = uiState.isExporting,
                importBusy = uiState.isPreviewingImport || uiState.isImporting || uiState.isLoadingBackupFiles,
                exportEnabled = !uiState.isExporting && !uiState.isImporting && !uiState.isPreviewingImport,
                importEnabled = !uiState.isExporting &&
                    !uiState.isImporting &&
                    !uiState.isPreviewingImport &&
                    !uiState.isLoadingBackupFiles,
                importLabel = when {
                    uiState.isLoadingBackupFiles -> "加载中"
                    uiState.isPreviewingImport -> "预览中"
                    else -> "从 DAV 导入"
                },
                onExport = { viewModel.showExportFileNameDialog() },
                onImport = { viewModel.previewImportData() }
            )

        }
    }
}

@Composable
private fun DavTransferActionsCard(
    exportBusy: Boolean,
    importBusy: Boolean,
    exportEnabled: Boolean,
    importEnabled: Boolean,
    importLabel: String,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    SettingsSurfaceCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExport,
                enabled = exportEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (exportBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出到 DAV")
            }

            OutlinedButton(
                onClick = onImport,
                enabled = importEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (importBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(importLabel)
            }
        }
    }
}

@Composable
private fun LastSyncDetailsCard(
    details: DavSyncSuccessDetails?,
    fallbackTime: java.time.LocalDateTime?
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm") }
    SettingsSurfaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "最后成功同步",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                details != null -> {
                    SyncDetailRow("时间", details.syncedAt.format(formatter))
                    SyncDetailRow("类型", details.direction.label())
                    SyncDetailRow("文件", details.fileName)
                    SyncDetailRow("大小", formatFileSize(details.fileSizeBytes))
                    SyncDetailRow("耗时", formatDuration(details.durationMillis))
                }

                fallbackTime != null -> {
                    SyncDetailRow("时间", fallbackTime.format(formatter))
                    Text(
                        text = "文件详情会在下一次成功同步后记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        text = "尚未同步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConflictStrategyCard(
    selectedStrategy: DavConflictStrategy,
    onStrategyChange: (DavConflictStrategy) -> Unit
) {
    SettingsSurfaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "冲突策略",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConflictStrategySelector(
                selectedStrategy = selectedStrategy,
                onStrategyChange = onStrategyChange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedStrategy.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConflictStrategySelector(
    selectedStrategy: DavConflictStrategy,
    onStrategyChange: (DavConflictStrategy) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(
            DavConflictStrategy.LOCAL_FIRST,
            DavConflictStrategy.REMOTE_FIRST,
            DavConflictStrategy.MANUAL
        ).forEach { strategy ->
            FilterChip(
                selected = selectedStrategy == strategy,
                onClick = { onStrategyChange(strategy) },
                label = { Text(strategy.label()) }
            )
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: BackupRestorePreview,
    backupFile: DavBackupFile?,
    conflictStrategy: DavConflictStrategy,
    onStrategyChange: (DavConflictStrategy) -> Unit,
    onConfirmByStrategy: () -> Unit,
    onRemoteImport: () -> Unit,
    onKeepLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasOverwriteRisk = preview.changes.modified > 0 ||
        preview.changes.deleted > 0 ||
        preview.conflicts.totalConflicts > 0
    val confirmLabel = when {
        conflictStrategy == DavConflictStrategy.REMOTE_FIRST -> "远端覆盖"
        conflictStrategy == DavConflictStrategy.LOCAL_FIRST && hasOverwriteRisk -> "保留本地"
        conflictStrategy == DavConflictStrategy.MANUAL && hasOverwriteRisk -> "远端覆盖"
        else -> "继续导入"
    }
    val confirmAction = when {
        conflictStrategy == DavConflictStrategy.REMOTE_FIRST -> onRemoteImport
        conflictStrategy == DavConflictStrategy.LOCAL_FIRST && hasOverwriteRisk -> onKeepLocal
        conflictStrategy == DavConflictStrategy.MANUAL && hasOverwriteRisk -> onRemoteImport
        else -> onConfirmByStrategy
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认 DAV 导入") },
        text = {
            Column {
                backupFile?.let {
                    Text(
                        text = "备份文件：${it.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "同步前差异",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewMetric("新增", preview.changes.added, Modifier.weight(1f))
                    PreviewMetric("修改", preview.changes.modified, Modifier.weight(1f))
                    PreviewMetric("删除", preview.changes.deleted, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "当前数据：${preview.current.formatSummary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "远端数据：${preview.incoming.formatSummary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "冲突：${preview.conflicts.totalConflicts} 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (preview.conflicts.totalConflicts > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ConflictStrategySelector(
                    selectedStrategy = conflictStrategy,
                    onStrategyChange = onStrategyChange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conflictStrategy.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirmAction) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Row {
                if (conflictStrategy == DavConflictStrategy.MANUAL && hasOverwriteRisk) {
                    TextButton(onClick = onKeepLocal) {
                        Text("保留本地")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
private fun PreviewMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private fun BackupSnapshotSummary.formatSummary(): String {
    return "记录 $records、分类 $categories、账户 $accounts、预算 $budgets、模板 $templates、账本 $ledgers"
}

private fun DavConflictStrategy.label(): String {
    return when (this) {
        DavConflictStrategy.LOCAL_FIRST -> "本地优先"
        DavConflictStrategy.REMOTE_FIRST -> "远端优先"
        DavConflictStrategy.MANUAL -> "手动选择"
    }
}

private fun DavConflictStrategy.description(): String {
    return when (this) {
        DavConflictStrategy.LOCAL_FIRST -> "有修改或删除风险时保留本地数据。"
        DavConflictStrategy.REMOTE_FIRST -> "确认后用远端备份覆盖本地数据。"
        DavConflictStrategy.MANUAL -> "预览后再选择保留本地或远端覆盖。"
    }
}

private fun DavSyncDirection.label(): String {
    return when (this) {
        DavSyncDirection.EXPORT -> "手动导出"
        DavSyncDirection.IMPORT -> "手动导入"
        DavSyncDirection.AUTO_BACKUP -> "自动备份"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0)
    }
}

private fun formatDuration(durationMillis: Long): String {
    return if (durationMillis < 1000L) {
        "${durationMillis}ms"
    } else {
        String.format(Locale.getDefault(), "%.1fs", durationMillis / 1000.0)
    }
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
