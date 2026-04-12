package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mewbook.app.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE ledgerId = :ledgerId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getRecordsByMonth(ledgerId: Long, startDate: Long, endDate: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY date DESC, createdAt DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RecordEntity>)

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()

    @Query("SELECT SUM(amount) FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate")
    fun getTotalAmountByType(type: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM records")
    suspend fun getAllRecordsOnce(): List<RecordEntity>
}
