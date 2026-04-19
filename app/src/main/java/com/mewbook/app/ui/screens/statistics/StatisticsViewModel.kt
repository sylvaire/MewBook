package com.mewbook.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.record.GetRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class TimeRange {
    WEEK, MONTH, YEAR
}

data class StatisticsUiState(
    val timeRange: TimeRange = TimeRange.MONTH,
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val records: List<Record> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val expenseByCategory: Map<Long, Double> = emptyMap(),
    val dailyIncome: List<Double> = emptyList(),
    val dailyExpense: List<Double> = emptyList(),
    val labels: List<String> = emptyList(),
    val observedMask: List<Boolean> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getRecordsUseCase: GetRecordsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.MONTH)
    private val _anchorDate = MutableStateFlow(LocalDate.now())

    private val _records = MutableStateFlow<List<Record>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    val uiState: StateFlow<StatisticsUiState> = combine(
        _timeRange,
        _anchorDate,
        _records,
        _categories
    ) { timeRange, anchorDate, records, categories ->
        val (startDate, endDate) = getDateRange(timeRange, anchorDate)

        val filteredRecords = records.filter { record ->
            !record.date.isBefore(startDate) && !record.date.isAfter(endDate)
        }

        val totalIncome = filteredRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount }
        val totalExpense = filteredRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }

        val expenseByCategory = filteredRecords
            .filter { it.type == RecordType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, records) -> records.sumOf { it.amount } }

        val (dailyIncome, dailyExpense, labels) = when (timeRange) {
            TimeRange.WEEK -> calculateWeeklyData(filteredRecords, startDate)
            TimeRange.MONTH -> calculateMonthlyData(filteredRecords, YearMonth.from(startDate))
            TimeRange.YEAR -> calculateYearlyData(filteredRecords, startDate.year)
        }
        val observedMask = buildObservedMask(timeRange, startDate, labels.size)

        val nextAnchorDate = shiftAnchorDate(anchorDate, timeRange, 1)
        val (_, nextEndDate) = getDateRange(timeRange, nextAnchorDate)

        StatisticsUiState(
            timeRange = timeRange,
            periodStart = startDate,
            periodEnd = endDate,
            periodLabel = buildPeriodLabel(timeRange, startDate, endDate),
            canGoNext = !nextEndDate.isAfter(LocalDate.now()),
            records = filteredRecords,
            categories = categories.associateBy { it.id },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            expenseByCategory = expenseByCategory,
            dailyIncome = dailyIncome,
            dailyExpense = dailyExpense,
            labels = labels,
            observedMask = observedMask,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsUiState()
    )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getCategoriesUseCase.getAll().collect { categories ->
                _categories.update { categories }
            }
        }

        viewModelScope.launch {
            getRecordsUseCase.getAll().collect { records ->
                _records.update { records }
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.update { range }
    }

    fun previousPeriod() {
        val range = _timeRange.value
        _anchorDate.update { current -> shiftAnchorDate(current, range, -1) }
    }

    fun nextPeriod() {
        val range = _timeRange.value
        _anchorDate.update { current ->
            val candidate = shiftAnchorDate(current, range, 1)
            val (_, candidateEnd) = getDateRange(range, candidate)
            if (candidateEnd.isAfter(LocalDate.now())) current else candidate
        }
    }

    private fun shiftAnchorDate(anchorDate: LocalDate, timeRange: TimeRange, step: Int): LocalDate {
        return when (timeRange) {
            TimeRange.WEEK -> anchorDate.plusWeeks(step.toLong())
            TimeRange.MONTH -> anchorDate.plusMonths(step.toLong())
            TimeRange.YEAR -> anchorDate.plusYears(step.toLong())
        }
    }

    private fun getDateRange(timeRange: TimeRange, anchorDate: LocalDate): Pair<LocalDate, LocalDate> {
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

    private fun calculateWeeklyData(
        records: List<Record>,
        startOfWeek: LocalDate
    ): Triple<List<Double>, List<Double>, List<String>> {
        val dailyIncome = mutableListOf<Double>()
        val dailyExpense = mutableListOf<Double>()
        val labels = mutableListOf<String>()

        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayRecords = records.filter { it.date == date }

            dailyIncome.add(dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount })
            dailyExpense.add(dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount })

            labels.add(date.dayOfWeek.toChineseString())
        }

        return Triple(dailyIncome, dailyExpense, labels)
    }

    private fun calculateMonthlyData(
        records: List<Record>,
        yearMonth: YearMonth
    ): Triple<List<Double>, List<Double>, List<String>> {
        val daysInMonth = yearMonth.lengthOfMonth()
        val dailyIncome = mutableListOf<Double>()
        val dailyExpense = mutableListOf<Double>()
        val labels = mutableListOf<String>()

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)

            val dayRecords = records.filter { it.date == date }

            dailyIncome.add(dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount })
            dailyExpense.add(dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount })

            labels.add(day.toString())
        }

        return Triple(dailyIncome, dailyExpense, labels)
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

    private fun calculateYearlyData(
        records: List<Record>,
        targetYear: Int
    ): Triple<List<Double>, List<Double>, List<String>> {
        val monthlyIncome = mutableListOf<Double>()
        val monthlyExpense = mutableListOf<Double>()
        val labels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

        for (month in 1..12) {
            val monthRecords = records.filter { it.date.monthValue == month && it.date.year == targetYear }

            monthlyIncome.add(monthRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount })
            monthlyExpense.add(monthRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount })
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
        count: Int
    ): List<Boolean> {
        val today = LocalDate.now()
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
}
