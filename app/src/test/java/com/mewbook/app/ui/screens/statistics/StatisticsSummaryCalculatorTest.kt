package com.mewbook.app.ui.screens.statistics

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StatisticsSummaryCalculatorTest {

    @Test
    fun build_filtersRecordsByActiveLedgerBeforeAggregating() {
        val result = StatisticsSummaryCalculator.build(
            timeRange = TimeRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 19),
            today = LocalDate.of(2026, 4, 19),
            activeLedgerId = 1L,
            records = listOf(
                record(
                    amount = 18.0,
                    type = RecordType.EXPENSE,
                    categoryId = 10L,
                    date = LocalDate.of(2026, 4, 10),
                    ledgerId = 1L
                ),
                record(
                    amount = 120.0,
                    type = RecordType.INCOME,
                    categoryId = 20L,
                    date = LocalDate.of(2026, 4, 12),
                    ledgerId = 1L
                ),
                record(
                    amount = 99.0,
                    type = RecordType.EXPENSE,
                    categoryId = 10L,
                    date = LocalDate.of(2026, 4, 11),
                    ledgerId = 2L
                )
            ),
            categories = listOf(
                Category(
                    id = 10L,
                    name = "餐饮",
                    icon = "restaurant",
                    color = 0xFFFF6B6B,
                    type = RecordType.EXPENSE,
                    isDefault = true,
                    sortOrder = 0
                ),
                Category(
                    id = 20L,
                    name = "工资",
                    icon = "payments",
                    color = 0xFF4CAF50,
                    type = RecordType.INCOME,
                    isDefault = true,
                    sortOrder = 0
                )
            )
        )

        assertEquals(2, result.records.size)
        assertEquals(120.0, result.totalIncome, 0.001)
        assertEquals(18.0, result.totalExpense, 0.001)
        assertEquals(18.0, result.expenseByCategory[10L] ?: 0.0, 0.001)
    }

    @Test
    fun build_groupsIncomeByCategoryForActiveLedgerOnly() {
        val result = StatisticsSummaryCalculator.build(
            timeRange = TimeRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 19),
            today = LocalDate.of(2026, 4, 19),
            activeLedgerId = 1L,
            records = listOf(
                record(
                    amount = 8000.0,
                    type = RecordType.INCOME,
                    categoryId = 20L,
                    date = LocalDate.of(2026, 4, 3),
                    ledgerId = 1L
                ),
                record(
                    amount = 1200.0,
                    type = RecordType.INCOME,
                    categoryId = 21L,
                    date = LocalDate.of(2026, 4, 8),
                    ledgerId = 1L
                ),
                record(
                    amount = 300.0,
                    type = RecordType.INCOME,
                    categoryId = 20L,
                    date = LocalDate.of(2026, 4, 15),
                    ledgerId = 1L
                ),
                record(
                    amount = 50.0,
                    type = RecordType.EXPENSE,
                    categoryId = 20L,
                    date = LocalDate.of(2026, 4, 16),
                    ledgerId = 1L
                ),
                record(
                    amount = 999.0,
                    type = RecordType.INCOME,
                    categoryId = 20L,
                    date = LocalDate.of(2026, 4, 20),
                    ledgerId = 2L
                )
            ),
            categories = listOf(
                Category(
                    id = 20L,
                    name = "工资",
                    icon = "payments",
                    color = 0xFF4CAF50,
                    type = RecordType.INCOME,
                    isDefault = true,
                    sortOrder = 0
                ),
                Category(
                    id = 21L,
                    name = "兼职",
                    icon = "work",
                    color = 0xFFA8D8EA,
                    type = RecordType.INCOME,
                    isDefault = true,
                    sortOrder = 1
                )
            )
        )

        assertEquals(8300.0, result.incomeByCategory[20L] ?: 0.0, 0.001)
        assertEquals(1200.0, result.incomeByCategory[21L] ?: 0.0, 0.001)
        assertEquals(2, result.incomeByCategory.size)
    }

    private fun record(
        amount: Double,
        type: RecordType,
        categoryId: Long,
        date: LocalDate,
        ledgerId: Long
    ) = Record(
        id = amount.toLong(),
        amount = amount,
        type = type,
        categoryId = categoryId,
        note = null,
        date = date,
        createdAt = LocalDateTime.of(2026, 4, 19, 12, 0),
        updatedAt = LocalDateTime.of(2026, 4, 19, 12, 0),
        syncId = null,
        ledgerId = ledgerId,
        accountId = null
    )
}
