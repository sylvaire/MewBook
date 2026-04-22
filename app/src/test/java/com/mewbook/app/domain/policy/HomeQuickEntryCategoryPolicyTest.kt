package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HomeQuickEntryCategoryPolicyTest {

    @Test
    fun suggest_prioritizesRecentCategoriesWithinLedgerAndType() {
        val breakfast = Category(
            id = 1L,
            name = "早餐",
            icon = "restaurant",
            color = 0xFFFF6B6BL,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 0
        )
        val lunch = Category(
            id = 2L,
            name = "午餐",
            icon = "restaurant",
            color = 0xFFFF6B6BL,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 1
        )
        val salary = Category(
            id = 3L,
            name = "工资",
            icon = "payments",
            color = 0xFF4CAF50,
            type = RecordType.INCOME,
            isDefault = true,
            sortOrder = 0
        )
        val now = LocalDateTime.of(2026, 4, 21, 10, 0)
        val records = listOf(
            record(categoryId = breakfast.id, ledgerId = 1L, type = RecordType.EXPENSE, updatedAt = now.minusHours(1)),
            record(categoryId = breakfast.id, ledgerId = 1L, type = RecordType.EXPENSE, updatedAt = now.minusDays(1)),
            record(categoryId = lunch.id, ledgerId = 1L, type = RecordType.EXPENSE, updatedAt = now.minusHours(2)),
            record(categoryId = breakfast.id, ledgerId = 2L, type = RecordType.EXPENSE, updatedAt = now),
            record(categoryId = salary.id, ledgerId = 1L, type = RecordType.INCOME, updatedAt = now)
        )

        val suggestions = HomeQuickEntryCategoryPolicy.suggest(
            categories = listOf(breakfast, lunch, salary),
            records = records,
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            limit = 4
        )

        assertEquals(listOf(breakfast, lunch), suggestions)
    }

    @Test
    fun suggest_fallsBackToTopLevelCategoriesWhenNoHistory() {
        val dining = Category(
            id = 1L,
            name = "餐饮",
            icon = "restaurant",
            color = 0xFFFF6B6BL,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 0
        )
        val transport = Category(
            id = 2L,
            name = "交通",
            icon = "bus",
            color = 0xFF4ECDC4,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 1
        )
        val breakfast = Category(
            id = 3L,
            name = "早餐",
            icon = "free_breakfast",
            color = 0xFFFF9F43,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 0,
            parentId = dining.id
        )
        val salary = Category(
            id = 4L,
            name = "工资",
            icon = "payments",
            color = 0xFF4CAF50,
            type = RecordType.INCOME,
            isDefault = true,
            sortOrder = 0
        )

        val suggestions = HomeQuickEntryCategoryPolicy.suggest(
            categories = listOf(dining, transport, breakfast, salary),
            records = emptyList(),
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            limit = 4
        )

        assertEquals(listOf(dining, transport), suggestions)
    }

    @Test
    fun suggest_excludesLegacySubwayFromFallbackAndRecentSuggestions() {
        val subway = Category(
            id = 1L,
            name = "地铁",
            icon = "train",
            color = 0xFF78909C,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 0
        )
        val transport = Category(
            id = 2L,
            name = "交通",
            icon = "directions_bus",
            color = 0xFF4ECDC4,
            type = RecordType.EXPENSE,
            isDefault = true,
            sortOrder = 1
        )
        val now = LocalDateTime.of(2026, 4, 21, 10, 0)

        val recentSuggestions = HomeQuickEntryCategoryPolicy.suggest(
            categories = listOf(subway, transport),
            records = listOf(
                record(categoryId = subway.id, ledgerId = 1L, type = RecordType.EXPENSE, updatedAt = now),
                record(categoryId = transport.id, ledgerId = 1L, type = RecordType.EXPENSE, updatedAt = now.minusHours(1))
            ),
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            limit = 4
        )

        val fallbackSuggestions = HomeQuickEntryCategoryPolicy.suggest(
            categories = listOf(subway, transport),
            records = emptyList(),
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            limit = 4
        )

        assertEquals(listOf(transport), recentSuggestions)
        assertEquals(listOf(transport), fallbackSuggestions)
    }

    private fun record(
        categoryId: Long,
        ledgerId: Long,
        type: RecordType,
        updatedAt: LocalDateTime
    ): Record {
        return Record(
            id = 0L,
            amount = 18.0,
            type = type,
            categoryId = categoryId,
            note = null,
            date = LocalDate.of(2026, 4, 21),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            syncId = null,
            ledgerId = ledgerId,
            accountId = null
        )
    }
}
