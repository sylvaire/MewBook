package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.DeletedRecord
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecycleBinFilterPolicyTest {

    @Test
    fun filter_keepsOnlySelectedCategory() {
        val records = listOf(
            deletedRecord(id = 1L, amount = 28.0, categoryId = 10L),
            deletedRecord(id = 2L, amount = 88.0, categoryId = 11L),
            deletedRecord(id = 3L, amount = 208.0, categoryId = 10L)
        )

        val result = RecycleBinFilterPolicy.filter(
            records = records,
            categoryId = 10L,
            amountFilter = RecycleBinAmountFilter.ALL
        )

        assertEquals(listOf(1L, 3L), result.map { it.record.id })
    }

    @Test
    fun filter_appliesAmountBucketsAfterCategoryFilter() {
        val records = listOf(
            deletedRecord(id = 1L, amount = 50.0, categoryId = 10L),
            deletedRecord(id = 2L, amount = 120.0, categoryId = 10L),
            deletedRecord(id = 3L, amount = 260.0, categoryId = 10L),
            deletedRecord(id = 4L, amount = 180.0, categoryId = 11L)
        )

        val result = RecycleBinFilterPolicy.filter(
            records = records,
            categoryId = 10L,
            amountFilter = RecycleBinAmountFilter.FROM_50_TO_200
        )

        assertEquals(listOf(2L), result.map { it.record.id })
    }

    private fun deletedRecord(
        id: Long,
        amount: Double,
        categoryId: Long
    ) = DeletedRecord(
        record = Record(
            id = id,
            amount = amount,
            type = RecordType.EXPENSE,
            categoryId = categoryId,
            note = null,
            date = LocalDate.of(2026, 5, id.toInt()),
            createdAt = LocalDateTime.of(2026, 5, id.toInt(), 8, 0),
            updatedAt = LocalDateTime.of(2026, 5, id.toInt(), 8, 0),
            syncId = null
        ),
        deletedAt = LocalDateTime.of(2026, 5, 20, 8, 0)
    )
}
