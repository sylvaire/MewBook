package com.mewbook.app.ui.screens.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.usecase.category.GetCategoriesUseCase
import com.mewbook.app.domain.usecase.record.GetRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CategoryExpenseDetailUiState(
    val categoryName: String = "",
    val categoryIcon: String = "more_horiz",
    val categoryColor: Long = 0xFF808080,
    val periodSubtitle: String = "",
    val records: List<Record> = emptyList(),
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoryExpenseDetailViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<Long>("categoryId")
        ?: error("categoryId required")
    private val periodStart: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("startEpoch") ?: error("startEpoch required")
    )
    private val periodEnd: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.get<Long>("endEpoch") ?: error("endEpoch required")
    )

    val uiState: StateFlow<CategoryExpenseDetailUiState> = combine(
        getRecordsUseCase.getExpenseByCategoryAndDateRange(categoryId, periodStart, periodEnd),
        getCategoriesUseCase.getAll()
    ) { records, categories ->
        val cat = categories.find { it.id == categoryId }
        val name = cat?.name ?: "未知"
        val subtitle = formatPeriodSubtitle(periodStart, periodEnd)
        CategoryExpenseDetailUiState(
            categoryName = name,
            categoryIcon = cat?.icon ?: "more_horiz",
            categoryColor = cat?.color ?: 0xFF808080,
            periodSubtitle = subtitle,
            records = records,
            totalExpense = records.sumOf { it.amount },
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CategoryExpenseDetailUiState(isLoading = true)
    )

    private fun formatPeriodSubtitle(start: LocalDate, end: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        return if (start == end) {
            start.format(fmt)
        } else {
            "${start.format(fmt)} — ${end.format(fmt)}"
        }
    }
}
