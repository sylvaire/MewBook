package com.mewbook.app.util

import com.mewbook.app.domain.model.BudgetPeriodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object PeriodDateRange {

    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun dateRange(period: BudgetPeriodType, anchor: LocalDate): Pair<LocalDate, LocalDate> {
        return when (period) {
            BudgetPeriodType.MONTH -> {
                val ym = java.time.YearMonth.from(anchor)
                ym.atDay(1) to ym.atEndOfMonth()
            }
            BudgetPeriodType.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusDays(6)
            }
            BudgetPeriodType.DAY -> anchor to anchor
        }
    }

    fun shiftAnchor(period: BudgetPeriodType, anchor: LocalDate, step: Int): LocalDate {
        return when (period) {
            BudgetPeriodType.MONTH -> anchor.plusMonths(step.toLong())
            BudgetPeriodType.WEEK -> anchor.plusWeeks(step.toLong())
            BudgetPeriodType.DAY -> anchor.plusDays(step.toLong())
        }
    }

    fun periodKey(period: BudgetPeriodType, anchor: LocalDate): String {
        return when (period) {
            BudgetPeriodType.MONTH -> java.time.YearMonth.from(anchor).toString()
            BudgetPeriodType.WEEK -> weekPeriodKey(anchor)
            BudgetPeriodType.DAY -> anchor.format(dayFormatter)
        }
    }

    fun weekPeriodKey(anchor: LocalDate): String {
        val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val y = monday.get(IsoFields.WEEK_BASED_YEAR)
        val w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return String.format("%d-W%02d", y, w)
    }

    fun canGoToNextPeriod(period: BudgetPeriodType, anchor: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val candidate = shiftAnchor(period, anchor, 1)
        val (start, _) = dateRange(period, candidate)
        // 与 StatisticsViewModel 一致：用下一周期「首日」判断，避免周期末日晚于今天时误禁用（如从上月切到本月）
        return !start.isAfter(today)
    }

    fun formatPeriodLabel(period: BudgetPeriodType, start: LocalDate, end: LocalDate): String {
        return when (period) {
            BudgetPeriodType.MONTH ->
                java.time.YearMonth.from(start).format(DateTimeFormatter.ofPattern("yyyy年MM月"))
            BudgetPeriodType.WEEK ->
                "${start.format(dayFormatter)} ~ ${end.format(dayFormatter)}"
            BudgetPeriodType.DAY ->
                start.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
        }
    }
}
