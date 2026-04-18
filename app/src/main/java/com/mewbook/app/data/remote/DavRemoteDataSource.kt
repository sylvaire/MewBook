package com.mewbook.app.data.remote

interface DavRemoteDataSource {
    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean>

    suspend fun propfind(
        serverUrl: String,
        username: String,
        password: String,
        depth: String = "1"
    ): Result<List<String>>

    suspend fun getFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<String>

    suspend fun putFile(
        serverUrl: String,
        username: String,
        password: String,
        content: String
    ): Result<Boolean>

    suspend fun mkcol(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean>

    fun generateBackupFileName(): String

    fun buildDirectoryUrl(serverUrl: String, remotePath: String): String

    fun buildFileUrl(serverUrl: String, remotePath: String, fileName: String): String
}
