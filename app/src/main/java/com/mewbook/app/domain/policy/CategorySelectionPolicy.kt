package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType

object CategorySelectionPolicy {

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
        return visibleCategories(categories, type)
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
