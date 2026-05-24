package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mewbook.app.data.local.entity.DeletedRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedRecordDao {

    @Query("SELECT * FROM deleted_records ORDER BY deletedAt DESC, date DESC, createdAt DESC")
    fun getDeletedRecords(): Flow<List<DeletedRecordEntity>>

    @Query("SELECT * FROM deleted_records WHERE recordId = :recordId")
    suspend fun getDeletedRecordById(recordId: Long): DeletedRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecordEntity)

    @Query("DELETE FROM deleted_records WHERE recordId = :recordId")
    suspend fun deleteDeletedRecordById(recordId: Long)

    @Query("DELETE FROM deleted_records WHERE deletedAt < :cutoffEpochSeconds")
    suspend fun deleteRecordsDeletedBefore(cutoffEpochSeconds: Long): Int

    @Query("DELETE FROM deleted_records")
    suspend fun deleteAllDeletedRecords()
}
