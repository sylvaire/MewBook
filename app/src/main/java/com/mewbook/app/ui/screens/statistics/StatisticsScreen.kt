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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
                        labels = uiState.labels,
                        observedMask = uiState.observedMask,
                        timeRange = uiState.timeRange
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
    labels: List<String>,
    observedMask: List<Boolean>,
    timeRange: TimeRange
) {
    val density = LocalDensity.current
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "收支趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TrendLegendItem(label = "收入", color = IncomeGreen)
            TrendLegendItem(label = "支出", color = ExpenseRed)
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
                    val normalizedObservedMask = List(pointCount) { observedMask.getOrElse(it) { true } }
                    val defaultSelectedIndex = remember(normalizedIncome, normalizedExpense, normalizedObservedMask) {
                        buildSelectionIndex(
                            incomeData = normalizedIncome,
                            expenseData = normalizedExpense,
                            observedMask = normalizedObservedMask
                        )
                    }
                    var selectedIndex by remember(normalizedIncome, normalizedExpense, labels, normalizedObservedMask) {
                        mutableIntStateOf(defaultSelectedIndex)
                    }
                    val maxValue = (normalizedIncome + normalizedExpense).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val chartHeight = 208.dp
                    val selectedGuideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    val selectedIncome = normalizedIncome.getOrElse(selectedIndex) { 0.0 }
                    val selectedExpense = normalizedExpense.getOrElse(selectedIndex) { 0.0 }
                    val selectedLabel = labels.getOrElse(selectedIndex) { "" }
                    val selectedLabelDisplay = formatFocusedLabel(selectedLabel, timeRange)
                    val selectedBalance = selectedIncome - selectedExpense
                    val highestExpense = normalizedExpense.maxOrNull() ?: 0.0
                    val averageExpense = normalizedExpense.average().takeIf { !it.isNaN() } ?: 0.0
                    val pointRadius = 8.dp
                    val annotationBandHeight = 10.dp
                    val chartInnerHeight = chartHeight - annotationBandHeight - pointRadius * 2
                    val highestExpenseRatio = (highestExpense / maxValue).toFloat().coerceIn(0f, 1f)
                    val highestExpenseLineOffset = annotationBandHeight + pointRadius + chartInnerHeight * (1f - highestExpenseRatio)
                    var highestExpenseLabelHeightPx by remember(highestExpense) { mutableIntStateOf(0) }
                    val highestExpenseLabelOffset = with(density) {
                        val linePx = highestExpenseLineOffset.roundToPx()
                        val gapPx = 2.dp.roundToPx()
                        (linePx - highestExpenseLabelHeightPx - gapPx).coerceAtLeast(0).toDp()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    ) {
                        if (highestExpense > 0) {
                            Text(
                                text = formatCurrency(highestExpense),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 4.dp, top = highestExpenseLabelOffset),
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold,
                                onTextLayout = { highestExpenseLabelHeightPx = it.size.height }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(normalizedIncome, normalizedExpense, pointCount) {
                                    detectTapGestures { offset ->
                                        val pointRadiusPx = 8.dp.toPx()
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
                                        selectedIndex = index
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val pointRadiusPx = pointRadius.toPx()
                                val xStart = pointRadiusPx
                                val xEnd = size.width - pointRadiusPx
                                val yTop = annotationBandHeight.toPx() + pointRadiusPx
                                val yBottom = size.height - pointRadiusPx
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
                                val selectedX = xFor(selectedIndex)
                                val dashedPathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(10.dp.toPx(), 8.dp.toPx()),
                                    phase = 0f
                                )

                                if (highestExpense > 0) {
                                    val highestY = yFor(highestExpense)
                                    drawLine(
                                        color = ExpenseRed.copy(alpha = 0.32f),
                                        start = Offset(xStart, highestY),
                                        end = Offset(xEnd, highestY),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }

                                if (averageExpense > 0) {
                                    val averageY = yFor(averageExpense)
                                    drawLine(
                                        color = ExpenseRed.copy(alpha = 0.22f),
                                        start = Offset(xStart, averageY),
                                        end = Offset(xEnd, averageY),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = dashedPathEffect
                                    )
                                }

                                drawLine(
                                    color = selectedGuideColor,
                                    start = Offset(selectedX, yTop),
                                    end = Offset(selectedX, yBottom),
                                    strokeWidth = 2.dp.toPx()
                                )

                                val incomePath = Path()
                                normalizedIncome.forEachIndexed { index, value ->
                                    val x = xFor(index)
                                    val y = yFor(value)
                                    if (index == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
                                }
                                drawPath(
                                    path = incomePath,
                                    color = IncomeGreen,
                                    style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
                                )
                                normalizedIncome.forEachIndexed { index, value ->
                                    val isObserved = normalizedObservedMask.getOrElse(index) { true }
                                    val radius = if (selectedIndex == index && isObserved) 6.5.dp.toPx() else 5.dp.toPx()
                                    val solidOuterRadius = radius - 2.dp.toPx()
                                    val hollowStrokeWidth = 1.5.dp.toPx()
                                    val hollowRadius = (solidOuterRadius - hollowStrokeWidth / 2f).coerceAtLeast(1.dp.toPx())
                                    val center = Offset(xFor(index), yFor(value))
                                    if (selectedIndex == index && isObserved) {
                                        drawCircle(
                                            color = IncomeGreen.copy(alpha = 0.20f),
                                            radius = 10.dp.toPx(),
                                            center = center
                                        )
                                    }
                                    drawCircle(
                                        color = Color.White,
                                        radius = radius,
                                        center = center
                                    )
                                    if (isObserved) {
                                        drawCircle(
                                            color = IncomeGreen,
                                            radius = solidOuterRadius,
                                            center = center,
                                            style = Fill
                                        )
                                    } else {
                                        drawCircle(
                                            color = IncomeGreen.copy(alpha = 0.82f),
                                            radius = hollowRadius,
                                            center = center,
                                            style = Stroke(width = hollowStrokeWidth)
                                        )
                                    }
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
                                    style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
                                )
                                normalizedExpense.forEachIndexed { index, value ->
                                    val isObserved = normalizedObservedMask.getOrElse(index) { true }
                                    val radius = if (selectedIndex == index && isObserved) 6.5.dp.toPx() else 5.dp.toPx()
                                    val solidOuterRadius = radius - 2.dp.toPx()
                                    val hollowStrokeWidth = 1.5.dp.toPx()
                                    val hollowRadius = (solidOuterRadius - hollowStrokeWidth / 2f).coerceAtLeast(1.dp.toPx())
                                    val center = Offset(xFor(index), yFor(value))
                                    if (selectedIndex == index && isObserved) {
                                        drawCircle(
                                            color = ExpenseRed.copy(alpha = 0.20f),
                                            radius = 10.dp.toPx(),
                                            center = center
                                        )
                                    }
                                    drawCircle(
                                        color = Color.White,
                                        radius = radius,
                                        center = center
                                    )
                                    if (isObserved) {
                                        drawCircle(
                                            color = ExpenseRed,
                                            radius = solidOuterRadius,
                                            center = center,
                                            style = Fill
                                        )
                                    } else {
                                        drawCircle(
                                            color = ExpenseRed.copy(alpha = 0.82f),
                                            radius = hollowRadius,
                                            center = center,
                                            style = Stroke(width = hollowStrokeWidth)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedLabelDisplay,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "当前聚焦",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TrendMetricRow(
                                label = "收入",
                                value = "+${formatCurrency(selectedIncome)}",
                                valueColor = IncomeGreen
                            )
                            TrendMetricRow(
                                label = "支出",
                                value = "-${formatCurrency(selectedExpense)}",
                                valueColor = ExpenseRed
                            )
                            TrendMetricRow(
                                label = "结余",
                                value = formatCurrency(selectedBalance),
                                valueColor = if (selectedBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val bottomLabels = buildBottomLabels(labels)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        bottomLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (label == selectedLabel) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (label == selectedLabel) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendLegendItem(
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TrendMetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildSelectionIndex(
    incomeData: List<Double>,
    expenseData: List<Double>,
    observedMask: List<Boolean>
): Int {
    val combined = incomeData.zip(expenseData) { income, expense -> income + expense }
    for (index in combined.indices.reversed()) {
        if (observedMask.getOrElse(index) { true } && combined[index] > 0.0) {
            return index
        }
    }
    for (index in observedMask.indices.reversed()) {
        if (observedMask[index]) {
            return index
        }
    }
    return combined.lastIndex.coerceAtLeast(0)
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

private fun formatFocusedLabel(
    label: String,
    timeRange: TimeRange
): String {
    return if (timeRange == TimeRange.MONTH && label.all { it.isDigit() }) {
        "${label}日"
    } else {
        label
    }
}
