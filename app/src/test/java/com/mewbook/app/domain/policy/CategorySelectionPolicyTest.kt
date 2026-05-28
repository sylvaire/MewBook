package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySelectionPolicyTest {

    @Test
    fun visibleCategories_excludesRetiredDefaultSecondaryCategories() {
        val result = CategorySelectionPolicy.visibleCategories(
            categories = listOf(
                category(id = 1L, name = "早餐", type = RecordType.EXPENSE),
                category(id = 2L, name = "餐饮", type = RecordType.EXPENSE),
                category(id = 3L, name = "工资", type = RecordType.INCOME),
                category(id = 4L, name = "自定义早餐", type = RecordType.EXPENSE, isDefault = false)
            ),
            type = RecordType.EXPENSE
        ).map(Category::id)

        assertEquals(listOf(2L, 4L), result)
    }

    @Test
    fun recordSelectionCandidates_returnsCurrentDefaultsAndCustomCategories() {
        val result = CategorySelectionPolicy.recordSelectionCandidates(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "餐饮", type = RecordType.EXPENSE),
                category(id = 3L, name = "公交", type = RecordType.EXPENSE),
                category(id = 4L, name = "工资", type = RecordType.INCOME),
                category(id = 5L, name = "自定义支出", type = RecordType.EXPENSE, isDefault = false)
            ),
            type = RecordType.EXPENSE
        ).map(Category::id)

        assertEquals(listOf(2L, 5L), result)
    }

    @Test
    fun recordSelectionCandidates_keepsSelectedRetiredCategoryForEditing() {
        val result = CategorySelectionPolicy.recordSelectionCandidates(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "餐饮", type = RecordType.EXPENSE),
                category(id = 3L, name = "公交", type = RecordType.EXPENSE)
            ),
            type = RecordType.EXPENSE,
            selectedCategoryId = 3L
        ).map(Category::id)

        assertEquals(listOf(2L, 3L), result)
    }

    @Test
    fun recordSelectionCandidates_matchCategoryManagementForNewRecords() {
        val categories = listOf(
            category(id = 30L, name = "购物", type = RecordType.EXPENSE, sortOrder = 1),
            category(id = 10L, name = "餐饮", type = RecordType.EXPENSE, sortOrder = 0),
            category(id = 20L, name = "交通", type = RecordType.EXPENSE, sortOrder = 0),
            category(id = 40L, name = "工资", type = RecordType.INCOME, sortOrder = 0),
            category(id = 50L, name = "地铁", type = RecordType.EXPENSE, isDefault = false, sortOrder = 0)
        )

        assertEquals(
            CategorySelectionPolicy.visibleCategories(categories, RecordType.EXPENSE),
            CategorySelectionPolicy.recordSelectionCandidates(categories, RecordType.EXPENSE)
        )
    }

    @Test
    fun reorderSortUpdates_usesCategoryIdsInsteadOfLazyColumnAbsoluteIndexes() {
        val updates = CategorySelectionPolicy.reorderSortUpdates(
            categories = listOf(
                category(id = 10L, name = "餐饮", type = RecordType.EXPENSE),
                category(id = 20L, name = "交通", type = RecordType.EXPENSE),
                category(id = 30L, name = "工资", type = RecordType.INCOME),
                category(id = 40L, name = "购物", type = RecordType.EXPENSE)
            ),
            type = RecordType.EXPENSE,
            fromCategoryId = 10L,
            toCategoryId = 40L
        )

        assertEquals(listOf(20L to 0, 40L to 1, 10L to 2), updates)
    }

    @Test
    fun reorderSortUpdates_ignoresIdsOutsideTheCurrentType() {
        val updates = CategorySelectionPolicy.reorderSortUpdates(
            categories = listOf(
                category(id = 10L, name = "餐饮", type = RecordType.EXPENSE),
                category(id = 20L, name = "工资", type = RecordType.INCOME),
                category(id = 30L, name = "购物", type = RecordType.EXPENSE)
            ),
            type = RecordType.EXPENSE,
            fromCategoryId = 10L,
            toCategoryId = 20L
        )

        assertEquals(emptyList<Pair<Long, Int>>(), updates)
    }

    @Test
    fun resolvePreferredCategoryId_fallsBackToFirstVisibleCategory() {
        val categories = listOf(
            category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
            category(id = 2L, name = "交通", type = RecordType.EXPENSE),
            category(id = 3L, name = "餐饮", type = RecordType.EXPENSE)
        )

        assertEquals(
            2L,
            CategorySelectionPolicy.resolvePreferredCategoryId(
                categories = categories,
                type = RecordType.EXPENSE,
                preferredCategoryId = 1L
            )
        )
        assertEquals(
            3L,
            CategorySelectionPolicy.resolvePreferredCategoryId(
                categories = categories,
                type = RecordType.EXPENSE,
                preferredCategoryId = 3L
            )
        )
    }

    @Test
    fun defaultCategories_areAllFlatCategories() {
        assertEquals(DefaultCategories.expenseCategories + DefaultCategories.incomeCategories, DefaultCategories.all)
    }

    @Test
    fun defaultExpenseCategories_removeFormerSecondaryCategoryDefaults() {
        val defaultExpenseNames = DefaultCategories.expenseCategories.map(Category::name).toSet()

        assertEquals(DefaultCategories.expenseCategories.size, defaultExpenseNames.size)
        assertTrue(defaultExpenseNames.containsAll(setOf("餐饮", "交通", "居住")))
        listOf("早餐", "地铁", "打车", "房租").forEach { retiredName ->
            assertFalse(defaultExpenseNames.contains(retiredName))
        }
    }

    private fun category(
        id: Long,
        name: String,
        type: RecordType,
        isDefault: Boolean = true,
        sortOrder: Int = id.toInt()
    ) = Category(
        id = id,
        name = name,
        icon = "more_horiz",
        color = 0xFF808080,
        type = type,
        isDefault = isDefault,
        sortOrder = sortOrder
    )
}
