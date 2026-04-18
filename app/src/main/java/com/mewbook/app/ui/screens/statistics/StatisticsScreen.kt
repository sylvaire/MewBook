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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                PeriodNavigator(
                    periodLabel = uiState.periodLabel,
                    canGoNext = uiState.canGoNext,
                    onPrevious = { viewModel.previousPeriod() },
                    onNext = { viewModel.nextPeriod() }
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
                            TimeRange.WEEK -> "周"
                            TimeRange.MONTH -> "月"
                            TimeRange.YEAR -> "年"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun PeriodNavigator(
    periodLabel: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "上一周期"
            )
        }
        Text(
            text = periodLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "下一周期"
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
                    val pointCount = maxOf(incomeData.size, expenseData.size, labels.size).coerceAtLeast(1)
                    val normalizedIncome = List(pointCount) { incomeData.getOrElse(it) { 0.0 } }
                    val normalizedExpense = List(pointCount) { expenseData.getOrElse(it) { 0.0 } }
                    var selectedIndex by remember(normalizedIncome, normalizedExpense, labels) {
                        mutableStateOf<Int?>(null)
                    }
                    val maxValue = (normalizedIncome + normalizedExpense).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val chartHeight = 180.dp
                    val yAxisWidth = 56.dp
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .width(yAxisWidth)
                                .fillMaxSize()
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = formatCurrency(maxValue),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(maxValue / 2),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .pointerInput(normalizedIncome, normalizedExpense, pointCount) {
                                    detectTapGestures { offset ->
                                        val pointRadiusPx = 6.dp.toPx()
                                        val xStart = pointRadiusPx
                                        val xEnd = size.width - pointRadiusPx
                                        val stepX = if (pointCount > 1) {
                                            (xEnd - xStart) / (pointCount - 1)
                                        } else {
                                            0f
                                        }
                                        val index = if (pointCount == 1) {
                                            0
                                        } else {
                                            ((offset.x - xStart) / stepX).toInt().coerceIn(0, pointCount - 1)
                                        }
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val pointRadius = 6.dp.toPx()
                                val xStart = pointRadius
                                val xEnd = size.width - pointRadius
                                val yTop = pointRadius
                                val yBottom = size.height - pointRadius
                                val chartHeightPx = (yBottom - yTop).coerceAtLeast(1f)
                                val stepX = if (pointCount > 1) {
                                    (xEnd - xStart) / (pointCount - 1)
                                } else {
                                    0f
                                }
                                fun xFor(index: Int): Float = if (pointCount == 1) {
                                    (xStart + xEnd) / 2f
                                } else {
                                    xStart + index * stepX
                                }
                                fun yFor(value: Double): Float {
                                    val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
                                    return yBottom - ratio * chartHeightPx
                                }

                                for (i in 0..2) {
                                    val y = yTop + (chartHeightPx / 2f) * i
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(xStart, y),
                                        end = Offset(xEnd, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                val incomePath = Path()
                                normalizedIncome.forEachIndexed { index, value ->
                                    val x = xFor(index)
                                    val y = yFor(value)
                                    if (index == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
                                }
                                drawPath(
                                    path = incomePath,
                                    color = IncomeGreen,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                                normalizedIncome.forEachIndexed { index, value ->
                                    drawCircle(
                                        color = IncomeGreen,
                                        radius = if (selectedIndex == index) 8.dp.toPx() else 5.dp.toPx(),
                                        center = Offset(xFor(index), yFor(value))
                                    )
                                }

                                val expensePath = Path()
                                normalizedExpense.forEachIndexed { index, value ->
                                    val x = xFor(index)
                                    val y = yFor(value)
                                    if (index == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
                                }
                                drawPath(
                                    path = expensePath,
                                    color = ExpenseRed,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                                normalizedExpense.forEachIndexed { index, value ->
                                    drawCircle(
                                        color = ExpenseRed,
                                        radius = if (selectedIndex == index) 8.dp.toPx() else 5.dp.toPx(),
                                        center = Offset(xFor(index), yFor(value))
                                    )
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
                                        text = "+${formatCurrency(normalizedIncome.getOrElse(index) { 0.0 })}",
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
                                        text = "-${formatCurrency(normalizedExpense.getOrElse(index) { 0.0 })}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                val balance = normalizedIncome.getOrElse(index) { 0.0 } - normalizedExpense.getOrElse(index) { 0.0 }
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

                    val bottomLabels = buildBottomLabels(labels)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = yAxisWidth),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        bottomLabels.forEach { label ->
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

private fun buildBottomLabels(labels: List<String>): List<String> {
    if (labels.isEmpty()) {
        return emptyList()
    }
    if (labels.size <= 8) {
        return labels
    }

    val indexCandidates = listOf(
        0,
        labels.lastIndex / 4,
        labels.lastIndex / 2,
        labels.lastIndex * 3 / 4,
        labels.lastIndex
    )
    return indexCandidates.map { labels[it.coerceIn(0, labels.lastIndex)] }
}
