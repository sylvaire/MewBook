package com.mewbook.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.BudgetWithSpending
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.policy.BudgetCategoryBudgetPolicy
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.mewbook.app.util.PeriodDateRange

enum class BudgetDialogType {
    TOTAL,
    CATEGORY
}

data class BudgetUiState(
    val selectedPeriodType: BudgetPeriodType = BudgetPeriodType.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val totalBudget: Budget? = null,
    val totalSpent: Double = 0.0,
    val categoryBudgets: List<BudgetWithSpending> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val availableCategoryOptions: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingBudget: Budget? = null,
    val dialogType: BudgetDialogType = BudgetDialogType.TOTAL,
    val dialogCategoryId: Long? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val ledgerRepository: LedgerRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val currentPeriodTypeFlow = MutableStateFlow(BudgetPeriodType.MONTH)
    private val currentAnchorDateFlow = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            combine(
                ledgerRepository.getDefaultLedgerFlow(),
                currentPeriodTypeFlow,
                currentAnchorDateFlow
            ) { ledger, periodType, anchorDate ->
                Triple(ledger?.id ?: 1L, periodType, anchorDate)
            }.collectLatest { (ledgerId, periodType, anchorDate) ->
                _uiState.value = _uiState.value.copy(isLoading = true)
                val (periodStart, periodEnd) = PeriodDateRange.dateRange(periodType, anchorDate)
                val periodKey = PeriodDateRange.periodKey(periodType, anchorDate)

                combine(
                    budgetRepository.getBudgetsByPeriod(ledgerId, periodType, periodKey),
                    categoryRepository.getAllCategories(),
                    recordRepository.getRecordsByLedgerAndDateRange(ledgerId, periodStart, periodEnd)
                ) { budgets, categories, records ->
                    val categoryMap = categories.associateBy { it.id }
                    val totalSpent = records.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
                    val categorySpending = records
                        .filter { it.type.name == "EXPENSE" }
                        .groupBy { it.categoryId }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val budgetsWithSpending = budgets
                        .map { budget ->
                            BudgetWithSpending(
                                budget = budget,
                                spent = if (budget.categoryId != null) {
                                    categorySpending[budget.categoryId] ?: 0.0
                                } else {
                                    totalSpent
                                }
                            )
                        }
                        .sortedBy { budgetWithSpending ->
                            budgetWithSpending.budget.categoryId?.let { categoryMap[it]?.sortOrder } ?: Int.MIN_VALUE
                        }

                    BudgetUiState(
                        selectedPeriodType = periodType,
                        anchorDate = anchorDate,
                        periodLabel = PeriodDateRange.formatPeriodLabel(periodType, periodStart, periodEnd),
                        canGoNext = PeriodDateRange.canGoToNextPeriod(periodType, anchorDate),
                        totalBudget = budgets.find { it.categoryId == null },
                        totalSpent = totalSpent,
                        categoryBudgets = budgetsWithSpending,
                        categories = categoryMap,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.update { current ->
                        current.copy(
                            selectedPeriodType = state.selectedPeriodType,
                            anchorDate = state.anchorDate,
                            periodLabel = state.periodLabel,
                            canGoNext = state.canGoNext,
                            totalBudget = state.totalBudget,
                            totalSpent = state.totalSpent,
                            categoryBudgets = state.categoryBudgets.filter { it.budget.categoryId != null },
                            categories = state.categories,
                            isLoading = false,
                            availableCategoryOptions = resolveAvailableCategoryOptions(
                                categories = state.categories.values.toList(),
                                categoryBudgets = state.categoryBudgets.map(BudgetWithSpending::budget),
                                editingBudget = current.editingBudget,
                                dialogType = current.dialogType
                            ),
                            dialogCategoryId = resolveDialogCategoryId(
                                existingSelection = current.dialogCategoryId,
                                editingBudget = current.editingBudget,
                                dialogType = current.dialogType,
                                availableCategories = resolveAvailableCategoryOptions(
                                    categories = state.categories.values.toList(),
                                    categoryBudgets = state.categoryBudgets.map(BudgetWithSpending::budget),
                                    editingBudget = current.editingBudget,
                                    dialogType = current.dialogType
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectPeriodType(periodType: BudgetPeriodType) {
        currentPeriodTypeFlow.value = periodType
    }

    fun previousPeriod() {
        val periodType = currentPeriodTypeFlow.value
        currentAnchorDateFlow.value = PeriodDateRange.shiftAnchor(periodType, currentAnchorDateFlow.value, -1)
    }

    fun nextPeriod() {
        val periodType = currentPeriodTypeFlow.value
        val anchorDate = currentAnchorDateFlow.value
        if (PeriodDateRange.canGoToNextPeriod(periodType, anchorDate)) {
            currentAnchorDateFlow.value = PeriodDateRange.shiftAnchor(periodType, anchorDate, 1)
        }
    }

    fun showTotalBudgetDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingBudget = _uiState.value.totalBudget,
            dialogType = BudgetDialogType.TOTAL,
            dialogCategoryId = null,
            availableCategoryOptions = emptyList()
        )
    }

    fun showCategoryBudgetDialog(categoryId: Long? = null) {
        val editingBudget = categoryId?.let { selectedCategoryId ->
            _uiState.value.categoryBudgets.find { it.budget.categoryId == selectedCategoryId }?.budget
        }
        val availableCategoryOptions = resolveAvailableCategoryOptions(
            categories = _uiState.value.categories.values.toList(),
            categoryBudgets = _uiState.value.categoryBudgets.map(BudgetWithSpending::budget),
            editingBudget = editingBudget,
            dialogType = BudgetDialogType.CATEGORY
        )

        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingBudget = editingBudget,
            dialogType = BudgetDialogType.CATEGORY,
            dialogCategoryId = resolveDialogCategoryId(
                existingSelection = categoryId,
                editingBudget = editingBudget,
                dialogType = BudgetDialogType.CATEGORY,
                availableCategories = availableCategoryOptions
            ),
            availableCategoryOptions = availableCategoryOptions
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingBudget = null,
            dialogType = BudgetDialogType.TOTAL,
            dialogCategoryId = null,
            availableCategoryOptions = emptyList()
        )
    }

    fun updateDialogCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(dialogCategoryId = categoryId)
    }

    fun saveBudget(amount: Double) {
        viewModelScope.launch {
            val ledgerId = ledgerRepository.getDefaultLedger()?.id ?: 1L
            val periodType = _uiState.value.selectedPeriodType
            val periodKey = PeriodDateRange.periodKey(periodType, _uiState.value.anchorDate)
            val categoryId = when (_uiState.value.dialogType) {
                BudgetDialogType.TOTAL -> null
                BudgetDialogType.CATEGORY -> _uiState.value.dialogCategoryId
            }

            if (_uiState.value.dialogType == BudgetDialogType.CATEGORY && categoryId == null) {
                return@launch
            }

            val budget = Budget(
                id = _uiState.value.editingBudget?.id ?: 0,
                categoryId = categoryId,
                periodType = periodType,
                periodKey = periodKey,
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

    private fun resolveAvailableCategoryOptions(
        categories: List<Category>,
        categoryBudgets: List<Budget>,
        editingBudget: Budget?,
        dialogType: BudgetDialogType
    ): List<Category> {
        if (dialogType != BudgetDialogType.CATEGORY) {
            return emptyList()
        }
        return BudgetCategoryBudgetPolicy.availableExpenseCategories(
            categories = categories,
            budgets = categoryBudgets,
            editingBudget = editingBudget
        )
    }

    private fun resolveDialogCategoryId(
        existingSelection: Long?,
        editingBudget: Budget?,
        dialogType: BudgetDialogType,
        availableCategories: List<Category>
    ): Long? {
        if (dialogType != BudgetDialogType.CATEGORY) {
            return null
        }

        val candidate = editingBudget?.categoryId ?: existingSelection
        return when {
            candidate == null -> availableCategories.firstOrNull()?.id
            availableCategories.any { it.id == candidate } -> candidate
            else -> availableCategories.firstOrNull()?.id
        }
    }
}
