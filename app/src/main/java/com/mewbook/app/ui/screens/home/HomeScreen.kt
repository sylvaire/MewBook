package com.mewbook.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.ui.components.RecordItem
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.screens.add.AddEditRecordSheet
import com.mewbook.app.ui.theme.BudgetWarning
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency
import java.time.format.DateTimeFormatter

// ============================================
// Warm Claymorphism Home Screen
// 温暖黏土风首页
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddSheetVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isDarkTheme = isSystemInDarkTheme()
    var pendingDeleteRecordId by remember { mutableLongStateOf(0L) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalBudget by viewModel.totalBudget.collectAsStateWithLifecycle()
    val budgetRemaining by viewModel.budgetRemaining.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.showAddEditSheet) {
        onAddSheetVisibilityChanged(uiState.showAddEditSheet)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // 温暖渐变顶部导航栏
                MewCompactTopAppBar(
                    title = "喵喵记账",
                    titleContent = {
                        Text(
                            text = "🐱",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "喵喵记账",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
            },
            floatingActionButton = {
                // Claymorphism 浮动按钮 - 多层阴影
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.18f else 0.4f)
                        )
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.10f else 0.25f)
                        )
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.showAddSheet() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "添加记录",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 月份选择器
                MonthSelector(
                    selectedMonth = uiState.selectedMonth,
                    onMonthChange = { viewModel.selectMonth(it) }
                )

                // 概要卡片 - Claymorphism 风格
                SummaryCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    totalBudget = totalBudget,
                    budgetRemaining = budgetRemaining
                )

                // 记录列表
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (records.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🍊",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "还没有记录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击 + 添加第一笔记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 100.dp // 为 FAB 留出空间
                        ),
                        verticalArrangement = Arrangement.spacedBy(ClayDesign.CardSpacing)
                    ) {
                        items(records, key = { it.id }) { record ->
                            val category = uiState.categories[record.categoryId]
                            RecordItem(
                                record = record,
                                categoryName = category?.name ?: "未知",
                                categoryIcon = category?.icon ?: "more_horiz",
                                categoryColor = category?.color ?: 0xFF808080,
                                onClick = { viewModel.showEditSheet(record) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showAddEditSheet) {
            AddEditRecordSheet(
                categories = uiState.categories.values.toList(),
                accounts = uiState.accounts,
                editingRecord = uiState.editingRecord,
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

@Composable
fun MonthSelector(
    selectedMonth: java.time.YearMonth,
    onMonthChange: (java.time.YearMonth) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧按钮 - 黏土圆形
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.08f else 0.2f))
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onMonthChange(selectedMonth.minusMonths(1)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "上一月",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 月份文字
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 右侧按钮 - 黏土圆形
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.08f else 0.2f))
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onMonthChange(selectedMonth.plusMonths(1)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "下一月",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    totalBudget: Double = 0.0,
    budgetRemaining: Double = 0.0
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardShadowPrimary = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.08f else 0.15f)
    val cardShadowSecondary = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.04f else 0.10f)
    val progressShadow = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.18f else 0.10f)

    // Claymorphism 卡片 - 多层阴影
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = ClayDesign.CardShadowElevation1,
                shape = RoundedCornerShape(ClayDesign.CardRadius),
                spotColor = cardShadowPrimary
            )
            .shadow(
                elevation = ClayDesign.CardShadowElevation2,
                shape = RoundedCornerShape(ClayDesign.CardRadius),
                spotColor = cardShadowSecondary
            ),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ClayDesign.CardPadding + 4.dp)
        ) {
            // 预算进度（如果有设置）
            if (totalBudget > 0) {
                // 预算剩余
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🍊 ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "预算剩余",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCurrency(budgetRemaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            budgetRemaining < 0 -> ExpenseRed
                            budgetRemaining < totalBudget * 0.2 -> BudgetWarning
                            else -> IncomeGreen
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 进度条 - Claymorphism 风格
                val progress = (totalExpense / totalBudget).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .shadow(2.dp, RoundedCornerShape(6.dp), spotColor = progressShadow)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(12.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = when {
                                        progress >= 0.9f -> listOf(ExpenseRed, ExpenseRed)
                                        progress >= 0.7f -> listOf(BudgetWarning, BudgetWarning)
                                        else -> listOf(IncomeGreen, IncomeGreen)
                                    }
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "已花费 ${formatCurrency(totalExpense)} / ${formatCurrency(totalBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 收入、支出、结余 - 三列布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            IncomeGreen.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "📈",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatCurrency(totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 支出
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            ExpenseRed.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "📉",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${formatCurrency(totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 结余
                val balance = totalIncome - totalExpense
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (balance >= 0) IncomeGreen.copy(alpha = 0.08f)
                            else ExpenseRed.copy(alpha = 0.08f),
                            RoundedCornerShape(ClayDesign.ButtonRadius)
                        )
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = if (balance >= 0) "💰" else "😿",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "结余",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}
