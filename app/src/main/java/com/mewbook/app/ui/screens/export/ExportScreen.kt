package com.mewbook.app.ui.screens.export

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(viewModel::backupToLocal)
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingRestoreUri = uri
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("确认本地还原") },
            text = {
                Text("本地还原会覆盖当前应用中的记录、分类、账户、预算、分支、DAV 配置与主题设置。建议先做一次本地备份。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreFromLocal(uri)
                        pendingRestoreUri = null
                    }
                ) {
                    Text("继续还原")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
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
                title = "数据导出",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "导出您的记账数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "选择导出格式，数据将以您选择的格式保存，可用于备份或迁移到其他设备。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        enabled = !uiState.isBackingUpLocally && !uiState.isRestoringLocally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isBackingUpLocally) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("本地备份")
                        }
                    }

                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json")) },
                        enabled = !uiState.isBackingUpLocally && !uiState.isRestoringLocally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isRestoringLocally) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
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
                        text = "导出失败: ${uiState.error}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "导出的数据包含：",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• 本地备份/还原：完整应用数据（记录、分类、账户、预算、分支、DAV配置、主题）\n• 分享导出 CSV：便于表格查看\n• 分享导出 JSON：统一版本化备份快照",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                OutlinedButton(onClick = onClick) {
                    Text("导出")
                }
            }
        }
    }
}
