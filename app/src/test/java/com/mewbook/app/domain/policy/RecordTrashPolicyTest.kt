package com.mewbook.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RecordTrashPolicyTest {

    private val deletedAt = LocalDateTime.of(2026, 5, 1, 8, 0)

    @Test
    fun isExpired_keepsRecordsUntilEndOfRetentionWindow() {
        assertFalse(
            RecordTrashPolicy.isExpired(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(30)
            )
        )
        assertTrue(
            RecordTrashPolicy.isExpired(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(30).plusSeconds(1)
            )
        )
    }

    @Test
    fun remainingDaysRoundsUpPartialDaysAndNeverDropsBelowZero() {
        assertEquals(
            30,
            RecordTrashPolicy.remainingDays(
                deletedAt = deletedAt,
                now = deletedAt
            )
        )
        assertEquals(
            29,
            RecordTrashPolicy.remainingDays(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(1)
            )
        )
        assertEquals(
            1,
            RecordTrashPolicy.remainingDays(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(29).plusHours(23)
            )
        )
        assertEquals(
            0,
            RecordTrashPolicy.remainingDays(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(31)
            )
        )
    }

    @Test
    fun expirationCutoffDeletesRecordsOlderThanRetentionWindow() {
        assertEquals(
            LocalDateTime.of(2026, 4, 23, 12, 0),
            RecordTrashPolicy.expirationCutoff(LocalDateTime.of(2026, 5, 23, 12, 0))
        )
    }

    @Test
    fun isExpiringSoonWarnsOnlyDuringLastRetentionDay() {
        assertFalse(
            RecordTrashPolicy.isExpiringSoon(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(28).plusHours(23)
            )
        )
        assertTrue(
            RecordTrashPolicy.isExpiringSoon(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(29)
            )
        )
        assertFalse(
            RecordTrashPolicy.isExpiringSoon(
                deletedAt = deletedAt,
                now = deletedAt.plusDays(30).plusSeconds(1)
            )
        )
    }
}
