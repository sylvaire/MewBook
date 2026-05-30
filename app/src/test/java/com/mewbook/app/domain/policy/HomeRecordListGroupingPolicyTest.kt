package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.BudgetPeriodType
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HomeRecordListGroupingPolicyTest {

    @Test
    fun buildEntries_dayPeriodDoesNotAddDateHeaders() {
        val first = record(id = 1, date = LocalDate.of(2026, 5, 2))
        val second = record(id = 2, date = LocalDate.of(2026, 5, 1))

        val result = HomeRecordListGroupingPolicy.buildEntries(
            records = listOf(first, second),
            periodType = BudgetPeriodType.DAY
        )

        assertEquals(
            listOf(
                HomeRecordListEntry.RecordEntry(first),
                HomeRecordListEntry.RecordEntry(second)
            ),
            result
        )
    }

    @Test
    fun buildEntries_weekPeriodAddsDateHeaderWhenDateChanges() {
        val first = record(id = 1, date = LocalDate.of(2026, 5, 2))
        val second = record(id = 2, date = LocalDate.of(2026, 5, 2))
        val third = record(id = 3, date = LocalDate.of(2026, 5, 1))

        val result = HomeRecordListGroupingPolicy.buildEntries(
            records = listOf(first, second, third),
            periodType = BudgetPeriodType.WEEK
        )

        assertEquals(
            listOf(
                HomeRecordListEntry.DateHeader(LocalDate.of(2026, 5, 2)),
                HomeRecordListEntry.RecordEntry(first),
                HomeRecordListEntry.RecordEntry(second),
                HomeRecordListEntry.DateHeader(LocalDate.of(2026, 5, 1)),
                HomeRecordListEntry.RecordEntry(third)
            ),
            result
        )
    }

    @Test
    fun buildEntries_monthPeriodAddsDateHeaderBeforeFirstRecord() {
        val first = record(id = 1, date = LocalDate.of(2026, 5, 2))

        val result = HomeRecordListGroupingPolicy.buildEntries(
            records = listOf(first),
            periodType = BudgetPeriodType.MONTH
        )

        assertEquals(
            listOf(
                HomeRecordListEntry.DateHeader(LocalDate.of(2026, 5, 2)),
                HomeRecordListEntry.RecordEntry(first)
            ),
            result
        )
    }

    private fun record(id: Long, date: LocalDate): Record {
        return Record(
            id = id,
            amount = 12.0,
            type = RecordType.EXPENSE,
            categoryId = 1,
            note = null,
            date = date,
            createdAt = LocalDateTime.of(2026, 5, 2, 12, 0),
            updatedAt = LocalDateTime.of(2026, 5, 2, 12, 0),
            syncId = null
        )
    }
}
