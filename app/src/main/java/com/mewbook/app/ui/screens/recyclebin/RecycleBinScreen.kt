package com.mewbook.app.ui.screens.recyclebin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.RecordTrashPolicy
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.components.MewSnackbarHost
import com.mewbook.app.ui.components.SettingsSummaryCard
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.ui.theme.clayCardShadow
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
    val now = remember(uiState.deletedRecords) { LocalDateTime.now() }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    SettingsSummaryCard(
                        icon = Icons.Filled.Restore,
                        title = "删除记录保留 30 天",
                        subtitle = "恢复后会重新计入首页、统计和账户余额。"
                    )
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
                    items(
                        items = uiState.deletedRecords,
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
                            actionsEnabled = uiState.busyRecordId == null,
                            onRestore = { viewModel.restore(deletedRecord.record.id) },
                            onDeleteForever = { pendingDeleteForeverId = deletedRecord.record.id }
                        )
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
    actionsEnabled: Boolean,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
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
    val amountColor = if (record.type == RecordType.INCOME) IncomeGreen else ExpenseRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    category = categoryForIcon,
                    emphasized = true,
                    containerSize = 52.dp,
                    iconSize = 26.dp
                )

                Spacer(modifier = Modifier.width(ClayDesign.CardSpacing + 4.dp))

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
