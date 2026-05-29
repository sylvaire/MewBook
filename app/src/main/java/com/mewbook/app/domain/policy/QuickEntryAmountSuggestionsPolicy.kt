package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.RecordType
import kotlin.math.roundToLong

object QuickEntryAmountSuggestionsPolicy {
    private val defaultAmounts = listOf(9.9, 12.0, 25.0)

    fun suggest(
        ledgerId: Long,
        type: RecordType,
        timeSlot: QuickEntryTimeSlot,
        memories: List<QuickEntryMemory>,
        limit: Int = 6
    ): List<Double> {
        val recentAmounts = memories
            .asSequence()
            .filter { it.ledgerId == ledgerId && it.type == type && it.timeSlot == timeSlot }
            .sortedByDescending { it.savedAtEpochMillis }
            .mapNotNull { it.amount }
            .filter { it > 0.0 }
            .toList()

        val seenCents = mutableSetOf<Long>()
        return (defaultAmounts + recentAmounts)
            .filter { amount -> seenCents.add(amount.toCents()) }
            .take(limit)
    }

    private fun Double.toCents(): Long = (this * 100).roundToLong()
}
