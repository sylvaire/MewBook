package com.mewbook.app.ui.screens.export

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.result.contract.ActivityResultContracts
import com.mewbook.app.data.backup.BackupCategoryImportAction
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.SettingsGroupCard
import com.mewbook.app.ui.components.SettingsGroupRow
import com.mewbook.app.ui.components.SettingsPageScaffold
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.components.SettingsSurfaceCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateToSmartImport: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val restoreContentContract = remember {
        GetContentWithMimeTypes(arrayOf("application/json", "text/json"))
    }
    val importContentContract = remember {
        GetContentWithMimeTypes(
            arrayOf(
                "text/csv",
                "application/csv",
                "application/vnd.ms-excel",
                "text/comma-separated-values",
                "text/plain"
            )
        )
    }
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        Log.d(TAG, "createBackupLauncher result uri=$uri")
        uri?.let(viewModel::backupToLocal)
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = restoreContentContract
    ) { uri ->
        Log.d(TAG, "restoreBackupLauncher result uri=$uri")
        uri?.let(viewModel::previewRestoreFromLocal)
    }
    val importRecordsLauncher = rememberLauncherForActivityResult(
        contract = importContentContract
    ) { uri ->
        Log.d(TAG, "importRecordsLauncher result uri=$uri")
        uri?.let(viewModel::previewImportRecords)
    }

    uiState.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRestorePreview() },
            title = { Text("确认本地还原") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("本地还原会覆盖当前应用中的数据。建议先做一次本地备份。")
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
                        val pendingUri = uiState.pendingRestoreUri
                        viewModel.clearRestorePreview()
                        if (pendingUri != null) {
                            viewModel.restoreFromLocal(pendingUri)
                        }
                    }
                ) {
                    Text("继续还原")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearRestorePreview() }) {
                    Text("取消")
                }
            }
        )
    }

    uiState.recordImportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRecordImportPreview() },
            title = { Text("确认导入记录") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("这会把外部账单记录合并到当前应用，不会清空现有预算、模板和设置。")
                    Text(
                        text = "当前数据：记录 ${preview.current.records}、分类 ${preview.current.categories}、账户 ${preview.current.accounts}、账本 ${preview.current.ledgers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "导入文件：记录 ${preview.incoming.records}、分类 ${preview.incoming.categories}、账户 ${preview.incoming.accounts}、账本 ${preview.incoming.ledgers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        Text(
                            text = "分类映射：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        preview.categoryMappings.take(8).forEach { mapping ->
                            val actionText = when (mapping.action) {
                                BackupCategoryImportAction.REUSE_EXISTING -> "迁移到"
                                BackupCategoryImportAction.CREATE_NEW -> "新建"
                            }
                            Text(
                                text = "${mapping.sourceName} -> $actionText ${mapping.targetName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pendingUri = uiState.pendingRecordImportUri
                        viewModel.clearRecordImportPreview()
                        if (pendingUri != null) {
                            viewModel.importRecords(pendingUri)
                        }
                    }
                ) {
                    Text("开始导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearRecordImportPreview() }) {
                    Text("取消")
                }
            }
        )
    }

    // Handle successful export
    if (uiState.exportedUri != null) {
        val mimeType = if (uiState.exportType == ExportType.CSV) "text/csv" else "application/json"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uiState.exportedUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享导出文件"))
        viewModel.clearExportedUri()
    }

    if (uiState.message != null) {
        androidx.compose.runtime.LaunchedEffect(uiState.message) {
            android.widget.Toast.makeText(context, uiState.message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    if (uiState.restoreRefreshToken != null) {
        androidx.compose.runtime.LaunchedEffect(uiState.restoreRefreshToken) {
            viewModel.consumeRestoreRefreshToken()
            context.findActivity()?.recreate()
        }
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "迁移与备份",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsPageScaffold(paddingValues = paddingValues) {
            uiState.currentSnapshotSummary?.let { summary ->
                SettingsSummaryCard(
                    title = "当前数据概览",
                    subtitle = "记录 ${summary.records}、分类 ${summary.categories}、账户 ${summary.accounts}、预算 ${summary.budgets}、模板 ${summary.templates}、账本 ${summary.ledgers}。${
                        if (summary.hasExistingData) "还原会覆盖当前本地内容。" else "当前没有本地数据。"
                    }",
                    icon = Icons.Filled.CheckCircle
                )
            }

            SettingsSectionHeader(
                title = "导入其他记账 App",
                description = "CSV 走本地解析；字段混乱时用智能导入，导入前都会先预览。"
            )

            ImportActionsCard(
                busy = uiState.isBackingUpLocally ||
                    uiState.isRestoringLocally ||
                    uiState.isPreviewingRestore ||
                    uiState.isPreviewingRecordImport ||
                    uiState.isImportingRecords,
                importBusy = uiState.isPreviewingRecordImport || uiState.isImportingRecords,
                onSmartImport = onNavigateToSmartImport,
                onChooseCsv = {
                    Log.d(TAG, "launch import picker via GetContent")
                    importRecordsLauncher.launch("*/*")
                }
            )

            SettingsSectionHeader(
                title = "本地备份",
                description = "完整保存或恢复应用数据；还原会覆盖本地内容，适合换机前后使用。"
            )

            LocalBackupActionsCard(
                busy = uiState.isBackingUpLocally ||
                    uiState.isRestoringLocally ||
                    uiState.isPreviewingRestore ||
                    uiState.isPreviewingRecordImport ||
                    uiState.isImportingRecords,
                backupBusy = uiState.isBackingUpLocally,
                restoreBusy = uiState.isRestoringLocally,
                onBackup = { createBackupLauncher.launch(viewModel.suggestedBackupFileName()) },
                onRestore = {
                    Log.d(TAG, "launch restore picker via GetContent")
                    restoreBackupLauncher.launch("*/*")
                }
            )

            SettingsSectionHeader(title = "分享导出")

            SettingsGroupCard {
                ExportOptionRow(
                    icon = Icons.Default.TableChart,
                    title = "CSV 格式",
                    description = "逗号分隔值文件，可用 Excel 打开",
                    onClick = { viewModel.export(ExportType.CSV) },
                    isLoading = uiState.isExporting && uiState.exportType == ExportType.CSV
                )

                ExportOptionRow(
                    icon = Icons.Default.Code,
                    title = "JSON 格式",
                    description = "保留完整数据结构，适合迁移和归档",
                    onClick = { viewModel.export(ExportType.JSON) },
                    isLoading = uiState.isExporting && uiState.exportType == ExportType.JSON,
                    showDivider = false
                )
            }

            if (uiState.error != null) {
                SettingsSurfaceCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "操作失败: ${uiState.error}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

        }
    }
}

private const val TAG = "ExportScreen"

private class GetContentWithMimeTypes(
    private val mimeTypes: Array<String>
) : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }
}

@Composable
private fun ButtonLoadingIndicator(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = color,
        strokeWidth = 2.dp
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun ImportActionsCard(
    busy: Boolean,
    importBusy: Boolean,
    onSmartImport: () -> Unit,
    onChooseCsv: () -> Unit
) {
    SettingsSurfaceCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSmartImport,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("智能导入")
            }
            OutlinedButton(
                onClick = onChooseCsv,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (importBusy) {
                    ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入中")
                } else {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择 CSV 文件")
                }
            }
        }
    }
}

@Composable
private fun LocalBackupActionsCard(
    busy: Boolean,
    backupBusy: Boolean,
    restoreBusy: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    SettingsSurfaceCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBackup,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (backupBusy) {
                    ButtonLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("备份中")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("本地备份")
                }
            }

            OutlinedButton(
                onClick = onRestore,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (restoreBusy) {
                    ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("还原中")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("本地还原")
                }
            }
        }
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    showDivider: Boolean = true
) {
    SettingsGroupRow(
        icon = icon,
        title = title,
        subtitle = description,
        onClick = onClick,
        showDivider = showDivider,
        trailing = {
            if (isLoading) {
                ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = "导出",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
