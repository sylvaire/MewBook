package com.mewbook.app.ui.screens.statistics

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object StatisticsSummaryCalculator {

    fun build(
        timeRange: TimeRange,
        anchorDate: LocalDate,
        today: LocalDate,
        activeLedgerId: Long,
        records: List<Record>,
        categories: List<Category>
    ): StatisticsUiState {
        val ledgerRecords = records.filter { it.ledgerId == activeLedgerId }
        val (startDate, endDate) = dateRange(timeRange, anchorDate)
        val filteredRecords = ledgerRecords.filter { record ->
            !record.date.isBefore(startDate) && !record.date.isAfter(endDate)
        }

        val totalIncome = filteredRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
        val totalExpense = filteredRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
        val incomeByCategory = filteredRecords
            .filter { it.type == RecordType.INCOME }
            .groupBy { it.categoryId }
            .mapValues { (_, groupedRecords) -> groupedRecords.sumOf { it.amount } }
        val expenseByCategory = filteredRecords
            .filter { it.type == RecordType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, groupedRecords) -> groupedRecords.sumOf { it.amount } }

        val (dailyIncome, dailyExpense, labels) = when (timeRange) {
            TimeRange.WEEK -> weeklyData(filteredRecords, startDate)
            TimeRange.MONTH -> monthlyData(filteredRecords, YearMonth.from(startDate))
            TimeRange.YEAR -> yearlyData(filteredRecords, startDate.year)
        }
        val observedMask = buildObservedMask(timeRange, startDate, today, labels.size)
        val canGoNext = canGoNextPeriod(timeRange, anchorDate, today)

        return StatisticsUiState(
            timeRange = timeRange,
            periodStart = startDate,
            periodEnd = endDate,
            periodLabel = buildPeriodLabel(timeRange, startDate, endDate),
            canGoNext = canGoNext,
            records = filteredRecords,
            categories = categories.associateBy { it.id },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            incomeByCategory = incomeByCategory,
            expenseByCategory = expenseByCategory,
            dailyIncome = dailyIncome,
            dailyExpense = dailyExpense,
            labels = labels,
            observedMask = observedMask,
            isLoading = false
        )
    }

    fun shiftAnchorDate(anchorDate: LocalDate, timeRange: TimeRange, step: Int): LocalDate {
        return when (timeRange) {
            TimeRange.WEEK -> anchorDate.plusWeeks(step.toLong())
            TimeRange.MONTH -> anchorDate.plusMonths(step.toLong())
            TimeRange.YEAR -> anchorDate.plusYears(step.toLong())
        }
    }

    fun canGoNextPeriod(timeRange: TimeRange, anchorDate: LocalDate, today: LocalDate): Boolean {
        val candidate = shiftAnchorDate(anchorDate, timeRange, 1)
        val (candidateStart, _) = dateRange(timeRange, candidate)
        return !candidateStart.isAfter(today)
    }

    private fun dateRange(timeRange: TimeRange, anchorDate: LocalDate): Pair<LocalDate, LocalDate> {
        return when (timeRange) {
            TimeRange.WEEK -> {
                val startOfWeek = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                startOfWeek to startOfWeek.plusDays(6)
            }

            TimeRange.MONTH -> {
                val targetMonth = YearMonth.from(anchorDate)
                targetMonth.atDay(1) to targetMonth.atEndOfMonth()
            }

            TimeRange.YEAR -> {
                val startOfYear = anchorDate.withDayOfYear(1)
                startOfYear to anchorDate.with(TemporalAdjusters.lastDayOfYear())
            }
        }
    }

    private fun weeklyData(
        records: List<Record>,
        startOfWeek: LocalDate
    ): Triple<List<Double>, List<Double>, List<String>> {
        val dailyIncome = mutableListOf<Double>()
        val dailyExpense = mutableListOf<Double>()
        val labels = mutableListOf<String>()

        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayRecords = records.filter { it.date == date }
            dailyIncome += dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
            dailyExpense += dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
            labels += date.dayOfWeek.toChineseString()
        }

        return Triple(dailyIncome, dailyExpense, labels)
    }

    private fun monthlyData(
        records: List<Record>,
        yearMonth: YearMonth
    ): Triple<List<Double>, List<Double>, List<String>> {
        val dailyIncome = mutableListOf<Double>()
        val dailyExpense = mutableListOf<Double>()
        val labels = mutableListOf<String>()

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dayRecords = records.filter { it.date == date }
            dailyIncome += dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
            dailyExpense += dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
            labels += day.toString()
        }

        return Triple(dailyIncome, dailyExpense, labels)
    }

    private fun yearlyData(
        records: List<Record>,
        targetYear: Int
    ): Triple<List<Double>, List<Double>, List<String>> {
        val monthlyIncome = mutableListOf<Double>()
        val monthlyExpense = mutableListOf<Double>()
        val labels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

        for (month in 1..12) {
            val monthRecords = records.filter { it.date.monthValue == month && it.date.year == targetYear }
            monthlyIncome += monthRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
            monthlyExpense += monthRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
        }

        return Triple(monthlyIncome, monthlyExpense, labels)
    }

    private fun buildPeriodLabel(
        timeRange: TimeRange,
        startDate: LocalDate,
        endDate: LocalDate
    ): String {
        return when (timeRange) {
            TimeRange.WEEK -> {
                if (startDate.year == endDate.year) {
                    "${startDate.monthValue}月${startDate.dayOfMonth}日 - ${endDate.monthValue}月${endDate.dayOfMonth}日"
                } else {
                    "${startDate.year}年${startDate.monthValue}月${startDate.dayOfMonth}日 - ${endDate.year}年${endDate.monthValue}月${endDate.dayOfMonth}日"
                }
            }

            TimeRange.MONTH -> "${startDate.year}年${startDate.monthValue}月"
            TimeRange.YEAR -> "${startDate.year}年"
        }
    }

    private fun buildObservedMask(
        timeRange: TimeRange,
        startDate: LocalDate,
        today: LocalDate,
        count: Int
    ): List<Boolean> {
        return when (timeRange) {
            TimeRange.WEEK -> List(count) { index ->
                !startDate.plusDays(index.toLong()).isAfter(today)
            }

            TimeRange.MONTH -> {
                val targetMonth = YearMonth.from(startDate)
                List(count) { index ->
                    !targetMonth.atDay(index + 1).isAfter(today)
                }
            }

            TimeRange.YEAR -> List(count) { index ->
                if (startDate.year < today.year) {
                    true
                } else {
                    index + 1 <= today.monthValue
                }
            }
        }
    }

    private fun DayOfWeek.toChineseString(): String = when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
