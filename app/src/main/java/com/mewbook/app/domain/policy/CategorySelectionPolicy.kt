package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType

object CategorySelectionPolicy {

    private const val LegacyHiddenExpenseCategoryName = "地铁"

    fun visibleTopLevelCategories(
        categories: List<Category>,
        type: RecordType
    ): List<Category> {
        return categories
            .asSequence()
            .filter { it.type == type }
            .filter(::isVisibleTopLevelCategory)
            .sortedWith(compareBy(Category::sortOrder, Category::name, Category::id))
            .toList()
    }

    fun recordSelectionCandidates(
        categories: List<Category>,
        type: RecordType,
        selectedCategoryId: Long
    ): List<Category> {
        return categories.filter { category ->
            category.type == type && (
                isVisibleTopLevelCategory(category) ||
                    (category.id == selectedCategoryId && category.parentId != null)
                )
        }
    }

    fun resolvePreferredTopLevelCategoryId(
        categories: List<Category>,
        type: RecordType,
        preferredCategoryId: Long? = null
    ): Long {
        val availableCategories = visibleTopLevelCategories(categories, type)
        return when {
            preferredCategoryId != null && availableCategories.any { it.id == preferredCategoryId } -> preferredCategoryId
            else -> availableCategories.firstOrNull()?.id ?: 0L
        }
    }

    private fun isVisibleTopLevelCategory(category: Category): Boolean {
        return category.parentId == null && !isHiddenLegacyExpenseCategory(category)
    }

    private fun isHiddenLegacyExpenseCategory(category: Category): Boolean {
        return category.type == RecordType.EXPENSE &&
            category.parentId == null &&
            category.name == LegacyHiddenExpenseCategoryName
    }
}
