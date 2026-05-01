package com.mewbook.app.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.RecordItem
import com.mewbook.app.ui.screens.add.AddEditRecordSheet
import com.mewbook.app.ui.screens.home.RecordDetailDialog
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.clayCardShadow
import com.mewbook.app.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpenseDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryExpenseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteRecordId by remember { mutableLongStateOf(0L) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "分类支出",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero card: category icon + name + period + total
                item(key = "hero") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clayCardShadow(),
                        shape = RoundedCornerShape(ClayDesign.CardRadius),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ClayDesign.CardPadding + 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(
                                    category = Category(
                                        name = uiState.categoryName,
                                        icon = uiState.categoryIcon,
                                        color = uiState.categoryColor,
                                        type = RecordType.EXPENSE,
                                        isDefault = true,
                                        sortOrder = 0
                                    ),
                                    emphasized = true,
                                    containerSize = 52.dp,
                                    iconSize = 26.dp
                                )
                                Spacer(modifier = Modifier.width(ClayDesign.CardSpacing + 4.dp))
                                Column {
                                    Text(
                                        text = uiState.categoryName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.periodSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "支出合计",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${formatCurrency(uiState.totalExpense)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }

                // Stats summary card: count + average
                if (uiState.records.isNotEmpty()) {
                    item(key = "stats") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clayCardShadow(),
                            shape = RoundedCornerShape(ClayDesign.CardRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "笔数",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${uiState.records.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "笔均",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val avg = if (uiState.records.isNotEmpty()) {
                                        uiState.totalExpense / uiState.records.size
                                    } else 0.0
                                    Text(
                                        text = formatCurrency(avg),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Record list
                if (uiState.records.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "该周期内暂无此类支出记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.records, key = { it.id }) { record ->
                        RecordItem(
                            record = record,
                            categoryName = uiState.categoryName,
                            categoryIcon = uiState.categoryIcon,
                            categoryColor = uiState.categoryColor,
                            onClick = { viewModel.showRecordDetail(record) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Record detail dialog
        uiState.browsingRecord?.let { browsingRecord ->
            RecordDetailDialog(
                record = browsingRecord,
                category = Category(
                    name = uiState.categoryName,
                    icon = uiState.categoryIcon,
                    color = uiState.categoryColor,
                    type = RecordType.EXPENSE,
                    isDefault = true,
                    sortOrder = 0
                ),
                account = uiState.accounts.firstOrNull { it.id == browsingRecord.accountId },
                onDismiss = { viewModel.hideRecordDetail() },
                onEdit = { record -> viewModel.editRecordFromDetail(record) }
            )
        }

        // Edit record sheet
        if (uiState.showAddEditSheet) {
            AddEditRecordSheet(
                categories = uiState.categories,
                accounts = uiState.accounts,
                recentNotesByCategory = uiState.recentNotesByCategory,
                defaultAccountId = uiState.defaultAccountId,
                defaultDate = uiState.editingRecord?.date ?: java.time.LocalDate.now(),
                editingRecord = uiState.editingRecord,
                initialType = RecordType.EXPENSE,
                onDismiss = { viewModel.hideAddEditSheet() },
                onSave = { amount, type, categoryId, note, date, accountId ->
                    viewModel.saveRecord(amount, type, categoryId, note, date, accountId)
                },
                onDelete = { id ->
                    pendingDeleteRecordId = id
                    showDeleteConfirmDialog = true
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    pendingDeleteRecordId = 0L
                },
                title = { Text("确认删除") },
                text = { Text("删除后无法恢复，确定要删除这条记录吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteRecord(pendingDeleteRecordId)
                            viewModel.hideAddEditSheet()
                            showDeleteConfirmDialog = false
                            pendingDeleteRecordId = 0L
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            pendingDeleteRecordId = 0L
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
