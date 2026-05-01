package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.DavAutoBackupStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface DavAutoBackupStatusRepository {
    val status: Flow<DavAutoBackupStatus>

    suspend fun getLastAttemptDateOnce(): LocalDate?

    suspend fun recordAttempt(date: LocalDate, time: LocalDateTime)

    suspend fun recordSuccess(time: LocalDateTime, message: String?)

    suspend fun recordFailure(message: String)
}
