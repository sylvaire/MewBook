package com.mewbook.app.data.repository

import com.mewbook.app.data.local.dao.DavConfigDao
import com.mewbook.app.data.local.entity.DavConfigEntity
import com.mewbook.app.data.remote.DavRemoteDataSource
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

        val result = repository.exportData(sampleConfig())

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
    var propfindResponse: Result<List<String>> = Result.success(emptyList())
    var getFileResponse: Result<String> = Result.failure(IllegalStateException("Not configured"))
    var mkcolResponse: Result<Boolean> = Result.success(true)
    var putFileResponse: Result<Boolean> = Result.success(true)

    var lastMkcolUrl: String? = null
    var lastPutFileUrl: String? = null
    var lastPutBody: String? = null
    var lastGetFileUrl: String? = null

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

    override fun generateBackupFileName(): String = generatedBackupFileName

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
