package com.mewbook.app.data.repository

import androidx.room.withTransaction
import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.dao.DeletedRecordDao
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.database.MewBookDatabase
import com.mewbook.app.data.local.entity.DeletedRecordEntity
import com.mewbook.app.data.local.entity.RecordEntity
import com.mewbook.app.domain.model.DeletedRecord
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.policy.MoneyAmountPolicy
import com.mewbook.app.domain.policy.RecordTrashPolicy
import com.mewbook.app.domain.repository.RecordTrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordTrashRepositoryImpl @Inject constructor(
    private val database: MewBookDatabase,
    private val recordDao: RecordDao,
    private val deletedRecordDao: DeletedRecordDao,
    private val accountDao: AccountDao
) : RecordTrashRepository {

    override fun getDeletedRecords(): Flow<List<DeletedRecord>> {
        return deletedRecordDao.getDeletedRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun moveRecordToTrash(recordId: Long, deletedAt: LocalDateTime): Boolean {
        return database.withTransaction {
            val record = recordDao.getRecordById(recordId) ?: return@withTransaction false
            deletedRecordDao.insertDeletedRecord(record.toDeletedEntity(deletedAt))
            recordDao.deleteRecordById(recordId)
            adjustAccountBalanceForRemoval(record)
            true
        }
    }

    override suspend fun restoreDeletedRecord(recordId: Long): Boolean {
        return database.withTransaction {
            val deletedRecord = deletedRecordDao.getDeletedRecordById(recordId) ?: return@withTransaction false
            val record = deletedRecord.toRecordEntity()
            val recordToRestore = if (recordDao.getRecordById(record.id) == null) {
                record
            } else {
                record.copy(id = 0)
            }
            recordDao.insertRecord(recordToRestore)
            deletedRecordDao.deleteDeletedRecordById(recordId)
            adjustAccountBalanceForRestore(record)
            true
        }
    }

    override suspend fun deleteDeletedRecordForever(recordId: Long) {
        deletedRecordDao.deleteDeletedRecordById(recordId)
    }

    override suspend fun purgeExpiredDeletedRecords(now: LocalDateTime): Int {
        val cutoff = RecordTrashPolicy.expirationCutoff(now).toEpochSecond(ZoneOffset.UTC)
        return deletedRecordDao.deleteRecordsDeletedBefore(cutoff)
    }

    private suspend fun adjustAccountBalanceForRemoval(record: RecordEntity) {
        val accountId = record.accountId ?: return
        val account = accountDao.getAccountById(accountId) ?: return
        val balanceChange = if (record.type == RecordType.INCOME.name) -record.amount else record.amount
        accountDao.updateBalance(accountId, MoneyAmountPolicy.normalizeCurrency(account.balance + balanceChange))
    }

    private suspend fun adjustAccountBalanceForRestore(record: RecordEntity) {
        val accountId = record.accountId ?: return
        val account = accountDao.getAccountById(accountId) ?: return
        val balanceChange = if (record.type == RecordType.INCOME.name) record.amount else -record.amount
        accountDao.updateBalance(accountId, MoneyAmountPolicy.normalizeCurrency(account.balance + balanceChange))
    }

    private fun RecordEntity.toDeletedEntity(deletedAt: LocalDateTime): DeletedRecordEntity {
        return DeletedRecordEntity(
            recordId = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncId = syncId,
            ledgerId = ledgerId,
            accountId = accountId,
            deletedAt = deletedAt.toEpochSecond(ZoneOffset.UTC)
        )
    }

    private fun DeletedRecordEntity.toRecordEntity(): RecordEntity {
        return RecordEntity(
            id = recordId,
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncId = syncId,
            ledgerId = ledgerId,
            accountId = accountId
        )
    }

    private fun DeletedRecordEntity.toDomain(): DeletedRecord {
        return DeletedRecord(
            record = Record(
                id = recordId,
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
            ),
            deletedAt = LocalDateTime.ofEpochSecond(deletedAt, 0, ZoneOffset.UTC)
        )
    }
}
