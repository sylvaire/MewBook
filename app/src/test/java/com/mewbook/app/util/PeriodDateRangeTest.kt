package com.mewbook.app.util

import com.mewbook.app.domain.model.BudgetPeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

class PeriodDateRangeTest {

    @Test
    fun month_periodKey_isYearMonth() {
        val d = LocalDate.of(2026, 4, 15)
        assertEquals("2026-04", PeriodDateRange.periodKey(BudgetPeriodType.MONTH, d))
    }

    @Test
    fun day_periodKey_isIsoDate() {
        val d = LocalDate.of(2026, 4, 19)
        assertEquals("2026-04-19", PeriodDateRange.periodKey(BudgetPeriodType.DAY, d))
    }

    @Test
    fun week_periodKey_matchesIsoWeekFields() {
        val anchor = LocalDate.of(2026, 4, 15)
        val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val y = monday.get(IsoFields.WEEK_BASED_YEAR)
        val w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val expected = String.format("%d-W%02d", y, w)
        assertEquals(expected, PeriodDateRange.periodKey(BudgetPeriodType.WEEK, anchor))
    }

    @Test
    fun week_range_isMondayToSunday() {
        val anchor = LocalDate.of(2026, 4, 15)
        val (start, end) = PeriodDateRange.dateRange(BudgetPeriodType.WEEK, anchor)
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertEquals(6L, java.time.temporal.ChronoUnit.DAYS.between(start, end))
    }

    @Test
    fun canGoNextPeriod_falseWhenFutureWeek() {
        val today = LocalDate.of(2026, 4, 19)
        val anchor = today.minusWeeks(1)
        assertTrue(PeriodDateRange.canGoToNextPeriod(BudgetPeriodType.WEEK, anchor, today))
        assertFalse(PeriodDateRange.canGoToNextPeriod(BudgetPeriodType.WEEK, today, today))
    }

    @Test
    fun canGoNextPeriod_month_allowsAdvancingFromPreviousMonthToCurrentEvenWhenMonthEndIsAfterToday() {
        val today = LocalDate.of(2026, 4, 19)
        val marchAnchor = LocalDate.of(2026, 3, 10)
        assertTrue(PeriodDateRange.canGoToNextPeriod(BudgetPeriodType.MONTH, marchAnchor, today))
    }
}
