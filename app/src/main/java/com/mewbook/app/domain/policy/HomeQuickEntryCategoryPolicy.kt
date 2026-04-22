package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType

object HomeQuickEntryCategoryPolicy {

    fun suggest(
        categories: List<Category>,
        records: List<Record>,
        ledgerId: Long,
        type: RecordType,
        limit: Int = 6
    ): List<Category> {
        val categoriesById = categories.associateBy { it.id }
        val visibleCategoryIds = CategorySelectionPolicy
            .visibleTopLevelCategories(categories, type)
            .map(Category::id)
            .toSet()
        val usageRanking = records
            .asSequence()
            .filter { it.ledgerId == ledgerId && it.type == type }
            .mapNotNull { record ->
                val category = categoriesById[record.categoryId] ?: return@mapNotNull null
                if (category.type != type) return@mapNotNull null
                record.categoryId to record.updatedAt
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, timestamps) ->
                timestamps.size to timestamps.maxOrNull()
            }

        val recentSuggestions = usageRanking.entries
            .sortedWith(
                compareByDescending<Map.Entry<Long, Pair<Int, java.time.LocalDateTime?>>> { it.value.first }
                    .thenByDescending { it.value.second }
            )
            .mapNotNull { (categoryId, _) -> categoriesById[categoryId] }
            .filter { category -> category.id in visibleCategoryIds }

        if (recentSuggestions.isNotEmpty()) {
            return recentSuggestions.take(limit)
        }

        return CategorySelectionPolicy.visibleTopLevelCategories(categories, type)
            .asSequence()
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.id })
            .take(limit)
            .toList()
    }
}
