package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.DavAutoBackupStatus
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavBackupPruneResult
import com.mewbook.app.domain.model.DavConfig
import com.mewbook.app.domain.repository.DavAutoBackupStatusRepository
import com.mewbook.app.domain.repository.DavRepository
import com.mewbook.app.domain.usecase.dav.ExportDataUseCase
import com.mewbook.app.domain.usecase.dav.GetDavConfigUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DavAutoBackupCoordinatorTest {

    @Test
    fun runIfDue_uploadsOncePerLocalDateAndRecordsSuccess() = runBlocking {
        val davRepository = FakeDavRepository(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            )
        )
        val statusRepository = FakeDavAutoBackupStatusRepository()
        val coordinator = coordinator(davRepository, statusRepository)
        val morning = LocalDateTime.of(2026, 4, 30, 8, 0)
        val evening = LocalDateTime.of(2026, 4, 30, 20, 0)

        coordinator.runIfDue(morning)
        coordinator.runIfDue(evening)

        assertEquals(1, davRepository.autoExportCalls)
        assertEquals(0, davRepository.manualExportCalls)
        assertEquals(1, davRepository.pruneCalls)
        assertEquals(LocalDate.of(2026, 4, 30), statusRepository.status.value.lastAttemptDate)
        assertEquals(morning, statusRepository.status.value.lastSuccessTime)
        assertEquals(null, statusRepository.status.value.lastMessage)
        assertFalse(statusRepository.status.value.lastMessageIsError)
    }

    @Test
    fun runIfDue_recordsWarningWhenBackupSucceedsButPruneReportsFailures() = runBlocking {
        val davRepository = FakeDavRepository(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            ),
            pruneResult = Result.success(
                DavBackupPruneResult(
                    deletedFiles = emptyList(),
                    failedFiles = listOf("https://dav.example.com/MewBook/mewbook_auto_backup_20260401_120000.json")
                )
            )
        )
        val statusRepository = FakeDavAutoBackupStatusRepository()
        val coordinator = coordinator(davRepository, statusRepository)

        coordinator.runIfDue(LocalDateTime.of(2026, 4, 30, 8, 0))

        assertEquals(1, davRepository.autoExportCalls)
        assertEquals(0, davRepository.manualExportCalls)
        assertEquals(1, davRepository.pruneCalls)
        assertTrue(statusRepository.status.value.lastMessage.orEmpty().contains("清理旧备份失败"))
        assertFalse(statusRepository.status.value.lastMessageIsError)
    }

    @Test
    fun runIfDue_recordsFailureAndSkipsPruneWhenBackupFails() = runBlocking {
        val davRepository = FakeDavRepository(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            ),
            exportResult = Result.failure(IllegalStateException("PUT failed: 500"))
        )
        val statusRepository = FakeDavAutoBackupStatusRepository()
        val coordinator = coordinator(davRepository, statusRepository)

        coordinator.runIfDue(LocalDateTime.of(2026, 4, 30, 8, 0))

        assertEquals(2, davRepository.autoExportCalls)
        assertEquals(0, davRepository.manualExportCalls)
        assertEquals(0, davRepository.pruneCalls)
        assertTrue(statusRepository.status.value.lastMessage.orEmpty().contains("PUT failed: 500"))
        assertTrue(statusRepository.status.value.lastMessageIsError)
        assertEquals(null, statusRepository.status.value.lastSuccessTime)
    }

    @Test
    fun runIfDue_recordsTimeoutWhenPruneExceedsLimit() = runBlocking {
        val davRepository = SlowPruneDavRepository(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            ),
            pruneDelayMs = 200L
        )
        val statusRepository = FakeDavAutoBackupStatusRepository()
        val coordinator = coordinator(davRepository, statusRepository)
        coordinator.pruneTimeoutMs = 50L

        coordinator.runIfDue(LocalDateTime.of(2026, 5, 1, 8, 0))

        assertEquals(1, davRepository.autoExportCalls)
        assertEquals(1, davRepository.pruneCalls)
        assertTrue(statusRepository.status.value.lastMessage.orEmpty().contains("清理旧备份超时"))
        assertFalse(statusRepository.status.value.lastMessageIsError)
        assertTrue(statusRepository.status.value.lastSuccessTime != null)
    }

    @Test
    fun runIfDue_recordsSuccessWhenPruneCompletesWithinTimeout() = runBlocking {
        val davRepository = SlowPruneDavRepository(
            config = DavConfig(
                serverUrl = "https://dav.example.com",
                username = "demo",
                password = "pass",
                isEnabled = true
            ),
            pruneDelayMs = 10L
        )
        val statusRepository = FakeDavAutoBackupStatusRepository()
        val coordinator = coordinator(davRepository, statusRepository)
        coordinator.pruneTimeoutMs = 200L

        coordinator.runIfDue(LocalDateTime.of(2026, 5, 1, 8, 0))

        assertEquals(1, davRepository.autoExportCalls)
        assertEquals(1, davRepository.pruneCalls)
        assertEquals(null, statusRepository.status.value.lastMessage)
        assertTrue(statusRepository.status.value.lastSuccessTime != null)
    }

    private fun coordinator(
        davRepository: DavRepository,
        statusRepository: FakeDavAutoBackupStatusRepository
    ): DavAutoBackupCoordinator {
        return DavAutoBackupCoordinator(
            getDavConfigUseCase = GetDavConfigUseCase(davRepository),
            exportDataUseCase = ExportDataUseCase(davRepository),
            davRepository = davRepository,
            statusRepository = statusRepository
        )
    }
}

private class FakeDavRepository(
    private val config: DavConfig?,
    private val exportResult: Result<Boolean> = Result.success(true),
    private val pruneResult: Result<DavBackupPruneResult> = Result.success(
        DavBackupPruneResult(deletedFiles = emptyList(), failedFiles = emptyList())
    )
) : DavRepository {
    var manualExportCalls: Int = 0
    var autoExportCalls: Int = 0
    var pruneCalls: Int = 0

    override fun getDavConfig(): Flow<DavConfig?> = MutableStateFlow(config)

    override suspend fun getDavConfigOnce(): DavConfig? = config

    override suspend fun saveDavConfig(config: DavConfig) = Unit

    override suspend fun testConnection(config: DavConfig): Result<Boolean> = Result.success(true)

    override suspend fun exportData(config: DavConfig, fileName: String?): Result<Boolean> {
        manualExportCalls += 1
        return exportResult
    }

    override suspend fun exportAutoBackupData(config: DavConfig): Result<Boolean> {
        autoExportCalls += 1
        return exportResult
    }

    override suspend fun listBackupFiles(config: DavConfig): Result<List<DavBackupFile>> {
        return Result.success(emptyList())
    }

    override suspend fun previewImportData(config: DavConfig): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun previewImportData(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun importData(config: DavConfig): Result<Boolean> = Result.success(true)

    override suspend fun importData(config: DavConfig, backupFile: DavBackupFile): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun updateLastSyncTime(time: Long) = Unit

    override suspend fun pruneBackupFiles(config: DavConfig, keepLatestCount: Int): Result<DavBackupPruneResult> {
        pruneCalls += 1
        return pruneResult
    }
}

private class SlowPruneDavRepository(
    private val config: DavConfig?,
    private val pruneDelayMs: Long
) : DavRepository {
    var autoExportCalls: Int = 0
    var pruneCalls: Int = 0

    override fun getDavConfig(): Flow<DavConfig?> = MutableStateFlow(config)

    override suspend fun getDavConfigOnce(): DavConfig? = config

    override suspend fun saveDavConfig(config: DavConfig) = Unit

    override suspend fun testConnection(config: DavConfig): Result<Boolean> = Result.success(true)

    override suspend fun exportData(config: DavConfig, fileName: String?): Result<Boolean> = Result.success(true)

    override suspend fun exportAutoBackupData(config: DavConfig): Result<Boolean> {
        autoExportCalls += 1
        return Result.success(true)
    }

    override suspend fun listBackupFiles(config: DavConfig): Result<List<DavBackupFile>> {
        return Result.success(emptyList())
    }

    override suspend fun previewImportData(config: DavConfig): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun previewImportData(
        config: DavConfig,
        backupFile: DavBackupFile
    ): Result<com.mewbook.app.data.backup.BackupRestorePreview> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun importData(config: DavConfig): Result<Boolean> = Result.success(true)

    override suspend fun importData(config: DavConfig, backupFile: DavBackupFile): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun updateLastSyncTime(time: Long) = Unit

    override suspend fun pruneBackupFiles(config: DavConfig, keepLatestCount: Int): Result<DavBackupPruneResult> {
        pruneCalls += 1
        delay(pruneDelayMs)
        return Result.success(
            DavBackupPruneResult(deletedFiles = emptyList(), failedFiles = emptyList())
        )
    }
}

private class FakeDavAutoBackupStatusRepository : DavAutoBackupStatusRepository {
    override val status = MutableStateFlow(DavAutoBackupStatus())

    override suspend fun getLastAttemptDateOnce(): LocalDate? {
        return status.value.lastAttemptDate
    }

    override suspend fun recordAttempt(date: LocalDate, time: LocalDateTime) {
        status.value = status.value.copy(
            lastAttemptDate = date,
            lastAttemptTime = time,
            lastMessage = null,
            lastMessageIsError = false
        )
    }

    override suspend fun recordSuccess(time: LocalDateTime, message: String?) {
        status.value = status.value.copy(
            lastSuccessTime = time,
            lastMessage = message,
            lastMessageIsError = false
        )
    }

    override suspend fun recordFailure(message: String) {
        status.value = status.value.copy(
            lastMessage = message,
            lastMessageIsError = true
        )
    }
}
