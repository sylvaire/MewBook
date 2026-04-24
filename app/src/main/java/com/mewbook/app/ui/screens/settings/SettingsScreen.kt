package com.mewbook.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.verticalScroll
import com.mewbook.app.BuildConfig
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.ui.components.BudgetPeriodTypeSelector
import com.mewbook.app.ui.components.MewCompactTopAppBar
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
    var showThemeDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showThemeDialog.value) {
        ThemeModeDialog(
            selectedThemeMode = themeMode,
            onDismiss = { showThemeDialog.value = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog.value = false
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "通用",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingsItem(
                icon = Icons.Filled.Palette,
                title = "主题",
                subtitle = themeMode.displayName,
                onClick = { showThemeDialog.value = true }
            )

            SettingsSwitchItem(
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

            SettingsItem(
                icon = Icons.Filled.AccountTree,
                title = "分支管理",
                subtitle = "长按删除，自定义排序",
                onClick = onNavigateToLedgerManagement
            )

            SettingsItem(
                icon = Icons.Filled.Category,
                title = "分类管理",
                subtitle = "管理收支分类",
                onClick = onNavigateToCategories
            )

            SettingsItem(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "预算管理",
                subtitle = "设置不同周期及类型预算",
                onClick = onNavigateToBudget
            )

            SettingsItem(
                icon = Icons.Filled.CalendarMonth,
                title = "周期模板",
                subtitle = "工资、房租、订阅等固定记账",
                onClick = onNavigateToRecurringTemplates
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "同步",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingsItem(
                icon = Icons.Filled.CloudSync,
                title = "DAV同步",
                subtitle = "云端导入导出与同步预览",
                onClick = onNavigateToDavSettings
            )

            SettingsItem(
                icon = Icons.Filled.Download,
                title = "迁移与备份",
                subtitle = "外部导入、本地备份、还原与格式导出",
                onClick = onNavigateToExport
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "关于",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingsItem(
                icon = Icons.Filled.Download,
                title = "检查更新",
                subtitle = updateStatusSubtitle(updateUiState),
                onClick = onCheckForUpdates
            )

            SettingsItem(
                icon = Icons.Filled.Info,
                title = "关于喵喵记账",
                subtitle = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = { }
            )
        }
    }
}

private fun updateStatusSubtitle(updateUiState: AppUpdateUiState): String {
    return when {
        updateUiState.isDownloading -> {
            val percentText = updateUiState.downloadProgressPercent?.let { "$it%" } ?: "进行中"
            "正在后台下载更新：$percentText"
        }

        updateUiState.isChecking -> "正在检查 GitHub Release..."
        updateUiState.availableRelease != null -> "发现新版本 ${updateUiState.availableRelease.versionName}"
        else -> "当前版本 ${BuildConfig.VERSION_NAME}"
    }
}

@Composable
private fun HomePeriodPreferenceItem(
    selectedPeriodType: BudgetPeriodType,
    onSelect: (BudgetPeriodType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
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
                        style = MaterialTheme.typography.bodyLarge
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

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
