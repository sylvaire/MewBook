package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsByPeriod(ledgerId: Long, periodType: BudgetPeriodType, periodKey: String): Flow<List<Budget>>
    suspend fun getBudgetByCategoryAndPeriod(
        categoryId: Long,
        periodType: BudgetPeriodType,
        periodKey: String,
        ledgerId: Long
    ): Budget?
    suspend fun getTotalBudgetByPeriod(periodType: BudgetPeriodType, periodKey: String, ledgerId: Long): Budget?
    suspend fun getTotalBudgetAmountByPeriod(periodType: BudgetPeriodType, periodKey: String, ledgerId: Long): Double
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    suspend fun deleteBudgetsByPeriod(ledgerId: Long, periodType: BudgetPeriodType, periodKey: String)
}
