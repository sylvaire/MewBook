package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.BudgetWithSpending
import com.mewbook.app.domain.model.Category
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class BudgetAlertKind {
    THRESHOLD,
    PROJECTION,
    ANOMALY
}

enum class BudgetAlertLevel {
    INFO,
    CAUTION,
    DANGER
}

data class BudgetAlert(
    val id: String,
    val kind: BudgetAlertKind,
    val level: BudgetAlertLevel,
    val title: String,
    val description: String,
    val categoryId: Long?,
    val progressRatio: Double?,
    val sortPriority: Int
)

object BudgetAlertPolicy {
    private const val DEFAULT_MAX_ALERTS = 4
    private const val ANOMALY_MULTIPLIER = 1.5
    private const val MIN_ANOMALY_DELTA = 20.0

    fun createAlerts(
        periodType: BudgetPeriodType,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
        totalBudget: BudgetWithSpending?,
        categoryBudgets: List<BudgetWithSpending>,
        categories: Map<Long, Category>,
        previousCategorySpending: Map<Long, Double>,
        maxAlerts: Int = DEFAULT_MAX_ALERTS
    ): List<BudgetAlert> {
        if (maxAlerts <= 0) return emptyList()

        val alerts = buildList {
            totalBudget?.let { budget ->
                thresholdAlert(
                    id = "threshold-total-${budget.budget.id}",
                    label = "总预算",
                    budget = budget,
                    categoryId = null
                )?.let(::add)
            }

            categoryBudgets.forEach { budget ->
                val categoryId = budget.budget.categoryId ?: return@forEach
                thresholdAlert(
                    id = "threshold-category-$categoryId-${budget.budget.id}",
                    label = categories[categoryId]?.name ?: "分类预算",
                    budget = budget,
                    categoryId = categoryId
                )?.let(::add)
            }

            projectionAlert(
                periodType = periodType,
                periodStart = periodStart,
                periodEnd = periodEnd,
                today = today,
                totalBudget = totalBudget
            )?.let(::add)

            categoryBudgets.forEach { budget ->
                val categoryId = budget.budget.categoryId ?: return@forEach
                anomalyAlert(
                    categoryId = categoryId,
                    label = categories[categoryId]?.name ?: "分类预算",
                    currentSpent = budget.spent,
                    previousSpent = previousCategorySpending[categoryId],
                    periodType = periodType
                )?.let(::add)
            }
        }

        return alerts
            .distinctBy(BudgetAlert::id)
            .sortedWith(
                compareBy<BudgetAlert> { it.sortPriority }
                    .thenByDescending { it.progressRatio ?: 0.0 }
                    .thenBy { it.title }
            )
            .take(maxAlerts)
    }

    private fun thresholdAlert(
        id: String,
        label: String,
        budget: BudgetWithSpending,
        categoryId: Long?
    ): BudgetAlert? {
        val amount = budget.budget.amount
        if (amount <= 0.0 || budget.spent <= 0.0) return null

        val ratio = budget.spent / amount
        val level = when {
            ratio >= 1.0 -> BudgetAlertLevel.DANGER
            ratio >= 0.8 -> BudgetAlertLevel.CAUTION
            ratio >= 0.5 -> BudgetAlertLevel.INFO
            else -> return null
        }
        val title = when (level) {
            BudgetAlertLevel.DANGER -> "$label 已超支"
            BudgetAlertLevel.CAUTION -> "$label 接近上限"
            BudgetAlertLevel.INFO -> "$label 已用过半"
        }

        return BudgetAlert(
            id = id,
            kind = BudgetAlertKind.THRESHOLD,
            level = level,
            title = title,
            description = "已用 ${formatPercent(ratio)}，已花 ${formatWhole(budget.spent)} / ${formatWhole(amount)}",
            categoryId = categoryId,
            progressRatio = ratio,
            sortPriority = when (level) {
                BudgetAlertLevel.DANGER -> 0
                BudgetAlertLevel.CAUTION -> 3
                BudgetAlertLevel.INFO -> 4
            }
        )
    }

    private fun projectionAlert(
        periodType: BudgetPeriodType,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
        totalBudget: BudgetWithSpending?
    ): BudgetAlert? {
        val budget = totalBudget ?: return null
        if (periodType != BudgetPeriodType.MONTH) return null
        if (today.isBefore(periodStart) || today.isAfter(periodEnd)) return null
        if (budget.budget.amount <= 0.0 || budget.spent <= 0.0 || budget.spent >= budget.budget.amount) return null

        val elapsedDays = ChronoUnit.DAYS.between(periodStart, today).toInt() + 1
        val totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1
        if (elapsedDays <= 0 || totalDays <= 0) return null

        val dailyAverage = budget.spent / elapsedDays
        val projectedSpent = dailyAverage * totalDays
        if (projectedSpent <= budget.budget.amount) return null

        return BudgetAlert(
            id = "projection-total-${budget.budget.id}-${budget.budget.periodKey}",
            kind = BudgetAlertKind.PROJECTION,
            level = BudgetAlertLevel.CAUTION,
            title = "本月可能超支",
            description = "按当前日均 ${formatWhole(dailyAverage)}，预计月底 ${formatWhole(projectedSpent)}，超过预算 ${formatWhole(projectedSpent - budget.budget.amount)}",
            categoryId = null,
            progressRatio = projectedSpent / budget.budget.amount,
            sortPriority = 1
        )
    }

    private fun anomalyAlert(
        categoryId: Long,
        label: String,
        currentSpent: Double,
        previousSpent: Double?,
        periodType: BudgetPeriodType
    ): BudgetAlert? {
        val previous = previousSpent ?: return null
        if (periodType == BudgetPeriodType.DAY) return null
        if (currentSpent <= 0.0 || previous <= 0.0) return null
        if (currentSpent - previous < MIN_ANOMALY_DELTA) return null
        if (currentSpent < previous * ANOMALY_MULTIPLIER) return null

        val increaseRatio = (currentSpent - previous) / previous
        return BudgetAlert(
            id = "anomaly-category-$categoryId",
            kind = BudgetAlertKind.ANOMALY,
            level = BudgetAlertLevel.CAUTION,
            title = "$label 支出波动",
            description = "本周期已花 ${formatWhole(currentSpent)}，比上周期 ${formatWhole(previous)} 增加 ${formatPercent(increaseRatio)}",
            categoryId = categoryId,
            progressRatio = increaseRatio,
            sortPriority = 2
        )
    }

    private fun formatPercent(value: Double): String {
        return "${(value * 100).roundToInt()}%"
    }

    private fun formatWhole(value: Double): String {
        return value.roundToInt().toString()
    }
}
