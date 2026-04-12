package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.CategoryDao
import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.remote.DavClient
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.model.RecordType
import com.mewbook.app.domain.repository.DavRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DavRepositoryImpl @Inject constructor(
    private val davConfigDao: DavConfigDao,
    private val recordDao: RecordDao,
    private val categoryDao: CategoryDao,
    private val davClient: DavClient
) : DavRepository {

    private val json = Json { prettyPrint = true }

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
        return davClient.testConnection(config.serverUrl, config.username, config.password)
    }

    override suspend fun exportData(config: DavConfig): Result<Boolean> {
        return try {
            val records = recordDao.getAllRecordsOnce()
            val categories = categoryDao.getAllCategoriesOnce()

            val exportData = ExportData(
                version = 2,
                exportTime = LocalDateTime.now().toString(),
                records = records.map { record ->
                    ExportRecord(
                        id = record.id,
                        amount = record.amount,
                        type = record.type,
                        categoryId = record.categoryId,
                        note = record.note,
                        date = record.date,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                        syncId = record.syncId ?: UUID.randomUUID().toString(),
                        ledgerId = record.ledgerId,
                        accountId = record.accountId
                    )
                },
                categories = categories.map { category ->
                    ExportCategory(
                        id = category.id,
                        name = category.name,
                        icon = category.icon,
                        color = category.color,
                        type = category.type,
                        isDefault = category.isDefault,
                        sortOrder = category.sortOrder,
                        parentId = category.parentId
                    )
                }
            )

            val jsonString = json.encodeToString(exportData)
            val backupFileName = davClient.generateBackupFileName()
            val remotePath = "${config.remotePath.trimEnd('/')}/$backupFileName"

            val dirResult = davClient.mkcol(config.serverUrl, config.username, config.password)
            if (dirResult.isFailure) {
                return Result.failure(dirResult.exceptionOrNull() ?: Exception("Failed to create directory"))
            }

            val result = davClient.putFile(remotePath, config.username, config.password, jsonString)
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
            val propfindResult = davClient.propfind(
                config.serverUrl,
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

            val fileUrl = if (latestFile.startsWith("http")) latestFile else "${config.serverUrl.trimEnd('/')}/$latestFile"

            val getResult = davClient.getFile(fileUrl, config.username, config.password)
            if (getResult.isFailure) {
                return Result.failure(getResult.exceptionOrNull() ?: Exception("Failed to download file"))
            }

            val jsonString = getResult.getOrNull() ?: return Result.failure(Exception("Empty file content"))
            val exportData = json.decodeFromString<ExportData>(jsonString)

            categoryDao.deleteNonDefaultCategories()
            val categoryEntities = exportData.categories.map { category ->
                com.mewbook.app.data.local.entity.CategoryEntity(
                    id = category.id,
                    name = category.name,
                    icon = category.icon,
                    color = category.color,
                    type = category.type,
                    isDefault = category.isDefault,
                    sortOrder = category.sortOrder,
                    parentId = category.parentId
                )
            }
            categoryDao.insertCategories(categoryEntities)

            recordDao.deleteAllRecords()
            val recordEntities = exportData.records.map { record ->
                com.mewbook.app.data.local.entity.RecordEntity(
                    id = record.id,
                    amount = record.amount,
                    type = record.type,
                    categoryId = record.categoryId,
                    note = record.note,
                    date = record.date,
                    createdAt = record.createdAt,
                    updatedAt = record.updatedAt,
                    syncId = record.syncId,
                    ledgerId = record.ledgerId,
                    accountId = record.accountId
                )
            }
            recordDao.insertRecords(recordEntities)

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
            lastSyncTime = lastSyncTime?.let {
                LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC)
            }
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
            lastSyncTime = lastSyncTime?.toEpochSecond(ZoneOffset.UTC)
        )
    }
}

@Serializable
data class ExportData(
    val version: Int,
    val exportTime: String,
    val records: List<ExportRecord>,
    val categories: List<ExportCategory>
)

@Serializable
data class ExportRecord(
    val id: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String?,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String,
    val ledgerId: Long,
    val accountId: Long?
)

@Serializable
data class ExportCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val parentId: Long?
)
