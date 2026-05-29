package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickEntryDefaultsPolicyTest {

    @Test
    fun resolve_prefersExactTimeSlotMemoryWhenIdsAreStillValid() {
        val dining = category(id = 10L, name = "餐饮")
        val transport = category(id = 11L, name = "交通")
        val cash = account(id = 20L, name = "现金")
        val wechat = account(id = 21L, name = "微信")
        val memories = listOf(
            memory(
                categoryId = transport.id,
                accountId = wechat.id,
                amount = 12.0,
                timeSlot = QuickEntryTimeSlot.NOON,
                savedAtEpochMillis = 2000L
            ),
            memory(
                categoryId = dining.id,
                accountId = cash.id,
                amount = 9.9,
                timeSlot = QuickEntryTimeSlot.MORNING,
                savedAtEpochMillis = 3000L
            )
        )

        val defaults = QuickEntryDefaultsPolicy.resolve(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = memories,
            quickCategories = listOf(dining, transport),
            accounts = listOf(cash, wechat),
            fallbackAccountId = cash.id
        )

        assertEquals(transport.id, defaults.categoryId)
        assertEquals(wechat.id, defaults.accountId)
        assertEquals(12.0, defaults.amount)
    }

    @Test
    fun resolve_fallsBackToLatestTypeMemoryWhenTimeSlotHasNoMatch() {
        val dining = category(id = 10L, name = "餐饮")
        val transport = category(id = 11L, name = "交通")
        val cash = account(id = 20L, name = "现金")
        val memories = listOf(
            memory(categoryId = dining.id, accountId = cash.id, amount = 9.9, timeSlot = QuickEntryTimeSlot.MORNING, savedAtEpochMillis = 1000L),
            memory(categoryId = transport.id, accountId = cash.id, amount = 25.0, timeSlot = QuickEntryTimeSlot.EVENING, savedAtEpochMillis = 3000L)
        )

        val defaults = QuickEntryDefaultsPolicy.resolve(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.AFTERNOON,
            memories = memories,
            quickCategories = listOf(dining, transport),
            accounts = listOf(cash),
            fallbackAccountId = cash.id
        )

        assertEquals(transport.id, defaults.categoryId)
        assertEquals(cash.id, defaults.accountId)
        assertEquals(25.0, defaults.amount)
    }

    @Test
    fun resolve_ignoresStaleCategoryAndAccountIds() {
        val dining = category(id = 10L, name = "餐饮")
        val cash = account(id = 20L, name = "现金")
        val memories = listOf(
            memory(categoryId = 999L, accountId = 888L, amount = 18.0, timeSlot = QuickEntryTimeSlot.NOON, savedAtEpochMillis = 5000L)
        )

        val defaults = QuickEntryDefaultsPolicy.resolve(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = memories,
            quickCategories = listOf(dining),
            accounts = listOf(cash),
            fallbackAccountId = cash.id
        )

        assertEquals(dining.id, defaults.categoryId)
        assertEquals(cash.id, defaults.accountId)
        assertEquals(18.0, defaults.amount)
    }

    @Test
    fun resolve_keepsRememberedCategoryWhenAvailableButOutsideQuickSuggestions() {
        val dining = category(id = 10L, name = "餐饮")
        val commute = category(id = 11L, name = "通勤")
        val cash = account(id = 20L, name = "现金")
        val memories = listOf(
            memory(categoryId = commute.id, accountId = cash.id, amount = 12.0, timeSlot = QuickEntryTimeSlot.NOON, savedAtEpochMillis = 5000L)
        )

        val defaults = QuickEntryDefaultsPolicy.resolve(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = memories,
            quickCategories = listOf(dining),
            availableCategories = listOf(dining, commute),
            accounts = listOf(cash),
            fallbackAccountId = cash.id
        )

        assertEquals(commute.id, defaults.categoryId)
    }

    @Test
    fun resolve_separatesLedgerAndType() {
        val dining = category(id = 10L, name = "餐饮", type = RecordType.EXPENSE)
        val salary = category(id = 12L, name = "工资", type = RecordType.INCOME)
        val cash = account(id = 20L, name = "现金")
        val memories = listOf(
            memory(ledgerId = 2L, categoryId = dining.id, accountId = cash.id, amount = 9.9, type = RecordType.EXPENSE, timeSlot = QuickEntryTimeSlot.NOON, savedAtEpochMillis = 4000L),
            memory(ledgerId = 1L, categoryId = salary.id, accountId = cash.id, amount = 3000.0, type = RecordType.INCOME, timeSlot = QuickEntryTimeSlot.NOON, savedAtEpochMillis = 5000L)
        )

        val defaults = QuickEntryDefaultsPolicy.resolve(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = memories,
            quickCategories = listOf(dining),
            accounts = listOf(cash),
            fallbackAccountId = cash.id
        )

        assertEquals(dining.id, defaults.categoryId)
        assertEquals(cash.id, defaults.accountId)
        assertEquals(null, defaults.amount)
    }

    @Test
    fun timeSlot_fromHourMapsDayParts() {
        assertEquals(QuickEntryTimeSlot.NIGHT, QuickEntryTimeSlot.fromHour(5))
        assertEquals(QuickEntryTimeSlot.MORNING, QuickEntryTimeSlot.fromHour(9))
        assertEquals(QuickEntryTimeSlot.NOON, QuickEntryTimeSlot.fromHour(12))
        assertEquals(QuickEntryTimeSlot.AFTERNOON, QuickEntryTimeSlot.fromHour(16))
        assertEquals(QuickEntryTimeSlot.EVENING, QuickEntryTimeSlot.fromHour(20))
    }

    private fun category(
        id: Long,
        name: String,
        type: RecordType = RecordType.EXPENSE
    ) = Category(
        id = id,
        name = name,
        icon = "restaurant",
        color = 0xFFFF6B6B,
        type = type,
        isDefault = true,
        sortOrder = 0
    )

    private fun account(id: Long, name: String) = Account(
        id = id,
        name = name,
        type = AccountType.CASH,
        balance = 0.0,
        icon = "wallet",
        color = 0xFF4CAF50,
        isDefault = false,
        sortOrder = 0,
        ledgerId = 1L
    )

    private fun memory(
        ledgerId: Long = 1L,
        type: RecordType = RecordType.EXPENSE,
        timeSlot: QuickEntryTimeSlot,
        categoryId: Long?,
        accountId: Long?,
        amount: Double?,
        savedAtEpochMillis: Long
    ) = QuickEntryMemory(
        ledgerId = ledgerId,
        type = type,
        timeSlot = timeSlot,
        categoryId = categoryId,
        accountId = accountId,
        amount = amount,
        note = null,
        savedAtEpochMillis = savedAtEpochMillis
    )
}
