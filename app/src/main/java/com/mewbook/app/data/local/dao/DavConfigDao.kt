package com.mewbook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mewbook.app.data.local.entity.DavConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DavConfigDao {

    @Query("SELECT * FROM dav_config WHERE id = 1")
    fun getDavConfig(): Flow<DavConfigEntity?>

    @Query("SELECT * FROM dav_config WHERE id = 1")
    suspend fun getDavConfigOnce(): DavConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDavConfig(config: DavConfigEntity)

    @Update
    suspend fun updateDavConfig(config: DavConfigEntity)

    @Query("DELETE FROM dav_config")
    suspend fun deleteDavConfig()

    @Query("UPDATE dav_config SET lastSyncTime = :syncTime WHERE id = 1")
    suspend fun updateLastSyncTime(syncTime: Long)
}
