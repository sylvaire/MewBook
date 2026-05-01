package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.entity.RecordEntity
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.repository.RecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao
) : RecordRepository {

    override fun getRecordsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Record>> {
        return recordDao.getRecordsByDateRange(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getExpenseRecordsByCategoryAndDateRange(
        categoryId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Record>> {
        return recordDao.getRecordsByCategoryTypeAndDateRange(
            categoryId,
            RecordType.EXPENSE.name,
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecordsByLedgerAndDateRange(
        ledgerId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Record>> {
        return recordDao.getRecordsByLedgerAndDateRange(
            ledgerId,
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecordsByMonth(ledgerId: Long, month: String): Flow<List<Record>> {
        val year = month.substring(0, 4).toInt()
        val monthNum = month.substring(5, 7).toInt()
        val startDate = LocalDate.of(year, monthNum, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return getRecordsByLedgerAndDateRange(ledgerId, startDate, endDate)
    }

    override fun getAllRecords(): Flow<List<Record>> {
        return recordDao.getAllRecords().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getRecordById(id: Long): Record? {
        return recordDao.getRecordById(id)?.toDomain()
    }

    override suspend fun insertRecord(record: Record): Long {
        return recordDao.insertRecord(record.toEntity())
    }

    override suspend fun updateRecord(record: Record) {
        recordDao.updateRecord(record.toEntity())
    }

    override suspend fun deleteRecord(record: Record) {
        recordDao.deleteRecord(record.toEntity())
    }

    override suspend fun deleteRecordById(id: Long) {
        recordDao.deleteRecordById(id)
    }

    override suspend fun getAllRecordsOnce(): List<Record> {
        return recordDao.getAllRecordsOnce().map { it.toDomain() }
    }

    override suspend fun insertRecords(records: List<Record>) {
        recordDao.insertRecords(records.map { it.toEntity() })
    }

    override suspend fun deleteAllRecords() {
        recordDao.deleteAllRecords()
    }

    override fun getDatesWithRecords(ledgerId: Long, startDate: LocalDate, endDate: LocalDate): Flow<Set<LocalDate>> {
        return recordDao.getDatesWithRecords(
            ledgerId,
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { days -> days.map { LocalDate.ofEpochDay(it) }.toSet() }
    }

    private fun RecordEntity.toDomain(): Record {
        return Record(
            id = id,
            amount = amount,
            type = RecordType.valueOf(type),
            categoryId = categoryId,
            note = note,
            date = LocalDate.ofEpochDay(date),
            createdAt = LocalDateTime.ofEpochSecond(createdAt, 0, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofEpochSecond(updatedAt, 0, ZoneOffset.UTC),
            syncId = syncId,
            ledgerId = ledgerId,
            accountId = accountId
        )
    }

    private fun Record.toEntity(): RecordEntity {
        return RecordEntity(
            id = id,
            amount = amount,
            type = type.name,
            categoryId = categoryId,
            note = note,
            date = date.toEpochDay(),
            createdAt = createdAt.toEpochSecond(ZoneOffset.UTC),
            updatedAt = updatedAt.toEpochSecond(ZoneOffset.UTC),
            syncId = syncId,
            ledgerId = ledgerId,
            accountId = accountId
        )
    }
}
