package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.DefaultCategories
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CategorySelectionPolicyTest {

    @Test
    fun visibleTopLevelCategories_excludesLegacySubwayAndChildCategories() {
        val result = CategorySelectionPolicy.visibleTopLevelCategories(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "交通", type = RecordType.EXPENSE),
                category(id = 3L, name = "工资", type = RecordType.INCOME),
                category(id = 4L, name = "公交", type = RecordType.EXPENSE, parentId = 2L)
            ),
            type = RecordType.EXPENSE
        ).map(Category::id)

        assertEquals(listOf(2L), result)
    }

    @Test
    fun recordSelectionCandidates_keepsSelectedChildButHidesLegacySubway() {
        val result = CategorySelectionPolicy.recordSelectionCandidates(
            categories = listOf(
                category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
                category(id = 2L, name = "交通", type = RecordType.EXPENSE),
                category(id = 3L, name = "公交", type = RecordType.EXPENSE, parentId = 2L),
                category(id = 4L, name = "工资", type = RecordType.INCOME)
            ),
            type = RecordType.EXPENSE,
            selectedCategoryId = 3L
        ).map(Category::id)

        assertEquals(listOf(2L, 3L), result)
    }

    @Test
    fun resolvePreferredTopLevelCategoryId_fallsBackToFirstVisibleCategory() {
        val categories = listOf(
            category(id = 1L, name = "地铁", type = RecordType.EXPENSE),
            category(id = 2L, name = "交通", type = RecordType.EXPENSE),
            category(id = 3L, name = "餐饮", type = RecordType.EXPENSE)
        )

        assertEquals(
            2L,
            CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
                categories = categories,
                type = RecordType.EXPENSE,
                preferredCategoryId = 1L
            )
        )
        assertEquals(
            3L,
            CategorySelectionPolicy.resolvePreferredTopLevelCategoryId(
                categories = categories,
                type = RecordType.EXPENSE,
                preferredCategoryId = 3L
            )
        )
    }

    @Test
    fun defaultExpenseCategories_noLongerContainsTopLevelSubway() {
        assertFalse(DefaultCategories.expenseCategories.any { it.name == "地铁" })
    }

    private fun category(
        id: Long,
        name: String,
        type: RecordType,
        parentId: Long? = null
    ) = Category(
        id = id,
        name = name,
        icon = "more_horiz",
        color = 0xFF808080,
        type = type,
        isDefault = true,
        sortOrder = id.toInt(),
        parentId = parentId
    )
}
