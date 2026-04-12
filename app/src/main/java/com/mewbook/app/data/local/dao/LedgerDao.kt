package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mewbook.app.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Query("SELECT * FROM ledgers ORDER BY isDefault DESC, createdAt ASC")
    fun getAllLedgers(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers WHERE id = :id")
    suspend fun getLedgerById(id: Long): LedgerEntity?

    @Query("SELECT * FROM ledgers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultLedger(): LedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: LedgerEntity): Long

    @Update
    suspend fun updateLedger(ledger: LedgerEntity)

    @Delete
    suspend fun deleteLedger(ledger: LedgerEntity)

    @Query("UPDATE ledgers SET isDefault = 0")
    suspend fun clearDefaultLedger()

    @Query("UPDATE ledgers SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultLedger(id: Long)
}
