package com.mewbook.app.data.preferences

import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.QuickEntryMemory
import com.mewbook.app.domain.policy.QuickEntryTimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickEntryMemorySerializerTest {

    @Test
    fun encodeAndDecode_roundTripsMemories() {
        val memories = listOf(
            QuickEntryMemory(
                ledgerId = 1L,
                type = RecordType.EXPENSE,
                timeSlot = QuickEntryTimeSlot.NOON,
                categoryId = 10L,
                accountId = 20L,
                amount = 12.0,
                note = "午餐",
                savedAtEpochMillis = 1000L
            )
        )

        val encoded = QuickEntryMemorySerializer.encode(memories)
        val decoded = QuickEntryMemorySerializer.decode(encoded)

        assertEquals(memories, decoded)
    }

    @Test
    fun decode_returnsEmptyListForInvalidJson() {
        val decoded = QuickEntryMemorySerializer.decode("not-json")

        assertTrue(decoded.isEmpty())
    }
}
