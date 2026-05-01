package com.mewbook.app.domain.repository

import com.mewbook.app.domain.model.DavBackupPruneResult
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConfig
import kotlinx.coroutines.flow.Flow

interface DavRepository {
    fun getDavConfig(): Flow<DavConfig?>
    suspend fun getDavConfigOnce(): DavConfig?
    suspend fun saveDavConfig(config: DavConfig)
    suspend fun testConnection(config: DavConfig): Result<Boolean>
    suspend fun exportData(config: DavConfig, fileName: String? = null): Result<Boolean>
    suspend fun exportAutoBackupData(config: DavConfig): Result<Boolean>
    suspend fun listBackupFiles(config: DavConfig): Result<List<DavBackupFile>>
    suspend fun previewImportData(config: DavConfig): Result<com.mewbook.app.data.backup.BackupRestorePreview>
    suspend fun previewImportData(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<com.mewbook.app.data.backup.BackupRestorePreview>
    suspend fun importData(config: DavConfig): Result<Boolean>
    suspend fun importData(config: DavConfig, backupFile: DavBackupFile): Result<Boolean>
    suspend fun updateLastSyncTime(time: Long)
    suspend fun pruneBackupFiles(config: DavConfig, keepLatestCount: Int): Result<DavBackupPruneResult>
}
