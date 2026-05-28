package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Budget
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType

object BudgetCategoryBudgetPolicy {

    fun availableExpenseCategories(
        categories: List<Category>,
        budgets: List<Budget>,
        editingBudget: Budget?
    ): List<Category> {
        val reservedCategoryIds = budgets
            .asSequence()
            .filter { it.categoryId != null }
            .mapNotNull(Budget::categoryId)
            .toMutableSet()

        editingBudget?.categoryId?.let(reservedCategoryIds::remove)

        return CategorySelectionPolicy.visibleCategories(categories, RecordType.EXPENSE)
            .filter { it.id !in reservedCategoryIds }
    }
}
