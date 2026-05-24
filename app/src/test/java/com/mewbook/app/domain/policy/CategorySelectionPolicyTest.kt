package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySelectionPolicyTest {

    @Test
    fun visibleCategories_includesEveryCategoryOfTheRequestedType() {
        val result = CategorySelectionPolicy.visibleCategories(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "交通", type = RecordType.EXPENSE),
                category(id = 3L, name = "工资", type = RecordType.INCOME),
                category(id = 4L, name = "公交", type = RecordType.EXPENSE)
            ),
            type = RecordType.EXPENSE
        ).map(Category::id)

        assertEquals(listOf(1L, 2L, 4L), result)
    }

    @Test
    fun recordSelectionCandidates_returnsRecordEntryExpenseCategoriesAndCustomCategories() {
        val result = CategorySelectionPolicy.recordSelectionCandidates(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "交通", type = RecordType.EXPENSE),
                category(id = 3L, name = "公交", type = RecordType.EXPENSE),
                category(id = 4L, name = "工资", type = RecordType.INCOME),
                category(id = 5L, name = "自定义支出", type = RecordType.EXPENSE, isDefault = false)
            ),
            type = RecordType.EXPENSE
        ).map(Category::id)

        assertEquals(listOf(2L, 5L), result)
    }

    @Test
    fun recordSelectionCandidates_keepsSelectedHiddenExpenseCategoryForEditing() {
        val result = CategorySelectionPolicy.recordSelectionCandidates(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "交通", type = RecordType.EXPENSE),
                category(id = 3L, name = "公交", type = RecordType.EXPENSE)
            ),
            type = RecordType.EXPENSE,
            selectedCategoryId = 3L
        ).map(Category::id)

        assertEquals(listOf(2L, 3L), result)
    }

    @Test
    fun resolvePreferredCategoryId_fallsBackToFirstVisibleCategory() {
        val categories = listOf(
            category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
            category(id = 2L, name = "交通", type = RecordType.EXPENSE),
            category(id = 3L, name = "餐饮", type = RecordType.EXPENSE)
        )

        assertEquals(
            1L,
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
    fun defaultExpenseCategories_preserveFormerNestedCategoriesAsFlatDefaults() {
        val defaultExpenseNames = DefaultCategories.expenseCategories.map(Category::name).toSet()

        assertEquals(DefaultCategories.expenseCategories.size, defaultExpenseNames.size)
        assertTrue(defaultExpenseNames.containsAll(setOf("早餐", "地铁", "打车", "房租")))
    }

    private fun category(
        id: Long,
        name: String,
        type: RecordType,
        isDefault: Boolean = true
    ) = Category(
        id = id,
        name = name,
        icon = "more_horiz",
        color = 0xFF808080,
        type = type,
        isDefault = isDefault,
        sortOrder = id.toInt()
    )
}
