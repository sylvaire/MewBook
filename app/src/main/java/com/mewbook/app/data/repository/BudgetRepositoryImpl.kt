package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.BudgetDao
import com.mewbook.app.data.local.entity.BudgetEntity
import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsByPeriod(ledgerId: Long, periodType: BudgetPeriodType, periodKey: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByPeriod(ledgerId, periodType.name, periodKey).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBudgetByCategoryAndPeriod(
        categoryId: Long,
        periodType: BudgetPeriodType,
        periodKey: String,
        ledgerId: Long
    ): Budget? {
        return budgetDao.getBudgetByCategoryAndPeriod(categoryId, periodType.name, periodKey, ledgerId)?.toDomain()
    }

    override suspend fun getTotalBudgetByPeriod(periodType: BudgetPeriodType, periodKey: String, ledgerId: Long): Budget? {
        return budgetDao.getTotalBudgetByPeriod(periodType.name, periodKey, ledgerId)?.toDomain()
    }

    override suspend fun getTotalBudgetAmountByPeriod(periodType: BudgetPeriodType, periodKey: String, ledgerId: Long): Double {
        return budgetDao.getTotalBudgetAmountByPeriod(periodType.name, periodKey, ledgerId)
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

    override suspend fun deleteBudgetsByPeriod(ledgerId: Long, periodType: BudgetPeriodType, periodKey: String) {
        budgetDao.deleteBudgetsByPeriod(ledgerId, periodType.name, periodKey)
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            periodType = BudgetPeriodType.valueOf(periodType),
            periodKey = periodKey,
            amount = amount,
            ledgerId = ledgerId
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            id = id,
            categoryId = categoryId,
            periodKey = periodKey,
            periodType = periodType.name,
            amount = amount,
            ledgerId = ledgerId
        )
    }
}
