package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.LedgerDao
import com.mewbook.app.data.local.entity.LedgerEntity
import com.mewbook.app.domain.model.Ledger
import com.mewbook.app.domain.model.LedgerType
import com.mewbook.app.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepositoryImpl @Inject constructor(
    private val ledgerDao: LedgerDao
) : LedgerRepository {

    override fun getAllLedgers(): Flow<List<Ledger>> {
        return ledgerDao.getAllLedgers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDefaultLedgerFlow(): Flow<Ledger?> {
        return ledgerDao.getDefaultLedgerFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getLedgerById(id: Long): Ledger? {
        return ledgerDao.getLedgerById(id)?.toDomain()
    }

    override suspend fun getDefaultLedger(): Ledger? {
        return ledgerDao.getDefaultLedger()?.toDomain()
    }

    override suspend fun insertLedger(ledger: Ledger): Long {
        return ledgerDao.insertLedger(ledger.toEntity())
    }

    override suspend fun updateLedger(ledger: Ledger) {
        ledgerDao.updateLedger(ledger.toEntity())
    }

    override suspend fun deleteLedger(ledger: Ledger) {
        ledgerDao.deleteLedger(ledger.toEntity())
    }

    override suspend fun setDefaultLedger(id: Long) {
        ledgerDao.clearDefaultLedger()
        ledgerDao.setDefaultLedger(id)
    }

    private fun LedgerEntity.toDomain(): Ledger {
        return Ledger(
            id = id,
            name = name,
            type = LedgerType.valueOf(type),
            icon = icon,
            color = color,
            createdAt = createdAt,
            isDefault = isDefault
        )
    }

    private fun Ledger.toEntity(): LedgerEntity {
        return LedgerEntity(
            id = id,
            name = name,
            type = type.name,
            icon = icon,
            color = color,
            createdAt = createdAt,
            isDefault = isDefault
        )
    }
}
