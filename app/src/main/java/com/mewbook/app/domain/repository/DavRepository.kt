package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.DavConfig
import kotlinx.coroutines.flow.Flow

interface DavRepository {
    fun getDavConfig(): Flow<DavConfig?>
    suspend fun getDavConfigOnce(): DavConfig?
    suspend fun saveDavConfig(config: DavConfig)
    suspend fun testConnection(config: DavConfig): Result<Boolean>
    suspend fun exportData(config: DavConfig): Result<Boolean>
    suspend fun importData(config: DavConfig): Result<Boolean>
    suspend fun updateLastSyncTime(time: Long)
}
