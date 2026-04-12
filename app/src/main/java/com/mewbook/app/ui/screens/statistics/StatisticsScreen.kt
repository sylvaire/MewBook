package com.mewbook.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MewCompactTopAppBar(
                title = "统计"
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                TimeRangeSelector(
                    selectedRange = uiState.timeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )

                SummarySection(
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense
                )

                if (uiState.expenseByCategory.isNotEmpty()) {
                    ExpenseBreakdown(
                        expenseByCategory = uiState.expenseByCategory,
                        categories = uiState.categories
                    )
                }

                if (uiState.dailyIncome.isNotEmpty() || uiState.dailyExpense.isNotEmpty()) {
                    IncomeExpenseTrend(
                        incomeData = uiState.dailyIncome,
                        expenseData = uiState.dailyExpense,
                        labels = uiState.labels
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        when (range) {
                            TimeRange.WEEK -> "本周"
                            TimeRange.MONTH -> "本月"
                            TimeRange.YEAR -> "本年"
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun SummarySection(
    totalIncome: Double,
    totalExpense: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = "总收入",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+${formatCurrency(totalIncome)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总支出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-${formatCurrency(totalExpense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "结余",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val balance = totalIncome - totalExpense
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
fun ExpenseBreakdown(
    expenseByCategory: Map<Long, Double>,
    categories: Map<Long, com.mewbook.app.domain.model.Category>
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "支出构成",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val total = expenseByCategory.values.sum()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                expenseByCategory.entries.sortedByDescending { it.value }.take(5).forEach { (categoryId, amount) ->
                    val category = categories[categoryId]
                    val percentage = if (total > 0) (amount / total * 100) else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(category?.color ?: 0xFF808080))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = category?.name ?: "未知",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${formatCurrency(amount)} (${String.format("%.1f", percentage)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseTrend(
    incomeData: List<Double>,
    expenseData: List<Double>,
    labels: List<String>
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "收支趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(IncomeGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", style = MaterialTheme.typography.labelSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ExpenseRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (incomeData.isNotEmpty() || expenseData.isNotEmpty()) {
                    var selectedIndex by remember { mutableStateOf<Int?>(null) }
                    val maxValue = (incomeData + expenseData).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val chartHeight = 160.dp
                    val chartPadding = 45f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight + 30.dp)
                    ) {
                        // Y轴标签
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth()
                                .height(chartHeight)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatCurrency(maxValue),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                            Text(
                                text = formatCurrency(maxValue / 2),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        // 图表区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 45.dp)
                                .height(chartHeight)
                                .pointerInput(incomeData, expenseData) {
                                    detectTapGestures { offset ->
                                        val width = size.width.toFloat()
                                        val padding = chartPadding
                                        val stepX = (width - padding * 2) / (incomeData.size - 1).coerceAtLeast(1)
                                        val index = ((offset.x - padding) / stepX).toInt()
                                            .coerceIn(0, (incomeData.size - 1).coerceAtLeast(0))
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val padding = chartPadding

                                // 绘制网格线
                                for (i in 0..2) {
                                    val y = padding + (height - padding * 2) / 2 * i
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // 收入折线
                                if (incomeData.isNotEmpty()) {
                                    val incomePath = Path()
                                    val incomeStepX = (width - padding * 2) / (incomeData.size - 1).coerceAtLeast(1)

                                    incomeData.forEachIndexed { index, value ->
                                        val x = padding + index * incomeStepX
                                        val y = height - padding - ((value / maxValue) * (height - padding * 2)).toFloat()

                                        if (index == 0) {
                                            incomePath.moveTo(x, y)
                                        } else {
                                            incomePath.lineTo(x, y)
                                        }
                                    }

                                    drawPath(
                                        path = incomePath,
                                        color = IncomeGreen,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // 收入数据点
                                    incomeData.forEachIndexed { index, value ->
                                        val x = padding + index * incomeStepX
                                        val y = height - padding - ((value / maxValue) * (height - padding * 2)).toFloat()
                                        drawCircle(
                                            color = IncomeGreen,
                                            radius = if (selectedIndex == index) 8.dp.toPx() else 5.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }

                                // 支出折线
                                if (expenseData.isNotEmpty()) {
                                    val expensePath = Path()
                                    val expenseStepX = (width - padding * 2) / (expenseData.size - 1).coerceAtLeast(1)

                                    expenseData.forEachIndexed { index, value ->
                                        val x = padding + index * expenseStepX
                                        val y = height - padding - ((value / maxValue) * (height - padding * 2)).toFloat()

                                        if (index == 0) {
                                            expensePath.moveTo(x, y)
                                        } else {
                                            expensePath.lineTo(x, y)
                                        }
                                    }

                                    drawPath(
                                        path = expensePath,
                                        color = ExpenseRed,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // 支出数据点
                                    expenseData.forEachIndexed { index, value ->
                                        val x = padding + index * expenseStepX
                                        val y = height - padding - ((value / maxValue) * (height - padding * 2)).toFloat()
                                        drawCircle(
                                            color = ExpenseRed,
                                            radius = if (selectedIndex == index) 8.dp.toPx() else 5.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 选中数据详情
                    selectedIndex?.let { index ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = labels.getOrNull(index) ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "收入",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "+${formatCurrency(incomeData.getOrNull(index) ?: 0.0)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IncomeGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "支出",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "-${formatCurrency(expenseData.getOrNull(index) ?: 0.0)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                val balance = (incomeData.getOrNull(index) ?: 0.0) - (expenseData.getOrNull(index) ?: 0.0)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "结余",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = formatCurrency(balance),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (balance >= 0) IncomeGreen else ExpenseRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 45.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        labels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
