package com.mewbook.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mewbook.app.domain.model.Category
import com.mewbook.app.ui.components.MewCompactTopAppBar
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.util.formatCurrency
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateToCategoryExpense: (categoryId: Long, periodStart: LocalDate, periodEnd: LocalDate) -> Unit = { _, _, _ -> },
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                StatisticsPeriodControls(
                    selectedRange = uiState.timeRange,
                    periodLabel = uiState.periodLabel,
                    canGoNext = uiState.canGoNext,
                    onRangeSelected = { viewModel.setTimeRange(it) },
                    onPrevious = { viewModel.previousPeriod() },
                    onNext = { viewModel.nextPeriod() }
                )

                SummarySection(
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense
                )

                if (uiState.expenseByCategory.isNotEmpty()) {
                    CategoryBreakdown(
                        title = "支出构成",
                        amountByCategory = uiState.expenseByCategory,
                        categories = uiState.categories,
                        amountPrefix = "-",
                        accentColor = ExpenseRed,
                        onCategoryClick = { categoryId ->
                            onNavigateToCategoryExpense(
                                categoryId,
                                uiState.periodStart,
                                uiState.periodEnd
                            )
                        }
                    )
                }

                if (uiState.incomeByCategory.isNotEmpty()) {
                    CategoryBreakdown(
                        title = "收入构成",
                        amountByCategory = uiState.incomeByCategory,
                        categories = uiState.categories,
                        amountPrefix = "+",
                        accentColor = IncomeGreen
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
private fun StatisticsPeriodControls(
    selectedRange: TimeRange,
    periodLabel: String,
    canGoNext: Boolean,
    onRangeSelected: (TimeRange) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
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
                                },
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "下一周期"
                    )
                }
            }
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
fun CategoryBreakdown(
    title: String,
    amountByCategory: Map<Long, Double>,
    categories: Map<Long, Category>,
    amountPrefix: String,
    accentColor: Color,
    onCategoryClick: ((Long) -> Unit)? = null
) {
    val total = amountByCategory.values.sum()
    val topCategories = amountByCategory.entries.sortedByDescending { it.value }.take(5)

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Top ${topCategories.size}",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                topCategories.forEach { (categoryId, amount) ->
                    val category = categories[categoryId]
                    val categoryColor = Color(category?.color ?: 0xFF808080)
                    val percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                    val clickableModifier = if (onCategoryClick != null) {
                        Modifier.clickable { onCategoryClick(categoryId) }
                    } else {
                        Modifier
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(clickableModifier),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category?.name ?: "未知",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${String.format("%.1f", percentage)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accentColor.copy(alpha = 0.10f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(categoryColor.copy(alpha = 0.86f))
                                )
                            }
                        }

                        Text(
                            text = "$amountPrefix${formatCurrency(amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
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
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "收支趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                    val selectedIncome = normalizedIncome.getOrElse(selectedIndex) { 0.0 }
                    val selectedExpense = normalizedExpense.getOrElse(selectedIndex) { 0.0 }
                    val selectedLabel = labels.getOrElse(selectedIndex) { "" }
                    val selectedLabelDisplay = formatFocusedLabel(selectedLabel, timeRange)
                    val selectedBalance = selectedIncome - selectedExpense
                    val totalIncome = normalizedIncome.sum()
                    val totalExpense = normalizedExpense.sum()
                    val pointRadius = 7.dp
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
                    val selectedGuideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    val pointSurfaceColor = MaterialTheme.colorScheme.surface

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrendSeriesPill(
                            label = "收入",
                            value = "+${formatCurrency(totalIncome)}",
                            color = IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TrendSeriesPill(
                            label = "支出",
                            value = "-${formatCurrency(totalExpense)}",
                            color = ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    ) {
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
                                val yTop = 10.dp.toPx() + pointRadiusPx
                                val yBottom = size.height - 10.dp.toPx() - pointRadiusPx
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

                                repeat(4) { index ->
                                    val y = yTop + chartHeightPx * index / 3f
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(xStart, y),
                                        end = Offset(xEnd, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                drawLine(
                                    color = selectedGuideColor,
                                    start = Offset(selectedX, yTop),
                                    end = Offset(selectedX, yBottom),
                                    strokeWidth = 2.dp.toPx()
                                )

                                fun drawSeriesPath(data: List<Double>, color: Color) {
                                    val path = Path()
                                    data.forEachIndexed { index, value ->
                                        val x = xFor(index)
                                        val y = yFor(value)
                                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = color.copy(alpha = 0.94f),
                                        style = Stroke(
                                            width = 2.25.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                                fun drawSeriesPoints(data: List<Double>, color: Color) {
                                    data.forEachIndexed { index, value ->
                                        val isObserved = normalizedObservedMask.getOrElse(index) { true }
                                        val isSelected = selectedIndex == index && isObserved
                                        val radius = if (isSelected) 6.5.dp.toPx() else 5.dp.toPx()
                                        val solidOuterRadius = radius - 1.9.dp.toPx()
                                        val hollowStrokeWidth = 1.5.dp.toPx()
                                        val hollowRadius = (solidOuterRadius - hollowStrokeWidth / 2f).coerceAtLeast(1.dp.toPx())
                                        val center = Offset(xFor(index), yFor(value))
                                        if (isSelected) {
                                            drawCircle(
                                                color = color.copy(alpha = 0.18f),
                                                radius = 11.dp.toPx(),
                                                center = center
                                            )
                                        }
                                        drawCircle(
                                            color = pointSurfaceColor,
                                            radius = radius,
                                            center = center
                                        )
                                        if (isObserved) {
                                            drawCircle(
                                                color = color,
                                                radius = solidOuterRadius,
                                                center = center,
                                                style = Fill
                                            )
                                        } else {
                                            drawCircle(
                                                color = color.copy(alpha = 0.70f),
                                                radius = hollowRadius,
                                                center = center,
                                                style = Stroke(width = hollowStrokeWidth)
                                            )
                                        }
                                    }
                                }

                                drawSeriesPath(normalizedIncome, IncomeGreen)
                                drawSeriesPath(normalizedExpense, ExpenseRed)
                                drawSeriesPoints(normalizedIncome, IncomeGreen)
                                drawSeriesPoints(normalizedExpense, ExpenseRed)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = "聚焦点",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TrendFocusValue(
                                    label = "收入",
                                    value = "+${formatCurrency(selectedIncome)}",
                                    valueColor = IncomeGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                TrendFocusValue(
                                    label = "支出",
                                    value = "-${formatCurrency(selectedExpense)}",
                                    valueColor = ExpenseRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            TrendMetricRow(
                                label = "结余",
                                value = formatCurrency(selectedBalance),
                                valueColor = if (selectedBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

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
private fun TrendSeriesPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrendFocusValue(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        }
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
