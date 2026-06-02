package com.mewbook.app.domain.policy

import java.time.Duration
import java.time.LocalDateTime

object RecordTrashPolicy {
    const val RETENTION_DAYS: Long = 30

    fun expirationCutoff(now: LocalDateTime): LocalDateTime {
        return now.minusDays(RETENTION_DAYS)
    }

    fun isExpired(deletedAt: LocalDateTime, now: LocalDateTime): Boolean {
        return deletedAt.isBefore(expirationCutoff(now))
    }

    fun remainingDays(deletedAt: LocalDateTime, now: LocalDateTime): Int {
        val expiresAt = deletedAt.plusDays(RETENTION_DAYS)
        if (!expiresAt.isAfter(now)) {
            return 0
        }
        val remaining = Duration.between(now, expiresAt)
        val wholeDays = remaining.toDays()
        val hasPartialDay = remaining.minusDays(wholeDays).seconds > 0 || remaining.minusDays(wholeDays).nano > 0
        return (wholeDays + if (hasPartialDay) 1 else 0).toInt()
    }

    fun isExpiringSoon(deletedAt: LocalDateTime, now: LocalDateTime): Boolean {
        return !isExpired(deletedAt, now) && remainingDays(deletedAt, now) <= 1
    }
}
