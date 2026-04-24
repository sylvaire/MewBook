package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.AccountType
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HomeRecordSearchPolicyTest {

    @Test
    fun search_matchesNoteCategoryAmountAndAccountWithinCurrentLedger() {
        val categories = listOf(
            category(id = 10L, name = "餐饮"),
            category(id = 11L, name = "交通")
        )
        val accounts = listOf(
            account(id = 100L, name = "微信"),
            account(id = 101L, name = "现金")
        )
        val records = listOf(
            record(
                id = 1L,
                amount = 28.5,
                categoryId = 10L,
                note = "和朋友吃火锅",
                ledgerId = 1L,
                accountId = 100L,
                date = LocalDate.of(2026, 4, 19)
            ),
            record(
                id = 2L,
                amount = 12.0,
                categoryId = 11L,
                note = "公交",
                ledgerId = 1L,
                accountId = 101L,
                date = LocalDate.of(2026, 4, 18)
            )
        )

        assertEquals(
            listOf(1L),
            HomeRecordSearchPolicy.search(
                query = "火锅",
                activeLedgerId = 1L,
                records = records,
                categoriesById = categories.associateBy { it.id },
                accountsById = accounts.associateBy { it.id }
            ).map(Record::id)
        )
        assertEquals(
            listOf(1L),
            HomeRecordSearchPolicy.search(
                query = "餐饮",
                activeLedgerId = 1L,
                records = records,
                categoriesById = categories.associateBy { it.id },
                accountsById = accounts.associateBy { it.id }
            ).map(Record::id)
        )
        assertEquals(
            listOf(1L),
            HomeRecordSearchPolicy.search(
                query = "28.5",
                activeLedgerId = 1L,
                records = records,
                categoriesById = categories.associateBy { it.id },
                accountsById = accounts.associateBy { it.id }
            ).map(Record::id)
        )
        assertEquals(
            listOf(1L),
            HomeRecordSearchPolicy.search(
                query = "微信",
                activeLedgerId = 1L,
                records = records,
                categoriesById = categories.associateBy { it.id },
                accountsById = accounts.associateBy { it.id }
            ).map(Record::id)
        )
    }

    @Test
    fun search_excludesRecordsFromOtherLedgers() {
        val records = listOf(
            record(
                id = 1L,
                amount = 88.0,
                categoryId = 10L,
                note = "共享账本记录",
                ledgerId = 2L,
                accountId = null,
                date = LocalDate.of(2026, 4, 19)
            )
        )

        val result = HomeRecordSearchPolicy.search(
            query = "共享",
            activeLedgerId = 1L,
            records = records,
            categoriesById = emptyMap(),
            accountsById = emptyMap()
        )

        assertEquals(emptyList<Record>(), result)
    }

    @Test
    fun search_returnsEmptyListForBlankQuery() {
        val result = HomeRecordSearchPolicy.search(
            query = "   ",
            activeLedgerId = 1L,
            records = listOf(
                record(
                    id = 1L,
                    amount = 18.0,
                    categoryId = 10L,
                    note = "早餐",
                    ledgerId = 1L,
                    accountId = null,
                    date = LocalDate.of(2026, 4, 19)
                )
            ),
            categoriesById = emptyMap(),
            accountsById = emptyMap()
        )

        assertEquals(emptyList<Record>(), result)
    }

    @Test
    fun search_returnsNewestFirstForMatchedRecords() {
        val resultIds = HomeRecordSearchPolicy.search(
            query = "早餐",
            activeLedgerId = 1L,
            records = listOf(
                record(
                    id = 1L,
                    amount = 8.0,
                    categoryId = 10L,
                    note = "早餐包子",
                    ledgerId = 1L,
                    accountId = null,
                    date = LocalDate.of(2026, 4, 18),
                    createdAt = LocalDateTime.of(2026, 4, 18, 8, 0)
                ),
                record(
                    id = 2L,
                    amount = 16.0,
                    categoryId = 10L,
                    note = "早餐豆浆",
                    ledgerId = 1L,
                    accountId = null,
                    date = LocalDate.of(2026, 4, 20),
                    createdAt = LocalDateTime.of(2026, 4, 20, 7, 0)
                ),
                record(
                    id = 3L,
                    amount = 22.0,
                    categoryId = 10L,
                    note = "早餐面条",
                    ledgerId = 1L,
                    accountId = null,
                    date = LocalDate.of(2026, 4, 20),
                    createdAt = LocalDateTime.of(2026, 4, 20, 9, 0)
                )
            ),
            categoriesById = emptyMap(),
            accountsById = emptyMap()
        ).map(Record::id)

        assertEquals(listOf(3L, 2L, 1L), resultIds)
    }

    private fun category(id: Long, name: String) = Category(
        id = id,
        name = name,
        icon = "more_horiz",
        color = 0xFF808080,
        type = RecordType.EXPENSE,
        isDefault = true,
        sortOrder = id.toInt()
    )

    private fun account(id: Long, name: String) = Account(
        id = id,
        name = name,
        type = AccountType.CASH,
        balance = 0.0,
        icon = "account_balance_wallet",
        color = 0xFF4CAF50,
        isDefault = false,
        sortOrder = id.toInt(),
        ledgerId = 1L
    )

    private fun record(
        id: Long,
        amount: Double,
        categoryId: Long,
        note: String?,
        ledgerId: Long,
        accountId: Long?,
        date: LocalDate,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 4, 20, 8, 0)
    ) = Record(
        id = id,
        amount = amount,
        type = RecordType.EXPENSE,
        categoryId = categoryId,
        note = note,
        date = date,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncId = null,
        ledgerId = ledgerId,
        accountId = accountId
    )
}
