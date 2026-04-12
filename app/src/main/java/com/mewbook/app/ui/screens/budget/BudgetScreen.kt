package com.mewbook.app.ui.screens.budget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mewbook.app.domain.model.BudgetWithSpending
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.theme.BudgetDanger
import com.mewbook.app.ui.theme.BudgetSafe
import com.mewbook.app.ui.theme.BudgetWarning
import com.mewbook.app.util.formatCurrency
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "预算"
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 月份选择器
                item {
                    MonthSelector(
                        selectedMonth = uiState.currentMonth,
                        onMonthChange = { viewModel.selectMonth(it) }
                    )
                }

                // 总预算卡片
                item {
                    TotalBudgetCard(
                        totalBudget = uiState.totalBudget,
                        totalSpent = uiState.totalSpent,
                        onAddClick = { viewModel.showAddDialog(null) }
                    )
                }

                // 分类预算列表
                item {
                    Text(
                        text = "分类预算",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.categoryBudgets.filter { it.budget.categoryId != null }) { budgetWithSpending ->
                    CategoryBudgetItem(
                        budgetWithSpending = budgetWithSpending,
                        categoryName = uiState.categories[budgetWithSpending.budget.categoryId]?.name ?: "未知",
                        categoryColor = uiState.categories[budgetWithSpending.budget.categoryId]?.color ?: 0xFF808080,
                        onEditClick = { viewModel.showAddDialog(budgetWithSpending.budget.categoryId) },
                        onDeleteClick = { viewModel.deleteBudget(budgetWithSpending.budget) }
                    )
                }

                item {
                    TextButton(
                        onClick = { viewModel.showAddDialog(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加预算")
                    }
                }
            }
        }

        // 添加/编辑预算对话框
        if (uiState.showAddDialog) {
            BudgetDialog(
                budget = uiState.editingBudget,
                onDismiss = { viewModel.hideDialog() },
                onSave = { amount -> viewModel.saveBudget(uiState.editingBudget?.categoryId, amount) }
            )
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: java.time.YearMonth,
    onMonthChange: (java.time.YearMonth) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "上一月")
        }

        Text(
            text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "下一月")
        }
    }
}

@Composable
fun TotalBudgetCard(
    totalBudget: com.mewbook.app.domain.model.Budget?,
    totalSpent: Double,
    onAddClick: () -> Unit
) {
    val budgetAmount = totalBudget?.amount ?: 0.0
    val remaining = budgetAmount - totalSpent
    val progress = if (budgetAmount > 0) (totalSpent / budgetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 0.9f -> BudgetDanger
            progress >= 0.7f -> BudgetWarning
            else -> BudgetSafe
        },
        label = "progressColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月预算",
                    style = MaterialTheme.typography.titleMedium
                )
                if (totalBudget == null) {
                    TextButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("设置预算")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatCurrency(budgetAmount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已花费 ${formatCurrency(totalSpent)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "剩余 ${formatCurrency(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remaining >= 0) BudgetSafe else BudgetDanger
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetItem(
    budgetWithSpending: BudgetWithSpending,
    categoryName: String,
    categoryColor: Long,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = budgetWithSpending.progress
    val progressColor by animateColorAsState(
        targetValue = when {
            progress >= 0.9f -> BudgetDanger
            progress >= 0.7f -> BudgetWarning
            else -> BudgetSafe
        },
        label = "progressColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .background(Color(categoryColor), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row {
                    TextButton(onClick = onEditClick) {
                        Text("编辑")
                    }
                    TextButton(onClick = onDeleteClick) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "预算 ${formatCurrency(budgetWithSpending.budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "已花 ${formatCurrency(budgetWithSpending.spent)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "剩余 ${formatCurrency(budgetWithSpending.remaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (budgetWithSpending.remaining >= 0) BudgetSafe else BudgetDanger
            )
        }
    }
}

@Composable
fun BudgetDialog(
    budget: com.mewbook.app.domain.model.Budget?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(budget?.amount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget != null) "编辑预算" else "添加预算") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = newValue
                    }
                },
                label = { Text("预算金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountText.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = amountText.toDoubleOrNull() != null && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
