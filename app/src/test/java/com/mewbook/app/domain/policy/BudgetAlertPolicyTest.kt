package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.BudgetWithSpending
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetAlertPolicyTest {

    @Test
    fun createAlerts_marksBudgetThresholdsAtFiftyEightyAndOneHundred() {
        val alerts = BudgetAlertPolicy.createAlerts(
            periodType = BudgetPeriodType.MONTH,
            periodStart = LocalDate.of(2026, 5, 1),
            periodEnd = LocalDate.of(2026, 5, 31),
            today = LocalDate.of(2026, 5, 20),
            totalBudget = BudgetWithSpending(budget(id = 1L, categoryId = null, amount = 1000.0), spent = 520.0),
            categoryBudgets = listOf(
                BudgetWithSpending(budget(id = 2L, categoryId = 10L, amount = 300.0), spent = 240.0),
                BudgetWithSpending(budget(id = 3L, categoryId = 11L, amount = 100.0), spent = 120.0)
            ),
            categories = mapOf(
                10L to category(id = 10L, name = "餐饮"),
                11L to category(id = 11L, name = "交通")
            ),
            previousCategorySpending = emptyMap()
        )

        val thresholdAlerts = alerts.filter { it.kind == BudgetAlertKind.THRESHOLD }

        assertEquals(BudgetAlertLevel.DANGER, thresholdAlerts.first { it.categoryId == 11L }.level)
        assertEquals(BudgetAlertLevel.CAUTION, thresholdAlerts.first { it.categoryId == 10L }.level)
        assertEquals(BudgetAlertLevel.INFO, thresholdAlerts.first { it.categoryId == null }.level)
    }

    @Test
    fun createAlerts_predictsMonthEndOverspendFromDailyAverage() {
        val alerts = BudgetAlertPolicy.createAlerts(
            periodType = BudgetPeriodType.MONTH,
            periodStart = LocalDate.of(2026, 5, 1),
            periodEnd = LocalDate.of(2026, 5, 31),
            today = LocalDate.of(2026, 5, 10),
            totalBudget = BudgetWithSpending(budget(id = 1L, categoryId = null, amount = 1000.0), spent = 400.0),
            categoryBudgets = emptyList(),
            categories = emptyMap(),
            previousCategorySpending = emptyMap()
        )

        val projection = alerts.first { it.kind == BudgetAlertKind.PROJECTION }

        assertEquals(BudgetAlertLevel.CAUTION, projection.level)
        assertTrue(projection.description.contains("预计月底"))
        assertTrue(projection.description.contains("1240"))
    }

    @Test
    fun createAlerts_flagsCategorySpendSpikeAgainstPreviousPeriod() {
        val alerts = BudgetAlertPolicy.createAlerts(
            periodType = BudgetPeriodType.WEEK,
            periodStart = LocalDate.of(2026, 5, 18),
            periodEnd = LocalDate.of(2026, 5, 24),
            today = LocalDate.of(2026, 5, 20),
            totalBudget = null,
            categoryBudgets = listOf(
                BudgetWithSpending(budget(id = 2L, categoryId = 10L, amount = 300.0), spent = 95.0)
            ),
            categories = mapOf(10L to category(id = 10L, name = "餐饮")),
            previousCategorySpending = mapOf(10L to 30.0)
        )

        val anomaly = alerts.first { it.kind == BudgetAlertKind.ANOMALY }

        assertEquals(10L, anomaly.categoryId)
        assertEquals(BudgetAlertLevel.CAUTION, anomaly.level)
        assertTrue(anomaly.description.contains("上周期"))
    }

    @Test
    fun createAlerts_suppressesPredictionAndAnomalyWhenSamplesAreInsufficient() {
        val alerts = BudgetAlertPolicy.createAlerts(
            periodType = BudgetPeriodType.MONTH,
            periodStart = LocalDate.of(2026, 5, 1),
            periodEnd = LocalDate.of(2026, 5, 31),
            today = LocalDate.of(2026, 5, 1),
            totalBudget = BudgetWithSpending(budget(id = 1L, categoryId = null, amount = 1000.0), spent = 0.0),
            categoryBudgets = listOf(
                BudgetWithSpending(budget(id = 2L, categoryId = 10L, amount = 300.0), spent = 95.0)
            ),
            categories = mapOf(10L to category(id = 10L, name = "餐饮")),
            previousCategorySpending = mapOf(10L to 0.0)
        )

        assertFalse(alerts.any { it.kind == BudgetAlertKind.PROJECTION })
        assertFalse(alerts.any { it.kind == BudgetAlertKind.ANOMALY })
    }

    private fun budget(
        id: Long,
        categoryId: Long?,
        amount: Double
    ) = Budget(
        id = id,
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTH,
        periodKey = "2026-05",
        amount = amount,
        ledgerId = 1L
    )

    private fun category(
        id: Long,
        name: String
    ) = Category(
        id = id,
        name = name,
        icon = "more_horiz",
        color = 0xFF808080,
        type = RecordType.EXPENSE,
        isDefault = true,
        sortOrder = id.toInt()
    )
}
