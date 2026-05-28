package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType

object CategorySelectionPolicy {

    private val CurrentDefaultCategoryKeys = DefaultCategories.all
        .map { it.type to it.name }
        .toSet()

    fun visibleCategories(
        categories: List<Category>,
        type: RecordType
    ): List<Category> {
        return categories
            .asSequence()
            .filter { it.type == type }
            .filter(::isCurrentCategory)
            .sortedWith(compareBy(Category::sortOrder, Category::name, Category::id))
            .toList()
    }

    fun recordSelectionCandidates(
        categories: List<Category>,
        type: RecordType,
        selectedCategoryId: Long? = null
    ): List<Category> {
        val visible = visibleCategories(categories, type)
        if (selectedCategoryId == null || visible.any { it.id == selectedCategoryId }) {
            return visible
        }

        val selectedCategory = categories.firstOrNull {
            it.id == selectedCategoryId && it.type == type
        } ?: return visible

        return (visible + selectedCategory)
            .sortedWith(compareBy(Category::sortOrder, Category::name, Category::id))
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

    fun reorderSortUpdates(
        categories: List<Category>,
        type: RecordType,
        fromCategoryId: Long,
        toCategoryId: Long
    ): List<Pair<Long, Int>> {
        if (fromCategoryId == toCategoryId) return emptyList()

        val siblings = visibleCategories(categories, type)
        val fromIndex = siblings.indexOfFirst { it.id == fromCategoryId }
        val toIndex = siblings.indexOfFirst { it.id == toCategoryId }
        if (fromIndex == -1 || toIndex == -1) return emptyList()

        val mutable = siblings.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)

        return mutable.mapIndexed { index, category ->
            category.id to index
        }
    }

    private fun isCurrentCategory(category: Category): Boolean {
        return !category.isDefault || (category.type to category.name) in CurrentDefaultCategoryKeys
    }
}
