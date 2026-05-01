package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.remote.DavRemoteDataSource
import com.mewbook.app.domain.model.DavBackupFile
import com.mewbook.app.domain.model.DavConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DavRepositoryImplTest {

    @Test
    fun exportData_usesUnifiedSnapshotForDavUpload() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource()
        val snapshot = FakeBackupSnapshotDataSource(
            exportJson = """{"schemaVersion":3,"payload":{"records":[]}}"""
        )

        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )

        val result = repository.exportData(sampleConfig(), null)

        assertTrue(result.isSuccess)
        assertEquals(1, snapshot.exportCalls)
        assertEquals("https://dav.example.com/mewbook", remote.lastMkcolUrl)
        assertEquals(
            "https://dav.example.com/mewbook/mewbook_backup_20260418_120000.json",
            remote.lastPutFileUrl
        )
        assertEquals("""{"schemaVersion":3,"payload":{"records":[]}}""", remote.lastPutBody)
        assertNotNull(dao.lastSyncTime)
    }

    @Test
    fun exportData_withCustomFileName_sanitizesNameAndAddsJsonExtension() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource()
        val snapshot = FakeBackupSnapshotDataSource(
            exportJson = """{"schemaVersion":3,"payload":{"records":[]}}"""
        )
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )

        val result = repository.exportData(sampleConfig(), "  April report/final  ")

        assertTrue(result.isSuccess)
        assertEquals(
            "https://dav.example.com/mewbook/April_report_final.json",
            remote.lastPutFileUrl
        )
    }

    @Test
    fun exportAutoBackupData_usesAutomaticBackupFileName() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource()
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.exportAutoBackupData(sampleConfig())

        assertTrue(result.isSuccess)
        assertEquals(
            "https://dav.example.com/mewbook/mewbook_auto_backup_20260418_120000.json",
            remote.lastPutFileUrl
        )
        assertNotNull(dao.lastSyncTime)
    }

    @Test
    fun importData_downloadsLatestBackupAndRestoresWithUnifiedSnapshot() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                listOf(
                    "https://dav.example.com/mewbook/mewbook_backup_20260418_110000.json",
                    "https://dav.example.com/mewbook/mewbook_backup_20260418_130000.json",
                    "https://dav.example.com/mewbook/readme.txt"
                )
            )
            getFileResponse = Result.success("""{"schemaVersion":3}""")
        }
        val snapshot = FakeBackupSnapshotDataSource(
            importResult = Result.success(true)
        )

        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )

        val result = repository.importData(sampleConfig())

        assertTrue(result.isSuccess)
        assertEquals(
            "https://dav.example.com/mewbook/mewbook_backup_20260418_130000.json",
            remote.lastGetFileUrl
        )
        assertEquals("""{"schemaVersion":3}""", snapshot.importedJson)
        assertNotNull(dao.lastSyncTime)
    }

    @Test
    fun importData_whenRestoreFails_doesNotUpdateLastSyncTime() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                listOf("https://dav.example.com/mewbook/mewbook_backup_20260418_130000.json")
            )
            getFileResponse = Result.success("""{"schemaVersion":99}""")
        }
        val snapshot = FakeBackupSnapshotDataSource(
            importResult = Result.failure(IllegalArgumentException("Backup schema v99 is newer than supported v3"))
        )

        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )

        val result = repository.importData(sampleConfig())

        assertTrue(result.isFailure)
        assertNull(dao.lastSyncTime)
        assertEquals("""{"schemaVersion":99}""", snapshot.importedJson)
    }

    @Test
    fun importData_resolvesRelativeHrefToAbsoluteUrl() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                listOf("/dav/MewBook/mewbook_backup_20260418_234137.json")
            )
            getFileResponse = Result.success("""{"schemaVersion":3}""")
        }
        val snapshot = FakeBackupSnapshotDataSource(importResult = Result.success(true))
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )

        val result = repository.importData(
            DavConfig(
                serverUrl = "https://dav.jianguoyun.com/dav/",
                username = "demo",
                password = "secret",
                remotePath = "/MewBook"
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "https://dav.jianguoyun.com/dav/MewBook/mewbook_backup_20260418_234137.json",
            remote.lastGetFileUrl
        )
    }

    @Test
    fun listBackupFiles_returnsResolvedJsonFilesByDisplayNameDescending() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                listOf(
                    "/dav/MewBook/mewbook_backup_20260418_110000.json",
                    "https://dav.example.com/dav/MewBook/mewbook_auto_backup_20260418_130000.json",
                    "/dav/MewBook/custom_april_export.json",
                    "/dav/MewBook/readme.txt",
                    "/dav/MewBook/mewbook_backup_20260418_120000.csv"
                )
            )
        }
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.listBackupFiles(
            DavConfig(
                serverUrl = "https://dav.example.com/dav",
                username = "demo",
                password = "secret",
                remotePath = "/MewBook"
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                "mewbook_backup_20260418_110000.json",
                "mewbook_auto_backup_20260418_130000.json",
                "custom_april_export.json"
            ),
            result.getOrThrow().map { it.displayName }
        )
        assertEquals(
            "https://dav.example.com/dav/MewBook/custom_april_export.json",
            result.getOrThrow()[2].fileUrl
        )
    }

    @Test
    fun previewImportData_downloadsSelectedBackupFile() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            getFileResponse = Result.success(currentBackupJson())
        }
        val snapshot = FakeBackupSnapshotDataSource(
            exportJson = currentBackupJson()
        )
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )
        val selectedBackup = DavBackupFile(
            displayName = "mewbook_backup_20260418_110000.json",
            fileUrl = "https://dav.example.com/MewBook/mewbook_backup_20260418_110000.json"
        )

        val result = repository.previewImportData(sampleConfig(), selectedBackup)

        assertTrue(result.isSuccess)
        assertEquals(selectedBackup.fileUrl, remote.lastGetFileUrl)
    }

    @Test
    fun importData_downloadsSelectedBackupFile() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            getFileResponse = Result.success(currentBackupJson())
        }
        val snapshot = FakeBackupSnapshotDataSource(importResult = Result.success(true))
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = snapshot
        )
        val selectedBackup = DavBackupFile(
            displayName = "mewbook_backup_20260418_110000.json",
            fileUrl = "https://dav.example.com/MewBook/mewbook_backup_20260418_110000.json"
        )

        val result = repository.importData(sampleConfig(), selectedBackup)

        assertTrue(result.isSuccess)
        assertEquals(selectedBackup.fileUrl, remote.lastGetFileUrl)
        assertEquals(currentBackupJson(), snapshot.importedJson)
        assertNotNull(dao.lastSyncTime)
    }

    @Test
    fun getDavConfigOnce_parsesLastSyncMillisCorrectly() = runBlocking {
        val dao = FakeDavConfigDao()
        dao.insertDavConfig(sampleEntity(lastSyncTime = 1_713_456_000_000L))
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = FakeDavRemoteDataSource(),
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.getDavConfigOnce()

        assertEquals(2024, result?.lastSyncTime?.year)
    }

    @Test
    fun getDavConfigOnce_parsesLegacyLastSyncSecondsCorrectly() = runBlocking {
        val dao = FakeDavConfigDao()
        dao.insertDavConfig(sampleEntity(lastSyncTime = 1_713_456_000L))
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = FakeDavRemoteDataSource(),
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.getDavConfigOnce()

        assertEquals(2024, result?.lastSyncTime?.year)
    }

    @Test
    fun pruneBackupFiles_deletesOldestAutomaticHrefsAndKeepsManualFiles() = runBlocking {
        val dao = FakeDavConfigDao()
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                (1..35).map { day ->
                    "/dav/MewBook/mewbook_auto_backup_202604%02d_120000.json".format(day)
                } + listOf(
                    "/dav/MewBook/readme.txt",
                    "/dav/MewBook/custom_manual_export.json",
                    "/dav/MewBook/mewbook_backup_20260401_120000.json",
                    "/dav/MewBook/mewbook_auto_backup_20260401_120000.csv"
                )
            )
        }
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.pruneBackupFiles(
            DavConfig(
                serverUrl = "https://dav.example.com/dav",
                username = "demo",
                password = "secret",
                remotePath = "/MewBook"
            ),
            keepLatestCount = 30
        )

        assertTrue(result.isSuccess)
        assertEquals(
            (1..5).map { day ->
                "https://dav.example.com/dav/MewBook/mewbook_auto_backup_202604%02d_120000.json".format(day)
            },
            remote.deletedUrls
        )
        assertEquals(remote.deletedUrls, result.getOrThrow().deletedFiles)
        assertTrue(result.getOrThrow().failedFiles.isEmpty())
    }

    @Test
    fun pruneBackupFiles_reportsDeleteFailuresWithoutFailingWholePrune() = runBlocking {
        val dao = FakeDavConfigDao()
        val failedHref = "https://dav.example.com/MewBook/mewbook_auto_backup_20260401_120000.json"
        val remote = FakeDavRemoteDataSource().apply {
            propfindResponse = Result.success(
                (1..31).map { day ->
                    "https://dav.example.com/MewBook/mewbook_auto_backup_202604%02d_120000.json".format(day)
                }
            )
            deleteResponses[failedHref] = Result.failure(IllegalStateException("DELETE failed: 500"))
        }
        val repository = DavRepositoryImpl(
            davConfigDao = dao,
            davRemoteDataSource = remote,
            backupSnapshotDataSource = FakeBackupSnapshotDataSource()
        )

        val result = repository.pruneBackupFiles(sampleConfig(), keepLatestCount = 30)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<String>(), result.getOrThrow().deletedFiles)
        assertEquals(listOf(failedHref), result.getOrThrow().failedFiles)
    }

    private fun sampleConfig(): DavConfig {
        return DavConfig(
            serverUrl = "https://dav.example.com",
            username = "demo",
            password = "secret",
            remotePath = "/mewbook"
        )
    }

    private fun sampleEntity(lastSyncTime: Long): DavConfigEntity {
        return DavConfigEntity(
            id = 1,
            serverUrl = "https://dav.example.com",
            username = "demo",
            password = "secret",
            remotePath = "/mewbook",
            isEnabled = true,
            lastSyncTime = lastSyncTime
        )
    }

    private fun currentBackupJson(): String {
        return """{"schemaVersion":4,"appVersion":"test","exportedAt":"2026-04-30T00:00:00","payload":{"records":[],"categories":[],"accounts":[],"budgets":[],"templates":[],"ledgers":[]}}"""
    }
}

private class FakeDavConfigDao : DavConfigDao {
    private val flow = MutableStateFlow<DavConfigEntity?>(null)
    var lastSyncTime: Long? = null

    override fun getDavConfig(): Flow<DavConfigEntity?> = flow

    override suspend fun getDavConfigOnce(): DavConfigEntity? = flow.value

    override suspend fun insertDavConfig(config: DavConfigEntity) {
        flow.value = config
    }

    override suspend fun updateDavConfig(config: DavConfigEntity) {
        flow.value = config
    }

    override suspend fun deleteDavConfig() {
        flow.value = null
    }

    override suspend fun updateLastSyncTime(syncTime: Long) {
        lastSyncTime = syncTime
    }
}

private class FakeDavRemoteDataSource : DavRemoteDataSource {
    var generatedBackupFileName: String = "mewbook_backup_20260418_120000.json"
    var generatedAutoBackupFileName: String = "mewbook_auto_backup_20260418_120000.json"
    var propfindResponse: Result<List<String>> = Result.success(emptyList())
    var getFileResponse: Result<String> = Result.failure(IllegalStateException("Not configured"))
    var mkcolResponse: Result<Boolean> = Result.success(true)
    var putFileResponse: Result<Boolean> = Result.success(true)
    var deleteResponses: MutableMap<String, Result<Boolean>> = mutableMapOf()

    var lastMkcolUrl: String? = null
    var lastPutFileUrl: String? = null
    var lastPutBody: String? = null
    var lastGetFileUrl: String? = null
    var deletedUrls: MutableList<String> = mutableListOf()

    override suspend fun testConnection(serverUrl: String, username: String, password: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun propfind(
        serverUrl: String,
        username: String,
        password: String,
        depth: String
    ): Result<List<String>> = propfindResponse

    override suspend fun getFile(serverUrl: String, username: String, password: String): Result<String> {
        lastGetFileUrl = serverUrl
        return getFileResponse
    }

    override suspend fun putFile(
        serverUrl: String,
        username: String,
        password: String,
        content: String
    ): Result<Boolean> {
        lastPutFileUrl = serverUrl
        lastPutBody = content
        return putFileResponse
    }

    override suspend fun mkcol(serverUrl: String, username: String, password: String): Result<Boolean> {
        lastMkcolUrl = serverUrl
        return mkcolResponse
    }

    override suspend fun deleteFile(serverUrl: String, username: String, password: String): Result<Boolean> {
        return deleteResponses[serverUrl] ?: Result.success(true).also {
            deletedUrls += serverUrl
        }
    }

    override fun generateBackupFileName(): String = generatedBackupFileName

    override fun generateAutoBackupFileName(): String = generatedAutoBackupFileName

    override fun buildDirectoryUrl(serverUrl: String, remotePath: String): String {
        val normalizedPath = remotePath.trim('/').takeIf { it.isNotEmpty() }
        return if (normalizedPath == null) serverUrl.trimEnd('/') else "${serverUrl.trimEnd('/')}/$normalizedPath"
    }

    override fun buildFileUrl(serverUrl: String, remotePath: String, fileName: String): String {
        return "${buildDirectoryUrl(serverUrl, remotePath)}/${fileName.trimStart('/')}"
    }
}

private class FakeBackupSnapshotDataSource(
    private val exportJson: String = """{"schemaVersion":3}""",
    private val importResult: Result<Boolean> = Result.success(true)
) : BackupSnapshotDataSource {
    var exportCalls: Int = 0
    var importedJson: String? = null

    override suspend fun exportToJsonString(): String {
        exportCalls += 1
        return exportJson
    }

    override suspend fun importFromJsonString(jsonString: String): Result<Boolean> {
        importedJson = jsonString
        return importResult
    }
}
