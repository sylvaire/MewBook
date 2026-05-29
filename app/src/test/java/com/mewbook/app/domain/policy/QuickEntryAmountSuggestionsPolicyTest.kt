package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickEntryAmountSuggestionsPolicyTest {

    @Test
    fun suggest_startsWithFixedDefaultsThenRecentMatchingAmounts() {
        val suggestions = QuickEntryAmountSuggestionsPolicy.suggest(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = listOf(
                memory(amount = 16.5, savedAtEpochMillis = 3000L),
                memory(amount = 8.0, savedAtEpochMillis = 1000L)
            )
        )

        assertEquals(listOf(9.9, 12.0, 25.0, 16.5, 8.0), suggestions)
    }

    @Test
    fun suggest_deDuplicatesByCentsAndDropsInvalidAmounts() {
        val suggestions = QuickEntryAmountSuggestionsPolicy.suggest(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = listOf(
                memory(amount = 12.004, savedAtEpochMillis = 5000L),
                memory(amount = 12.0, savedAtEpochMillis = 4000L),
                memory(amount = 0.0, savedAtEpochMillis = 3000L),
                memory(amount = -5.0, savedAtEpochMillis = 2000L),
                memory(amount = null, savedAtEpochMillis = 1000L)
            )
        )

        assertEquals(listOf(9.9, 12.0, 25.0), suggestions)
    }

    @Test
    fun suggest_filtersByLedgerTypeAndTimeSlot() {
        val suggestions = QuickEntryAmountSuggestionsPolicy.suggest(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = listOf(
                memory(ledgerId = 2L, amount = 15.0, savedAtEpochMillis = 5000L),
                memory(type = RecordType.INCOME, amount = 18.0, savedAtEpochMillis = 4000L),
                memory(timeSlot = QuickEntryTimeSlot.EVENING, amount = 22.0, savedAtEpochMillis = 3000L),
                memory(amount = 11.0, savedAtEpochMillis = 2000L)
            )
        )

        assertEquals(listOf(9.9, 12.0, 25.0, 11.0), suggestions)
    }

    @Test
    fun suggest_capsResultsAtLimit() {
        val suggestions = QuickEntryAmountSuggestionsPolicy.suggest(
            ledgerId = 1L,
            type = RecordType.EXPENSE,
            timeSlot = QuickEntryTimeSlot.NOON,
            memories = listOf(
                memory(amount = 31.0, savedAtEpochMillis = 7000L),
                memory(amount = 32.0, savedAtEpochMillis = 6000L),
                memory(amount = 33.0, savedAtEpochMillis = 5000L),
                memory(amount = 34.0, savedAtEpochMillis = 4000L),
                memory(amount = 35.0, savedAtEpochMillis = 3000L)
            ),
            limit = 6
        )

        assertEquals(listOf(9.9, 12.0, 25.0, 31.0, 32.0, 33.0), suggestions)
    }

    private fun memory(
        ledgerId: Long = 1L,
        type: RecordType = RecordType.EXPENSE,
        timeSlot: QuickEntryTimeSlot = QuickEntryTimeSlot.NOON,
        amount: Double?,
        savedAtEpochMillis: Long
    ) = QuickEntryMemory(
        ledgerId = ledgerId,
        type = type,
        timeSlot = timeSlot,
        categoryId = 1L,
        accountId = 1L,
        amount = amount,
        note = null,
        savedAtEpochMillis = savedAtEpochMillis
    )
}
