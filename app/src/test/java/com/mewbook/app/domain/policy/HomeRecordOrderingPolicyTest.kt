package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HomeRecordOrderingPolicyTest {

    @Test
    fun newestFirst_sortsByDateThenCreatedAtDescending() {
        val orderedIds = HomeRecordOrderingPolicy.newestFirst(
            listOf(
                record(
                    id = 1L,
                    date = LocalDate.of(2026, 4, 18),
                    createdAt = LocalDateTime.of(2026, 4, 18, 9, 0)
                ),
                record(
                    id = 2L,
                    date = LocalDate.of(2026, 4, 20),
                    createdAt = LocalDateTime.of(2026, 4, 20, 8, 0)
                ),
                record(
                    id = 3L,
                    date = LocalDate.of(2026, 4, 20),
                    createdAt = LocalDateTime.of(2026, 4, 20, 10, 0)
                )
            )
        ).map(Record::id)

        assertEquals(listOf(3L, 2L, 1L), orderedIds)
    }

    private fun record(
        id: Long,
        date: LocalDate,
        createdAt: LocalDateTime
    ) = Record(
        id = id,
        amount = 18.0,
        type = RecordType.EXPENSE,
        categoryId = 10L,
        note = "note-$id",
        date = date,
        createdAt = createdAt,
        updatedAt = createdAt,
        syncId = null,
        ledgerId = 1L,
        accountId = null
    )
}
