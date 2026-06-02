package com.mewbook.app.ui.screens.recyclebin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DeletedRecord
import com.mewbook.app.domain.policy.RecycleBinAmountFilter
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.RecordTrashPolicy
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.MewSnackbarHost
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.components.SettingsSurfaceCard
import com.mewbook.app.ui.theme.LocalMewBookSemanticColors
import com.mewbook.app.util.formatCurrency
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteForeverId by remember { mutableLongStateOf(0L) }
    var pendingBulkDelete by remember { mutableStateOf(false) }
    val now = remember(uiState.deletedRecords) { LocalDateTime.now() }
    val actionsEnabled = !uiState.isBatchBusy && uiState.busyRecordId == null

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (pendingDeleteForeverId != 0L) {
        AlertDialog(
            onDismissRequest = { pendingDeleteForeverId = 0L },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("永久删除") },
            text = { Text("永久删除后无法再从回收站找回，确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteForever(pendingDeleteForeverId)
                        pendingDeleteForeverId = 0L
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("永久删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteForeverId = 0L }) {
                    Text("取消")
                }
            }
        )
    }

    if (pendingBulkDelete) {
        AlertDialog(
            onDismissRequest = { pendingBulkDelete = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("批量永久删除") },
            text = { Text("将永久删除已选的 ${uiState.selectedRecordIds.size} 条记录，删除后无法找回。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedForever()
                        pendingBulkDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("永久删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulkDelete = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { MewSnackbarHost(snackbarHostState) },
        topBar = {
            MewCompactTopAppBar(
                title = "回收站",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.expiringSoonCount > 0) {
                        item(key = "expiring_warning") {
                            SettingsSummaryCard(
                                icon = Icons.Filled.Warning,
                                title = "${uiState.expiringSoonCount} 条记录即将自动清理",
                                subtitle = "这些记录将在 1 天内到期，建议先恢复或永久删除。"
                            )
                        }
                    }

                    if (uiState.deletedRecords.isEmpty()) {
                        item(key = "empty") {
                            SettingsSummaryCard(
                                icon = Icons.Filled.DeleteForever,
                                title = "暂无可找回记录",
                                subtitle = "删除的记录会在这里本地保留 30 天。"
                            )
                        }
                    } else {
                        item(key = "filters") {
                            RecycleBinFilterPanel(
                                totalCount = uiState.deletedRecords.size,
                                visibleCount = uiState.visibleDeletedRecords.size,
                                selectedCount = uiState.selectedRecordIds.size,
                                availableCategories = uiState.availableFilterCategories,
                                selectedCategoryId = uiState.selectedCategoryId,
                                amountFilter = uiState.amountFilter,
                                allVisibleSelected = uiState.visibleDeletedRecords.isNotEmpty() &&
                                    uiState.visibleDeletedRecords.all { it.record.id in uiState.selectedRecordIds },
                                actionsEnabled = actionsEnabled,
                                onCategorySelected = { viewModel.setCategoryFilter(it) },
                                onAmountFilterSelected = { viewModel.setAmountFilter(it) },
                                onSelectAllVisible = { viewModel.selectAllVisible() },
                                onClearSelection = { viewModel.clearSelection() },
                                onRestoreSelected = { viewModel.restoreSelected() },
                                onDeleteSelected = { pendingBulkDelete = true }
                            )
                        }

                        if (uiState.visibleDeletedRecords.isEmpty()) {
                            item(key = "filter_empty") {
                                SettingsSummaryCard(
                                    icon = Icons.Filled.FilterList,
                                    title = "没有匹配的记录",
                                    subtitle = "换个分类或金额范围再试试。"
                                )
                            }
                        }

                        items(
                            items = uiState.visibleDeletedRecords,
                            key = { it.record.id },
                            contentType = { "deleted_record" }
                        ) { deletedRecord ->
                            DeletedRecordCard(
                                deletedRecord = deletedRecord,
                                category = uiState.categoriesById[deletedRecord.record.categoryId],
                                accountName = deletedRecord.record.accountId?.let { accountId ->
                                    uiState.accountsById[accountId]?.name
                                },
                                now = now,
                                selected = deletedRecord.record.id in uiState.selectedRecordIds,
                                actionsEnabled = actionsEnabled,
                                onToggleSelected = { viewModel.toggleSelection(deletedRecord.record.id) },
                                onRestore = { viewModel.restore(deletedRecord.record.id) },
                                onDeleteForever = { pendingDeleteForeverId = deletedRecord.record.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecycleBinFilterPanel(
    totalCount: Int,
    visibleCount: Int,
    selectedCount: Int,
    availableCategories: List<Category>,
    selectedCategoryId: Long?,
    amountFilter: RecycleBinAmountFilter,
    allVisibleSelected: Boolean,
    actionsEnabled: Boolean,
    onCategorySelected: (Long?) -> Unit,
    onAmountFilterSelected: (RecycleBinAmountFilter) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearSelection: () -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    SettingsSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选与批量处理",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$visibleCount / $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("全部分类") }
                )
                availableCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name, maxLines = 1) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecycleBinAmountFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = amountFilter == filter,
                        onClick = { onAmountFilterSelected(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSelectAllVisible,
                    enabled = actionsEnabled && visibleCount > 0
                ) {
                    Text(if (allVisibleSelected) "取消全选" else "全选当前")
                }

                if (selectedCount > 0) {
                    TextButton(
                        onClick = onRestoreSelected,
                        enabled = actionsEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("恢复 $selectedCount")
                    }
                    TextButton(
                        onClick = onDeleteSelected,
                        enabled = actionsEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("删除 $selectedCount")
                    }
                    TextButton(
                        onClick = onClearSelection,
                        enabled = actionsEnabled
                    ) {
                        Text("清除")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletedRecordCard(
    deletedRecord: DeletedRecord,
    category: Category?,
    accountName: String?,
    now: LocalDateTime,
    selected: Boolean,
    actionsEnabled: Boolean,
    onToggleSelected: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val semanticColors = LocalMewBookSemanticColors.current
    val record = deletedRecord.record
    val categoryForIcon = category ?: Category(
        id = record.categoryId,
        name = "未知分类",
        icon = "more_horiz",
        color = 0xFF9E9E9E,
        type = record.type,
        isDefault = false,
        sortOrder = 0
    )
    val amountPrefix = if (record.type == RecordType.INCOME) "+" else "-"
    val amountColor = if (record.type == RecordType.INCOME) semanticColors.income else semanticColors.expense

    SettingsSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelected() },
                    enabled = actionsEnabled
                )

                CategoryIconBadge(
                    category = categoryForIcon,
                    emphasized = true,
                    containerSize = 52.dp,
                    iconSize = 26.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = categoryForIcon.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = record.note?.takeIf { it.isNotBlank() } ?: "无备注",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$amountPrefix${formatCurrency(record.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = record.date.format(recordDateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                accountName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "删除于 ${deletedRecord.deletedAt.format(deletedAtFormatter)} · ${remainingDaysLabel(deletedRecord.deletedAt, now)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onRestore,
                    enabled = actionsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("恢复")
                }

                TextButton(
                    onClick = onDeleteForever,
                    enabled = actionsEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("永久删除")
                }
            }
        }
    }
}

private val recordDateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
private val deletedAtFormatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")

private fun remainingDaysLabel(deletedAt: LocalDateTime, now: LocalDateTime): String {
    val remainingDays = RecordTrashPolicy.remainingDays(deletedAt, now)
    return if (remainingDays <= 0) {
        "今天到期"
    } else {
        "剩余 $remainingDays 天"
    }
}

private val RecycleBinAmountFilter.label: String
    get() = when (this) {
        RecycleBinAmountFilter.ALL -> "全部金额"
        RecycleBinAmountFilter.UP_TO_50 -> "50 以下"
        RecycleBinAmountFilter.FROM_50_TO_200 -> "50-200"
        RecycleBinAmountFilter.OVER_200 -> "200 以上"
    }
