package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCategoryBudgetPolicyTest {

    @Test
    fun availableExpenseCategories_excludesIncomeAndAlreadyBudgetedCategories() {
        val result = BudgetCategoryBudgetPolicy.availableExpenseCategories(
            categories = listOf(
                category(id = 1L, type = RecordType.EXPENSE),
                category(id = 2L, type = RecordType.EXPENSE),
                category(id = 3L, type = RecordType.INCOME)
            ),
            budgets = listOf(
                budget(id = 10L, categoryId = null),
                budget(id = 11L, categoryId = 2L)
            ),
            editingBudget = null
        ).map(Category::id)

        assertEquals(listOf(1L), result)
    }

    @Test
    fun availableExpenseCategories_keepsCurrentCategoryWhenEditingExistingBudget() {
        val result = BudgetCategoryBudgetPolicy.availableExpenseCategories(
            categories = listOf(
                category(id = 1L, type = RecordType.EXPENSE),
                category(id = 2L, type = RecordType.EXPENSE)
            ),
            budgets = listOf(
                budget(id = 11L, categoryId = 2L)
            ),
            editingBudget = budget(id = 11L, categoryId = 2L)
        ).map(Category::id)

        assertEquals(listOf(1L, 2L), result)
    }

    private fun category(
        id: Long,
        type: RecordType
    ) = Category(
        id = id,
        name = "Category-$id",
        icon = "more_horiz",
        color = 0xFF808080,
        type = type,
        isDefault = true,
        sortOrder = id.toInt()
    )

    private fun budget(
        id: Long,
        categoryId: Long?
    ) = Budget(
        id = id,
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTH,
        periodKey = "2026-04",
        amount = 100.0,
        ledgerId = 1L
    )
}
