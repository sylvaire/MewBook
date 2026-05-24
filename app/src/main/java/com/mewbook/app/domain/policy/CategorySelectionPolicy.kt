package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType

object CategorySelectionPolicy {

    private val RecordEntryExpenseCategoryNames = DefaultCategories.recordEntryExpenseCategories
        .map(Category::name)
        .toSet()
    private val LegacyExpenseSubCategoryNames = DefaultCategories.legacyExpenseSubCategoryNames

    fun visibleCategories(
        categories: List<Category>,
        type: RecordType
    ): List<Category> {
        return categories
            .asSequence()
            .filter { it.type == type }
            .sortedWith(compareBy(Category::sortOrder, Category::name, Category::id))
            .toList()
    }

    fun recordSelectionCandidates(
        categories: List<Category>,
        type: RecordType,
        selectedCategoryId: Long? = null
    ): List<Category> {
        if (type != RecordType.EXPENSE) {
            return visibleCategories(categories, type)
        }

        return categories
            .asSequence()
            .filter { it.type == RecordType.EXPENSE }
            .filter { category ->
                category.name in RecordEntryExpenseCategoryNames ||
                    (!category.isDefault && category.name !in LegacyExpenseSubCategoryNames) ||
                    category.id == selectedCategoryId
            }
            .sortedWith(compareBy(Category::sortOrder, Category::name, Category::id))
            .toList()
    }

    fun resolvePreferredCategoryId(
        categories: List<Category>,
        type: RecordType,
        preferredCategoryId: Long? = null
    ): Long {
        val availableCategories = visibleCategories(categories, type)
        return when {
            preferredCategoryId != null && availableCategories.any { it.id == preferredCategoryId } -> preferredCategoryId
            else -> availableCategories.firstOrNull()?.id ?: 0L
        }
    }
}
