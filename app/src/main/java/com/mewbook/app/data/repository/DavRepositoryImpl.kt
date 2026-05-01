package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.remote.DavRemoteDataSource
import com.mewbook.app.data.backup.BackupMigration
import com.mewbook.app.data.backup.BackupRestorePreview
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavBackupPruneResult
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.policy.DavAutoBackupPolicy
import com.mewbook.app.domain.repository.DavRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DavRepositoryImpl @Inject constructor(
    private val davConfigDao: DavConfigDao,
    private val davRemoteDataSource: DavRemoteDataSource,
    private val backupSnapshotDataSource: BackupSnapshotDataSource
) : DavRepository {

    override fun getDavConfig(): Flow<DavConfig?> {
        return davConfigDao.getDavConfig().map { it?.toDomain() }
    }

    override suspend fun getDavConfigOnce(): DavConfig? {
        return davConfigDao.getDavConfigOnce()?.toDomain()
    }

    override suspend fun saveDavConfig(config: DavConfig) {
        davConfigDao.insertDavConfig(config.toEntity())
    }

    override suspend fun testConnection(config: DavConfig): Result<Boolean> {
        val baseUrl = config.serverUrl.trim().trimEnd('/')
        return davRemoteDataSource.testConnection(baseUrl, config.username, config.password)
    }

    override suspend fun exportData(config: DavConfig, fileName: String?): Result<Boolean> {
        val backupFileName = normalizeManualBackupFileName(fileName)
            ?: davRemoteDataSource.generateBackupFileName()
        return exportBackup(config, backupFileName)
    }

    override suspend fun exportAutoBackupData(config: DavConfig): Result<Boolean> {
        return exportBackup(config, davRemoteDataSource.generateAutoBackupFileName())
    }

    private suspend fun exportBackup(config: DavConfig, backupFileName: String): Result<Boolean> {
        return try {
            val jsonString = backupSnapshotDataSource.exportToJsonString()
            val directoryUrl = davRemoteDataSource.buildDirectoryUrl(config.serverUrl, config.remotePath)
            val fileUrl = davRemoteDataSource.buildFileUrl(config.serverUrl, config.remotePath, backupFileName)

            val dirResult = davRemoteDataSource.mkcol(directoryUrl, config.username, config.password)
            if (dirResult.isFailure) {
                return Result.failure(dirResult.exceptionOrNull() ?: Exception("Failed to create directory"))
            }

            val result = davRemoteDataSource.putFile(fileUrl, config.username, config.password, jsonString)
            if (result.isSuccess) {
                updateLastSyncTime(System.currentTimeMillis())
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun previewImportData(config: DavConfig): Result<BackupRestorePreview> {
        return try {
            val latestBackup = latestBackupFile(config).getOrThrow()
            Result.success(previewImportData(config, latestBackup).getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun previewImportData(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<BackupRestorePreview> {
        return try {
            val jsonString = downloadBackupFile(config, backupFile).getOrThrow()
            val currentEnvelope = BackupMigration.parseToCurrentEnvelope(backupSnapshotDataSource.exportToJsonString())
            val incomingEnvelope = BackupMigration.parseToCurrentEnvelope(jsonString)
            Result.success(BackupMigration.compareEnvelopes(currentEnvelope, incomingEnvelope))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(config: DavConfig): Result<Boolean> {
        return try {
            val latestBackup = latestBackupFile(config).getOrThrow()
            Result.success(importData(config, latestBackup).getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(config: DavConfig, backupFile: DavBackupFile): Result<Boolean> {
        return try {
            val jsonString = downloadBackupFile(config, backupFile).getOrThrow()
            val restoreResult = backupSnapshotDataSource.importFromJsonString(jsonString)
            if (restoreResult.isFailure) {
                return Result.failure(restoreResult.exceptionOrNull() ?: Exception("Failed to import backup"))
            }

            updateLastSyncTime(System.currentTimeMillis())
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSyncTime(time: Long) {
        davConfigDao.updateLastSyncTime(time)
    }

    override suspend fun listBackupFiles(config: DavConfig): Result<List<DavBackupFile>> {
        return try {
            val directoryUrl = davRemoteDataSource.buildDirectoryUrl(config.serverUrl, config.remotePath)
            val propfindResult = davRemoteDataSource.propfind(
                directoryUrl,
                config.username,
                config.password
            )
            if (propfindResult.isFailure) {
                return Result.failure(propfindResult.exceptionOrNull() ?: Exception("Failed to list files"))
            }
            val backupFiles = propfindResult.getOrNull().orEmpty()
                .filter(::isBackupFile)
                .map { href ->
                    val fileUrl = resolveRemoteFileUrl(directoryUrl, href)
                    val rawName = fileUrl.substringAfterLast('/').ifBlank { href.substringAfterLast('/') }
                    val displayName = rawName.removePrefix("manual_")
                    DavBackupFile(
                        displayName = displayName,
                        fileUrl = fileUrl
                    )
                }
                .sortedByDescending { it.displayName }
            if (backupFiles.isEmpty()) {
                Result.failure(Exception("No backup files found"))
            } else {
                Result.success(backupFiles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pruneBackupFiles(
        config: DavConfig,
        keepLatestCount: Int
    ): Result<DavBackupPruneResult> {
        return try {
            val directoryUrl = davRemoteDataSource.buildDirectoryUrl(config.serverUrl, config.remotePath)
            val propfindResult = davRemoteDataSource.propfind(
                directoryUrl,
                config.username,
                config.password
            )
            if (propfindResult.isFailure) {
                return Result.failure(propfindResult.exceptionOrNull() ?: Exception("Failed to list backup files"))
            }

            val filesToDelete = DavAutoBackupPolicy.backupFilesToDelete(
                files = propfindResult.getOrNull().orEmpty(),
                keepLatestCount = keepLatestCount
            )
            val deletedFiles = mutableListOf<String>()
            val failedFiles = mutableListOf<String>()
            filesToDelete.forEach { file ->
                val fileUrl = resolveRemoteFileUrl(directoryUrl, file)
                val deleteResult = davRemoteDataSource.deleteFile(
                    fileUrl,
                    config.username,
                    config.password
                )
                if (deleteResult.isSuccess) {
                    deletedFiles += fileUrl
                } else {
                    failedFiles += fileUrl
                }
            }

            Result.success(
                DavBackupPruneResult(
                    deletedFiles = deletedFiles,
                    failedFiles = failedFiles
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DavConfigEntity.toDomain(): DavConfig {
        return DavConfig(
            id = id,
            serverUrl = serverUrl,
            username = username,
            password = password,
            remotePath = remotePath,
            isEnabled = isEnabled,
            lastSyncTime = lastSyncTime?.let(::fromStoredEpochTime)
        )
    }

    private fun DavConfig.toEntity(): DavConfigEntity {
        return DavConfigEntity(
            id = id,
            serverUrl = serverUrl,
            username = username,
            password = password,
            remotePath = remotePath,
            isEnabled = isEnabled,
            lastSyncTime = lastSyncTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
    }

    private suspend fun latestBackupFile(config: DavConfig): Result<DavBackupFile> {
        return listBackupFiles(config).mapCatching { backupFiles ->
            backupFiles.firstOrNull() ?: error("No backup files found")
        }
    }

    private suspend fun downloadBackupFile(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<String> {
        val getResult = davRemoteDataSource.getFile(
            backupFile.fileUrl,
            config.username,
            config.password
        )
        if (getResult.isFailure) {
            return Result.failure(getResult.exceptionOrNull() ?: Exception("Failed to download file"))
        }
        val jsonString = getResult.getOrNull()
        return if (jsonString.isNullOrEmpty()) {
            Result.failure(Exception("Empty file content"))
        } else {
            Result.success(jsonString)
        }
    }

    private fun isBackupFile(value: String): Boolean {
        val name = value.substringAfterLast('/')
        return name.startsWith("mewbook_backup_", ignoreCase = true) ||
            name.startsWith("mewbook_auto_backup_", ignoreCase = true) ||
            name.startsWith("manual_", ignoreCase = true)
    }

    private fun normalizeManualBackupFileName(fileName: String?): String? {
        val normalized = fileName
            ?.trim()
            ?.replace("""[\s\\/:*?"<>|]+""".toRegex(), "_")
            ?.trim('_', '.')
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val name = if (normalized.endsWith(".json", ignoreCase = true)) {
            normalized
        } else {
            "$normalized.json"
        }
        return if (name.startsWith("mewbook_backup_") || name.startsWith("mewbook_auto_backup_") || name.startsWith("manual_")) {
            name
        } else {
            "manual_$name"
        }
    }

    private fun resolveRemoteFileUrl(directoryUrl: String, latestFile: String): String {
        val trimmed = latestFile.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        val base = if (directoryUrl.endsWith("/")) directoryUrl else "$directoryUrl/"
        return URI(base).resolve(trimmed).toString()
    }

    private fun fromStoredEpochTime(raw: Long): LocalDateTime {
        val epochMillis = if (raw >= 1_000_000_000_000L) raw else raw * 1000
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
