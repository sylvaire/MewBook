package com.mewbook.app.ui.screens.export

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.result.contract.ActivityResultContracts
import com.mewbook.app.ui.components.MewCompactTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "迁移、备份与还原",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "这里集中管理本地备份、格式导出与数据还原。每次还原前都会先展示数据预览和冲突提示。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.currentSnapshotSummary?.let { summary ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "当前数据概览",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "记录 ${summary.records}、分类 ${summary.categories}、账户 ${summary.accounts}、预算 ${summary.budgets}、模板 ${summary.templates}、账本 ${summary.ledgers}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (summary.hasExistingData) {
                                "已有数据，执行还原会覆盖当前本地内容。"
                            } else {
                                "当前没有本地数据，可以直接开始导入或备份。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "导入其他记账 App",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "支持导入常见 CSV 导出列，例如日期、类型、分类、子分类、金额、备注和账户。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = {
                            Log.d(TAG, "launch import picker via GetContent")
                            importRecordsLauncher.launch("*/*")
                        },
                        enabled = !uiState.isBackingUpLocally &&
                            !uiState.isRestoringLocally &&
                            !uiState.isPreviewingRestore &&
                            !uiState.isPreviewingRecordImport &&
                            !uiState.isImportingRecords
                    ) {
                        if (uiState.isPreviewingRecordImport || uiState.isImportingRecords) {
                            ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("选择 CSV 文件")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "本地备份",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { createBackupLauncher.launch(viewModel.suggestedBackupFileName()) },
                        enabled = !uiState.isBackingUpLocally &&
                            !uiState.isRestoringLocally &&
                            !uiState.isPreviewingRestore &&
                            !uiState.isPreviewingRecordImport &&
                            !uiState.isImportingRecords,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isBackingUpLocally) {
                            ButtonLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("本地备份")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Log.d(TAG, "launch restore picker via GetContent")
                            restoreBackupLauncher.launch("*/*")
                        },
                        enabled = !uiState.isBackingUpLocally &&
                            !uiState.isRestoringLocally &&
                            !uiState.isPreviewingRestore &&
                            !uiState.isPreviewingRecordImport &&
                            !uiState.isImportingRecords,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isRestoringLocally) {
                            ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("本地还原")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "分享导出",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            ExportOptionCard(
                icon = Icons.Default.TableChart,
                title = "CSV 格式",
                description = "导出为逗号分隔值文件，可用 Excel 打开",
                onClick = { viewModel.export(ExportType.CSV) },
                isLoading = uiState.isExporting && uiState.exportType == ExportType.CSV
            )

            ExportOptionCard(
                icon = Icons.Default.Code,
                title = "JSON 格式",
                description = "导出为 JSON 格式，保留完整数据结构",
                onClick = { viewModel.export(ExportType.JSON) },
                isLoading = uiState.isExporting && uiState.exportType == ExportType.JSON
            )

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "操作失败: ${uiState.error}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "迁移说明：",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• 导入其他记账 App：读取常见 CSV 导出列并先预览，再按去重策略合并到当前账本\n• 本地备份/还原：完整应用数据（记录、分类、账户、预算、周期模板、分支、DAV配置、主题）\n• 分享导出 CSV：便于表格查看和迁移到其他工具\n• 分享导出 JSON：统一版本化备份快照，适合跨设备恢复",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isLoading) {
                ButtonLoadingIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(
                    onClick = onClick,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Text("导出", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
