package com.mewbook.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.ui.components.CategoryIconBadge
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.ui.theme.clayCardShadow
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
    val noteText = record.note?.takeIf { it.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clayCardShadow(),
            shape = RoundedCornerShape(ClayDesign.CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ClayDesign.CardPadding + 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: category icon + name + type badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconBadge(
                        category = displayCategory,
                        emphasized = true,
                        containerSize = 48.dp,
                        iconSize = 24.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayCategory.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = amountColor.copy(alpha = 0.12f),
                            contentColor = amountColor,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Amount - hero element
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "金额",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Detail rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Account
                    RecordDetailRow(
                        icon = Icons.Filled.Wallet,
                        label = "账户"
                    ) {
                        Text(
                            text = account?.name ?: "未选择账户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (account != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Date
                    RecordDetailRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "日期"
                    ) {
                        Text(
                            text = RecordDetailTimeFormatter.format(
                                date = record.date,
                                timeSource = record.createdAt
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Note
                    RecordDetailRow(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = "备注"
                    ) {
                        Text(
                            text = noteText ?: "无备注",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (noteText != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(ClayDesign.ButtonRadius)
                    ) {
                        Text("关闭")
                    }
                    Button(
                        onClick = { onEdit(record) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("编辑")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}
