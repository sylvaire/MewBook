package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.Account
import com.mewbook.app.domain.model.Category
import com.mewbook.app.domain.model.RecordType

enum class QuickEntryTimeSlot {
    MORNING,
    NOON,
    AFTERNOON,
    EVENING,
    NIGHT;

    companion object {
        fun fromHour(hour: Int): QuickEntryTimeSlot {
            val normalizedHour = hour.floorMod(24)
            return when (normalizedHour) {
                in 6..10 -> MORNING
                in 11..13 -> NOON
                in 14..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }

        private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
    }
}

data class QuickEntryMemory(
    val ledgerId: Long,
    val type: RecordType,
    val timeSlot: QuickEntryTimeSlot,
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Double?,
    val note: String?,
    val savedAtEpochMillis: Long
)

data class QuickEntryDefaults(
    val categoryId: Long?,
    val accountId: Long?,
    val amount: Double?
)

object QuickEntryDefaultsPolicy {

    fun resolve(
        ledgerId: Long,
        type: RecordType,
        timeSlot: QuickEntryTimeSlot,
        memories: List<QuickEntryMemory>,
        quickCategories: List<Category>,
        availableCategories: List<Category> = quickCategories,
        accounts: List<Account>,
        fallbackAccountId: Long?
    ): QuickEntryDefaults {
        val matchingMemories = memories
            .filter { it.ledgerId == ledgerId && it.type == type }
            .sortedByDescending { it.savedAtEpochMillis }
        val memory = matchingMemories.firstOrNull { it.timeSlot == timeSlot }
            ?: matchingMemories.firstOrNull()
        val validCategoryIds = availableCategories.map(Category::id).toSet()
        val validAccountIds = accounts.map(Account::id).toSet()

        return QuickEntryDefaults(
            categoryId = memory?.categoryId?.takeIf { it in validCategoryIds }
                ?: quickCategories.firstOrNull()?.id,
            accountId = memory?.accountId?.takeIf { it in validAccountIds }
                ?: fallbackAccountId?.takeIf { it in validAccountIds },
            amount = memory?.amount?.takeIf { it > 0.0 }
        )
    }
}
