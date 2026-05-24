package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.DeletedRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface RecordTrashRepository {
    fun getDeletedRecords(): Flow<List<DeletedRecord>>
    suspend fun moveRecordToTrash(recordId: Long, deletedAt: LocalDateTime = LocalDateTime.now()): Boolean
    suspend fun restoreDeletedRecord(recordId: Long): Boolean
    suspend fun deleteDeletedRecordForever(recordId: Long)
    suspend fun purgeExpiredDeletedRecords(now: LocalDateTime = LocalDateTime.now()): Int
}
