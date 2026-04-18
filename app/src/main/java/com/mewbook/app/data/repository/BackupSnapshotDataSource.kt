package com.mewbook.app.data.repository

interface BackupSnapshotDataSource {
    suspend fun exportToJsonString(): String
    suspend fun importFromJsonString(jsonString: String): Result<Boolean>
}
