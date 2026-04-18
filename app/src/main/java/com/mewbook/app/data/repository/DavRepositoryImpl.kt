package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.remote.DavRemoteDataSource
import com.mewbook.app.domain.model.DavConfig
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

    override suspend fun exportData(config: DavConfig): Result<Boolean> {
        return try {
            val jsonString = backupSnapshotDataSource.exportToJsonString()
            val backupFileName = davRemoteDataSource.generateBackupFileName()
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

    override suspend fun importData(config: DavConfig): Result<Boolean> {
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

            val files = propfindResult.getOrNull() ?: emptyList()
            val backupFiles = files.filter { it.contains("mewbook_backup_") && it.endsWith(".json") }

            if (backupFiles.isEmpty()) {
                return Result.failure(Exception("No backup files found"))
            }

            val latestFile = backupFiles.maxByOrNull { it } ?: return Result.failure(Exception("No backup files found"))
            val fileUrl = resolveRemoteFileUrl(directoryUrl, latestFile)

            val getResult = davRemoteDataSource.getFile(fileUrl, config.username, config.password)
            if (getResult.isFailure) {
                return Result.failure(getResult.exceptionOrNull() ?: Exception("Failed to download file"))
            }

            val jsonString = getResult.getOrNull() ?: return Result.failure(Exception("Empty file content"))
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
