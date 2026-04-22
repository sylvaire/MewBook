package com.mewbook.app.ui.screens.dav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mewbook.app.ui.components.MewCompactTopAppBar
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DavSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DavSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportPreview() },
            title = { Text("确认 DAV 导入") },
            text = {
                Column {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WebDAV 服务器配置",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.serverUrl,
                        onValueChange = { viewModel.updateServerUrl(it) },
                        label = { Text("服务器地址") },
                        placeholder = { Text("https://dav.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.remotePath,
                        onValueChange = { viewModel.updateRemotePath(it) },
                        label = { Text("远程路径") },
                        placeholder = { Text("/MewBook") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        Button(
                            onClick = { viewModel.saveConfig() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存配置")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.testConnection() },
                            enabled = !uiState.isTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp).width(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Link, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("测试连接")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "数据同步",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.lastSyncTime != null) {
                        Text(
                            text = "上次同步: ${uiState.lastSyncTime!!.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "尚未同步",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        OutlinedButton(
                            onClick = { viewModel.exportData() },
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

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.previewImportData() },
                            enabled = !uiState.isExporting && !uiState.isImporting && !uiState.isPreviewingImport,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isPreviewingImport || uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp).width(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isPreviewingImport) "预览中" else "从DAV导入")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 配置支持 WebDAV 协议的服务器地址（如 Nextcloud、群晖等）\n" +
                                "2. 输入您的用户名和密码\n" +
                                "3. 设置远程同步路径，默认为 /MewBook\n" +
                                "4. 点击\"导出到DAV\"可将本地数据备份到服务器\n" +
                                "5. 点击\"从DAV导入\"可从服务器恢复数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
