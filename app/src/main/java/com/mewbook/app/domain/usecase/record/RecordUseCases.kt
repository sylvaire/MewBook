package com.mewbook.app.domain.usecase.record

import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.repository.RecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetRecordsUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<Record>> {
        return recordRepository.getRecordsByDateRange(startDate, endDate)
    }

    fun getByLedgerMonth(ledgerId: Long, month: String): Flow<List<Record>> {
        return recordRepository.getRecordsByMonth(ledgerId, month)
    }

    fun getByLedgerAndDateRange(ledgerId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<Record>> {
        return recordRepository.getRecordsByLedgerAndDateRange(ledgerId, startDate, endDate)
    }

    /** 提供指定分类在日期区间内的支出记录，后续可在上层继续按账本等维度过滤。 */
    fun getExpenseByCategoryAndDateRange(
        categoryId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Record>> {
        return recordRepository.getExpenseRecordsByCategoryAndDateRange(categoryId, startDate, endDate)
    }

    fun getAll(): Flow<List<Record>> {
        return recordRepository.getAllRecords()
    }
}

class AddRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(record: Record): Long {
        return recordRepository.insertRecord(record)
    }
}

class UpdateRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(record: Record) {
        recordRepository.updateRecord(record)
    }
}

class DeleteRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(id: Long) {
        recordRepository.deleteRecordById(id)
    }
}
