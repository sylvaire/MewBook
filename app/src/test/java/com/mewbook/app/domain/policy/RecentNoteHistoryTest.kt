package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecentNoteHistoryTest {

    @Test
    fun notesByCategory_filtersByLedger_deduplicatesTrimmedNotes_andSortsByMostRecentUsage() {
        val records = listOf(
            record(
                categoryId = 10L,
                note = "午饭",
                updatedAt = LocalDateTime.of(2026, 4, 19, 12, 0),
                ledgerId = 1L
            ),
            record(
                categoryId = 10L,
                note = " 咖啡 ",
                updatedAt = LocalDateTime.of(2026, 4, 18, 9, 0),
                ledgerId = 1L
            ),
            record(
                categoryId = 10L,
                note = "咖啡",
                updatedAt = LocalDateTime.of(2026, 4, 17, 9, 0),
                ledgerId = 1L
            ),
            record(
                categoryId = 10L,
                note = "   ",
                updatedAt = LocalDateTime.of(2026, 4, 20, 8, 0),
                ledgerId = 1L
            ),
            record(
                categoryId = 11L,
                note = "地铁",
                updatedAt = LocalDateTime.of(2026, 4, 18, 8, 0),
                ledgerId = 1L
            ),
            record(
                categoryId = 10L,
                note = "午饭",
                updatedAt = LocalDateTime.of(2026, 4, 20, 12, 0),
                ledgerId = 2L
            )
        )

        val notesByCategory = RecentNoteHistory.notesByCategory(records, ledgerId = 1L)

        assertEquals(listOf("午饭", "咖啡"), notesByCategory[10L])
        assertEquals(listOf("地铁"), notesByCategory[11L])
    }

    private fun record(
        categoryId: Long,
        note: String?,
        updatedAt: LocalDateTime,
        ledgerId: Long
    ) = Record(
        id = updatedAt.hour.toLong(),
        amount = 10.0,
        type = RecordType.EXPENSE,
        categoryId = categoryId,
        note = note,
        date = LocalDate.of(2026, 4, 19),
        createdAt = updatedAt.minusHours(1),
        updatedAt = updatedAt,
        syncId = null,
        ledgerId = ledgerId,
        accountId = null
    )
}
