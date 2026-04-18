package com.mewbook.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetWithSpending
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.repository.BudgetRepository
import com.mewbook.app.domain.repository.CategoryRepository
import com.mewbook.app.domain.repository.LedgerRepository
import com.mewbook.app.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BudgetUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val totalBudget: Budget? = null,
    val totalSpent: Double = 0.0,
    val categoryBudgets: List<BudgetWithSpending> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingBudget: Budget? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val ledgerRepository: LedgerRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val currentMonthFlow = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            combine(
                ledgerRepository.getDefaultLedgerFlow(),
                currentMonthFlow
            ) { ledger, state ->
                (ledger?.id ?: 1L) to state
            }.collectLatest { (ledgerId, currentMonth) ->
                _uiState.value = _uiState.value.copy(isLoading = true)
                val month = currentMonth.toString()

                combine(
                    budgetRepository.getBudgetsByMonth(ledgerId, month),
                    categoryRepository.getAllCategories(),
                    recordRepository.getRecordsByMonth(ledgerId, month)
                ) { budgets, categories, records ->
                    val categoryMap = categories.associateBy { it.id }
                    val totalSpent = records.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
                    val categorySpending = records
                        .filter { it.type.name == "EXPENSE" }
                        .groupBy { it.categoryId }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val budgetsWithSpending = budgets.map { budget ->
                        BudgetWithSpending(
                            budget = budget,
                            spent = if (budget.categoryId != null) {
                                categorySpending[budget.categoryId] ?: 0.0
                            } else {
                                totalSpent
                            }
                        )
                    }

                    BudgetUiState(
                        currentMonth = currentMonth,
                        totalBudget = budgets.find { it.categoryId == null },
                        totalSpent = totalSpent,
                        categoryBudgets = budgetsWithSpending,
                        categories = categoryMap,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        currentMonthFlow.value = month
        _uiState.value = _uiState.value.copy(currentMonth = month)
    }

    fun showAddDialog(categoryId: Long? = null) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingBudget = categoryId?.let { id ->
                _uiState.value.categoryBudgets.find { it.budget.categoryId == id }?.budget
            }
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingBudget = null)
    }

    fun saveBudget(categoryId: Long?, amount: Double) {
        viewModelScope.launch {
            val month = _uiState.value.currentMonth.toString()
            val ledgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L

            val budget = Budget(
                id = _uiState.value.editingBudget?.id ?: 0,
                categoryId = categoryId,
                month = month,
                amount = amount,
                ledgerId = ledgerId
            )

            if (_uiState.value.editingBudget != null) {
                budgetRepository.updateBudget(budget)
            } else {
                budgetRepository.insertBudget(budget)
            }

            hideDialog()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
