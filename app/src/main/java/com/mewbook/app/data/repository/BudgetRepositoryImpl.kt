package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.BudgetDao
import com.mewbook.app.data.local.entity.BudgetEntity
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsByMonth(ledgerId: Long, month: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByMonth(ledgerId, month).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBudgetByCategoryAndMonth(categoryId: Long, month: String, ledgerId: Long): Budget? {
        return budgetDao.getBudgetByCategoryAndMonth(categoryId, month, ledgerId)?.toDomain()
    }

    override suspend fun getTotalBudgetByMonth(month: String, ledgerId: Long): Budget? {
        return budgetDao.getTotalBudgetByMonth(month, ledgerId)?.toDomain()
    }

    override suspend fun getTotalBudgetAmountByMonth(month: String, ledgerId: Long): Double {
        return budgetDao.getTotalBudgetAmountByMonth(month, ledgerId)
    }

    override suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget.toEntity())
    }

    override suspend fun deleteBudgetsByMonth(ledgerId: Long, month: String) {
        budgetDao.deleteBudgetsByMonth(ledgerId, month)
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            month = month,
            amount = amount,
            ledgerId = ledgerId
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            id = id,
            categoryId = categoryId,
            month = month,
            amount = amount,
            ledgerId = ledgerId
        )
    }
}
