package com.mewbook.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.components.AccountTypeIconBadge
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency

@Composable
fun RecordDetailDialog(
    record: Record,
    category: Category?,
    account: Account?,
    onDismiss: () -> Unit,
    onEdit: (Record) -> Unit
) {
    val displayCategory = remember(record.categoryId, record.type, category) {
        category ?: Category(
            id = record.categoryId,
            name = "未知分类",
            icon = "more_horiz",
            color = 0xFF9E9E9E,
            type = record.type,
            isDefault = false,
            sortOrder = 0
        )
    }
    val isIncome = record.type == RecordType.INCOME
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val typeLabel = if (isIncome) "收入" else "支出"
    val signedAmount = "${if (isIncome) "+" else "-"}${formatCurrency(record.amount)}"
    val noteText = record.note?.takeIf { it.isNotBlank() } ?: "无备注"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "记账详情",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = amountColor.copy(alpha = 0.12f),
                        contentColor = amountColor,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Text(
                    text = signedAmount,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecordDetailRow(label = "分类") {
                    CategoryIconBadge(
                        category = displayCategory,
                        emphasized = true,
                        containerSize = 38.dp,
                        iconSize = 20.dp
                    )
                    Text(
                        text = displayCategory.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
                RecordDetailRow(label = "账户") {
                    if (account != null) {
                        AccountTypeIconBadge(
                            type = account.type,
                            accentColor = Color(account.color),
                            containerSize = 38.dp,
                            iconSize = 20.dp,
                            emphasized = true
                        )
                    }
                    Text(
                        text = account?.name ?: "未选择账户",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                RecordDetailRow(label = "日期时间") {
                    Text(
                        text = RecordDetailTimeFormatter.format(
                            date = record.date,
                            timeSource = record.createdAt
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                RecordDetailRow(label = "备注") {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (noteText == "无备注") {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        confirmButton = {
            Button(onClick = { onEdit(record) }) {
                Text("编辑")
            }
        }
    )
}

@Composable
private fun RecordDetailRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
