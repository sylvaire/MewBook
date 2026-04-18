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

    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND month = :month")
    fun getBudgetsByMonth(ledgerId: Long, month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND ledgerId = :ledgerId")
    suspend fun getBudgetByCategoryAndMonth(categoryId: Long, month: String, ledgerId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND month = :month AND ledgerId = :ledgerId")
    suspend fun getTotalBudgetByMonth(month: String, ledgerId: Long): BudgetEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM budgets WHERE month = :month AND ledgerId = :ledgerId")
    suspend fun getTotalBudgetAmountByMonth(month: String, ledgerId: Long): Double

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

    @Query("DELETE FROM budgets WHERE ledgerId = :ledgerId AND month = :month")
    suspend fun deleteBudgetsByMonth(ledgerId: Long, month: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
