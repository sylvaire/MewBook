package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsByMonth(ledgerId: Long, month: String): Flow<List<Budget>>
    suspend fun getBudgetByCategoryAndMonth(categoryId: Long, month: String, ledgerId: Long): Budget?
    suspend fun getTotalBudgetByMonth(month: String, ledgerId: Long): Budget?
    suspend fun getTotalBudgetAmountByMonth(month: String, ledgerId: Long): Double
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    suspend fun deleteBudgetsByMonth(ledgerId: Long, month: String)
}
