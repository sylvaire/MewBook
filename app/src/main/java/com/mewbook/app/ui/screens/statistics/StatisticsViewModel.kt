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
    val records: List<Record> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val expenseByCategory: Map<Long, Double> = emptyMap(),
    val dailyIncome: List<Double> = emptyList(),
    val dailyExpense: List<Double> = emptyList(),
    val labels: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getRecordsUseCase: GetRecordsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.MONTH)

    private val _records = MutableStateFlow<List<Record>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    val uiState: StateFlow<StatisticsUiState> = combine(
        _timeRange,
        _records,
        _categories
    ) { timeRange, records, categories ->
        val (startDate, endDate) = getDateRange(timeRange)

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
            TimeRange.WEEK -> calculateWeeklyData(filteredRecords)
            TimeRange.MONTH -> calculateMonthlyData(filteredRecords)
            TimeRange.YEAR -> calculateYearlyData(filteredRecords)
        }

        StatisticsUiState(
            timeRange = timeRange,
            records = filteredRecords,
            categories = categories.associateBy { it.id },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            expenseByCategory = expenseByCategory,
            dailyIncome = dailyIncome,
            dailyExpense = dailyExpense,
            labels = labels,
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

    private fun getDateRange(timeRange: TimeRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (timeRange) {
            TimeRange.WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                startOfWeek to today
            }
            TimeRange.MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                startOfMonth to today
            }
            TimeRange.YEAR -> {
                val startOfYear = today.withDayOfYear(1)
                startOfYear to today
            }
        }
    }

    private fun calculateWeeklyData(records: List<Record>): Triple<List<Double>, List<Double>, List<String>> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

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

    private fun calculateMonthlyData(records: List<Record>): Triple<List<Double>, List<Double>, List<String>> {
        val today = LocalDate.now()
        val daysInMonth = today.lengthOfMonth()

        val dailyIncome = mutableListOf<Double>()
        val dailyExpense = mutableListOf<Double>()
        val labels = mutableListOf<String>()

        for (day in 1..daysInMonth) {
            val date = today.withDayOfMonth(day)
            if (date.isAfter(today)) break

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

    private fun calculateYearlyData(records: List<Record>): Triple<List<Double>, List<Double>, List<String>> {
        val monthlyIncome = mutableListOf<Double>()
        val monthlyExpense = mutableListOf<Double>()
        val labels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

        for (month in 1..12) {
            val monthRecords = records.filter { it.date.monthValue == month && it.date.year == LocalDate.now().year }

            monthlyIncome.add(monthRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amount })
            monthlyExpense.add(monthRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount })
        }

        return Triple(monthlyIncome, monthlyExpense, labels)
    }
}
