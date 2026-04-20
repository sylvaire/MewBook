package com.mewbook.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.record.GetRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
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
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.MONTH)
    private val _anchorDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<StatisticsUiState> = combine(
        _timeRange,
        _anchorDate,
        getRecordsUseCase.getAll(),
        getCategoriesUseCase.getAll(),
        ledgerRepository.getDefaultLedgerFlow()
    ) { timeRange, anchorDate, records, categories, activeLedger ->
        StatisticsSummaryCalculator.build(
            timeRange = timeRange,
            anchorDate = anchorDate,
            today = LocalDate.now(),
            activeLedgerId = activeLedger?.id ?: 1L,
            records = records,
            categories = categories
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsUiState(isLoading = true)
    )

    fun setTimeRange(range: TimeRange) {
        _timeRange.update { range }
    }

    fun previousPeriod() {
        val range = _timeRange.value
        _anchorDate.update { current -> StatisticsSummaryCalculator.shiftAnchorDate(current, range, -1) }
    }

    fun nextPeriod() {
        val range = _timeRange.value
        val today = LocalDate.now()
        _anchorDate.update { current ->
            val candidate = StatisticsSummaryCalculator.shiftAnchorDate(current, range, 1)
            if (StatisticsSummaryCalculator.canGoNextPeriod(range, current, today)) candidate else current
        }
    }
}
