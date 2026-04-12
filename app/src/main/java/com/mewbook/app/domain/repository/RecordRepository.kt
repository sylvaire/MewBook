package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Record
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecordRepository {
    fun getRecordsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Record>>
    fun getRecordsByMonth(ledgerId: Long, month: String): Flow<List<Record>>
    fun getAllRecords(): Flow<List<Record>>
    suspend fun getRecordById(id: Long): Record?
    suspend fun insertRecord(record: Record): Long
    suspend fun updateRecord(record: Record)
    suspend fun deleteRecord(record: Record)
    suspend fun deleteRecordById(id: Long)
    suspend fun getAllRecordsOnce(): List<Record>
    suspend fun insertRecords(records: List<Record>)
    suspend fun deleteAllRecords()
}
