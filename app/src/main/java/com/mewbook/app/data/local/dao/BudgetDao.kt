package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mewbook.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND periodType = :periodType AND month = :periodKey")
    fun getBudgetsByPeriod(ledgerId: Long, periodType: String, periodKey: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND periodType = :periodType AND month = :periodKey AND ledgerId = :ledgerId")
    suspend fun getBudgetByCategoryAndPeriod(
        categoryId: Long,
        periodType: String,
        periodKey: String,
        ledgerId: Long
    ): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND periodType = :periodType AND month = :periodKey AND ledgerId = :ledgerId")
    suspend fun getTotalBudgetByPeriod(periodType: String, periodKey: String, ledgerId: Long): BudgetEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM budgets WHERE periodType = :periodType AND month = :periodKey AND ledgerId = :ledgerId")
    suspend fun getTotalBudgetAmountByPeriod(periodType: String, periodKey: String, ledgerId: Long): Double

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE ledgerId = :ledgerId AND periodType = :periodType AND month = :periodKey")
    suspend fun deleteBudgetsByPeriod(ledgerId: Long, periodType: String, periodKey: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
