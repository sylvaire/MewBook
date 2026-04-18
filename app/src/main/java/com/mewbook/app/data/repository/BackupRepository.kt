package com.mewbook.app.data.repository

import android.content.Context
import android.net.Uri
import com.mewbook.app.BuildConfig
import com.mewbook.app.data.backup.BackupAccount
import com.mewbook.app.data.backup.BackupBudget
import com.mewbook.app.data.backup.BackupCategory
import com.mewbook.app.data.backup.BackupDavConfig
import com.mewbook.app.data.backup.BackupEnvelope
import com.mewbook.app.data.backup.BackupLedger
import com.mewbook.app.data.backup.BackupMigration
import com.mewbook.app.data.backup.BackupPayload
import com.mewbook.app.data.backup.BackupRecord
import com.mewbook.app.data.local.dao.AccountDao
import com.mewbook.app.data.local.dao.BudgetDao
import com.mewbook.app.data.local.dao.CategoryDao
import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.dao.LedgerDao
import com.mewbook.app.data.local.dao.RecordDao
import com.mewbook.app.data.local.database.MewBookDatabase
import com.mewbook.app.data.local.entity.AccountEntity
import com.mewbook.app.data.local.entity.BudgetEntity
import com.mewbook.app.data.local.entity.CategoryEntity
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.local.entity.LedgerEntity
import com.mewbook.app.data.local.entity.RecordEntity
import com.mewbook.app.data.preferences.ThemePreferencesRepository
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MewBookDatabase,
    private val recordDao: RecordDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val ledgerDao: LedgerDao,
    private val davConfigDao: DavConfigDao,
    private val themePreferencesRepository: ThemePreferencesRepository
) : BackupSnapshotDataSource {

    override suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        BackupMigration.encodeEnvelope(buildCurrentEnvelope())
    }

    suspend fun exportToUri(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = exportToJsonString()
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                requireNotNull(writer) { "无法写入备份文件" }
                writer.write(jsonString)
                writer.flush()
            }
            true
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
                requireNotNull(reader) { "无法读取备份文件" }
                reader.readText()
            }
            importFromJsonString(jsonString).getOrThrow()
        }
    }

    override suspend fun importFromJsonString(jsonString: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = BackupMigration.parseToCurrentEnvelope(jsonString)
            restoreEnvelope(envelope)
            true
        }
    }

    fun generateBackupFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "mewbook_backup_v${BackupMigration.CURRENT_SCHEMA_VERSION}_${timestamp}.json"
    }

    private suspend fun buildCurrentEnvelope(): BackupEnvelope {
        return BackupEnvelope(
            schemaVersion = BackupMigration.CURRENT_SCHEMA_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            payload = BackupPayload(
                records = recordDao.getAllRecordsOnce().map { it.toBackup() },
                categories = categoryDao.getAllCategoriesOnce().map { it.toBackup() },
                accounts = accountDao.getAllAccountsOnce().map { it.toBackup() },
                budgets = budgetDao.getAllBudgetsOnce().map { it.toBackup() },
                ledgers = ledgerDao.getAllLedgersOnce().map { it.toBackup() },
                davConfig = davConfigDao.getDavConfigOnce()?.toBackup(),
                themeMode = themePreferencesRepository.getThemeModeOnce().storageValue
            )
        )
    }

    private suspend fun restoreEnvelope(envelope: BackupEnvelope) {
        database.withTransaction {
            recordDao.deleteAllRecords()
            budgetDao.deleteAllBudgets()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
            ledgerDao.deleteAllLedgers()
            davConfigDao.deleteDavConfig()

            if (envelope.payload.ledgers.isNotEmpty()) {
                ledgerDao.insertLedgers(envelope.payload.ledgers.map { it.toEntity() })
            }
            if (envelope.payload.categories.isNotEmpty()) {
                categoryDao.insertCategories(envelope.payload.categories.map { it.toEntity() })
            }
            if (envelope.payload.accounts.isNotEmpty()) {
                accountDao.insertAccounts(envelope.payload.accounts.map { it.toEntity() })
            }
            if (envelope.payload.budgets.isNotEmpty()) {
                budgetDao.insertBudgets(envelope.payload.budgets.map { it.toEntity() })
            }
            if (envelope.payload.records.isNotEmpty()) {
                recordDao.insertRecords(envelope.payload.records.map { it.toEntity() })
            }
            envelope.payload.davConfig?.let { davConfigDao.insertDavConfig(it.toEntity()) }
            themePreferencesRepository.setThemeMode(
                com.mewbook.app.data.preferences.AppThemeMode.fromStorageValue(envelope.payload.themeMode)
            )
        }
    }

    private fun RecordEntity.toBackup() = BackupRecord(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        note = note,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncId = syncId,
        ledgerId = ledgerId,
        accountId = accountId
    )

    private fun CategoryEntity.toBackup() = BackupCategory(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type,
        isDefault = isDefault,
        sortOrder = sortOrder,
        parentId = parentId
    )

    private fun AccountEntity.toBackup() = BackupAccount(
        id = id,
        name = name,
        type = type,
        balance = balance,
        icon = icon,
        color = color,
        isDefault = isDefault,
        sortOrder = sortOrder,
        ledgerId = ledgerId
    )

    private fun BudgetEntity.toBackup() = BackupBudget(
        id = id,
        categoryId = categoryId,
        month = month,
        amount = amount,
        ledgerId = ledgerId
    )

    private fun LedgerEntity.toBackup() = BackupLedger(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        createdAt = createdAt,
        isDefault = isDefault
    )

    private fun DavConfigEntity.toBackup() = BackupDavConfig(
        serverUrl = serverUrl,
        username = username,
        password = password,
        remotePath = remotePath,
        isEnabled = isEnabled,
        lastSyncTime = lastSyncTime
    )

    private fun BackupRecord.toEntity() = RecordEntity(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        note = note,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncId = syncId,
        ledgerId = ledgerId,
        accountId = accountId
    )

    private fun BackupCategory.toEntity() = CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type,
        isDefault = isDefault,
        sortOrder = sortOrder,
        parentId = parentId
    )

    private fun BackupAccount.toEntity() = AccountEntity(
        id = id,
        name = name,
        type = type,
        balance = balance,
        icon = icon,
        color = color,
        isDefault = isDefault,
        sortOrder = sortOrder,
        ledgerId = ledgerId
    )

    private fun BackupBudget.toEntity() = BudgetEntity(
        id = id,
        categoryId = categoryId,
        month = month,
        amount = amount,
        ledgerId = ledgerId
    )

    private fun BackupLedger.toEntity() = LedgerEntity(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        createdAt = createdAt,
        isDefault = isDefault
    )

    private fun BackupDavConfig.toEntity() = DavConfigEntity(
        id = 1,
        serverUrl = serverUrl,
        username = username,
        password = password,
        remotePath = remotePath,
        isEnabled = isEnabled,
        lastSyncTime = lastSyncTime
    )
}
