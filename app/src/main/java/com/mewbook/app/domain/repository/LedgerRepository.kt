package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.Ledger
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getAllLedgers(): Flow<List<Ledger>>
    fun getDefaultLedgerFlow(): Flow<Ledger?>
    suspend fun getLedgerById(id: Long): Ledger?
    suspend fun getDefaultLedger(): Ledger?
    suspend fun insertLedger(ledger: Ledger): Long
    suspend fun updateLedger(ledger: Ledger)
    suspend fun deleteLedger(ledger: Ledger)
    suspend fun setDefaultLedger(id: Long)
}
