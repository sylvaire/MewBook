package com.mewbook.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.BuildConfig
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.ui.components.BudgetPeriodTypeSelector
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.SettingsDangerRowCard
import com.mewbook.app.ui.components.SettingsPageScaffold
import com.mewbook.app.ui.components.SettingsRowCard
import com.mewbook.app.ui.components.SettingsSectionHeader
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.components.SettingsSurfaceCard
import com.mewbook.app.ui.components.SettingsSwitchRowCard
import com.mewbook.app.ui.components.displayLabel
import com.mewbook.app.ui.update.AppUpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToDavSettings: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToRecurringTemplates: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToLedgerManagement: () -> Unit,
    updateUiState: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val showHomeOverviewCards by viewModel.showHomeOverviewCards.collectAsStateWithLifecycle()
    val selectedHomePeriod by viewModel.selectedHomePeriod.collectAsStateWithLifecycle()
    val updateEnabled by viewModel.updateEnabled.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearDataConfirmText by remember { mutableStateOf("") }

    if (showThemeDialog) {
        ThemeModeDialog(
            selectedThemeMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    if (showClearDataDialog) {
        val confirmed = clearDataConfirmText == "确认删除"

        AlertDialog(
            onDismissRequest = {
                showClearDataDialog = false
                clearDataConfirmText = ""
            },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("清除所有数据") },
            text = {
                Column {
                    Text("此操作将删除所有记账记录、账户、分类、预算、周期模板和账本，且不可恢复。")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = clearDataConfirmText,
                        onValueChange = { clearDataConfirmText = it },
                        label = { Text("请输入\"确认删除\"") },
                        singleLine = true,
                        isError = clearDataConfirmText.isNotEmpty() && !confirmed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                        clearDataConfirmText = ""
                    },
                    enabled = confirmed
                ) {
                    Text("确认清除", color = if (confirmed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    clearDataConfirmText = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "设置"
            )
        }
    ) { paddingValues ->
        SettingsPageScaffold(paddingValues = paddingValues) {
            SettingsSummaryCard(
                icon = Icons.Filled.Info,
                title = "喵喵记账",
                subtitle = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )

            val updateSubtitle = updateStatusSubtitle(updateUiState)
            if (updateSubtitle != null) {
                SettingsSummaryCard(
                    icon = Icons.Filled.Download,
                    title = "更新状态",
                    subtitle = updateSubtitle
                )
            }

            SettingsSectionHeader(
                title = "偏好",
                description = "控制主题、首页信息密度和默认统计周期。"
            )

            SettingsRowCard(
                icon = Icons.Filled.Palette,
                title = "主题",
                subtitle = themeMode.displayName,
                onClick = { showThemeDialog = true }
            )

            SettingsSwitchRowCard(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "首页收支概览",
                subtitle = "控制首页是否显示收支概览卡片",
                checked = showHomeOverviewCards,
                onCheckedChange = viewModel::setShowHomeOverviewCards
            )

            HomePeriodPreferenceItem(
                selectedPeriodType = selectedHomePeriod,
                onSelect = viewModel::setSelectedHomePeriod
            )

            SettingsSectionHeader(
                title = "账务结构",
                description = "管理账本、分类、预算和固定收支模板。"
            )

            SettingsRowCard(
                icon = Icons.Filled.AccountBalance,
                title = "账本管理",
                subtitle = "长按删除，自定义排序",
                onClick = onNavigateToLedgerManagement
            )

            SettingsRowCard(
                icon = Icons.Filled.Category,
                title = "分类管理",
                subtitle = "管理收支分类",
                onClick = onNavigateToCategories
            )

            SettingsRowCard(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "预算管理",
                subtitle = "设置不同周期及类型预算",
                onClick = onNavigateToBudget
            )

            SettingsRowCard(
                icon = Icons.Filled.CalendarMonth,
                title = "周期模板",
                subtitle = "工资、房租、订阅等固定记账",
                onClick = onNavigateToRecurringTemplates
            )

            SettingsSectionHeader(
                title = "数据与同步",
                description = "处理云端备份、本地备份、还原和外部导入。"
            )

            SettingsRowCard(
                icon = Icons.Filled.CloudSync,
                title = "DAV同步",
                subtitle = "自动备份、手动导入导出与同步预览",
                onClick = onNavigateToDavSettings
            )

            SettingsRowCard(
                icon = Icons.Filled.Download,
                title = "迁移与备份",
                subtitle = "外部导入、本地备份、还原与格式导出",
                onClick = onNavigateToExport
            )

            SettingsSectionHeader(
                title = "应用与安全",
                description = "更新检查和不可恢复的数据操作集中在这里。"
            )

            SettingsSwitchRowCard(
                icon = Icons.Filled.CloudSync,
                title = "自动检查更新",
                subtitle = if (updateEnabled) "已开启" else "已关闭",
                checked = updateEnabled,
                onCheckedChange = viewModel::setUpdateEnabled
            )

            SettingsRowCard(
                icon = Icons.Filled.Download,
                title = "检查更新",
                subtitle = if (!updateEnabled) "更新功能已关闭" else updateStatusSubtitle(updateUiState) ?: "点击检查",
                onClick = onCheckForUpdates
            )

            SettingsDangerRowCard(
                icon = Icons.Filled.DeleteForever,
                title = "清除数据",
                subtitle = "删除所有记账数据，不可恢复",
                onClick = { showClearDataDialog = true }
            )
        }
    }
}

private fun updateStatusSubtitle(updateUiState: AppUpdateUiState): String? {
    return when {
        updateUiState.isDownloading -> {
            val percentText = updateUiState.downloadProgressPercent?.let { "$it%" } ?: "进行中"
            "正在后台下载更新：$percentText"
        }

        updateUiState.isChecking -> "正在检查 GitHub Release..."
        updateUiState.availableRelease != null -> "发现新版本 ${updateUiState.availableRelease.versionName}"
        else -> null
    }
}

@Composable
private fun HomePeriodPreferenceItem(
    selectedPeriodType: BudgetPeriodType,
    onSelect: (BudgetPeriodType) -> Unit
) {
    SettingsSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "首页显示周期",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Text(
                        text = "当前：${selectedPeriodType.displayLabel()}，控制首页记录和金额概览的统计范围",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BudgetPeriodTypeSelector(
                selectedPeriodType = selectedPeriodType,
                onSelect = onSelect,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ThemeModeDialog(
    selectedThemeMode: AppThemeMode,
    onDismiss: () -> Unit,
    onSelect: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                AppThemeMode.entries.forEach { themeMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(themeMode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeMode.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (selectedThemeMode == themeMode) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
