package com.mewbook.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DavClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .head()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 401) {
                    Result.success(response.code == 401)
                } else {
                    Result.failure(IOException("Connection failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun propfind(
        serverUrl: String,
        username: String,
        password: String,
        depth: String = "1"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>".toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .header("Depth", depth)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val files = parsePropfindResponse(body)
                    Result.success(files)
                } else {
                    Result.failure(IOException("PROPFIND failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .get()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Result.success(body)
                } else {
                    Result.failure(IOException("GET failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun putFile(
        serverUrl: String,
        username: String,
        password: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .put(content.toRequestBody("application/json".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("PUT failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mkcol(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .method("MKCOL", null)
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 405) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("MKCOL failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateBackupFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = LocalDateTime.now().format(formatter)
        return "mewbook_backup_$timestamp.json"
    }

    private fun parsePropfindResponse(xml: String): List<String> {
        val files = mutableListOf<String>()
        val hrefRegex = "<d:href>(.*?)</d:href>".toRegex()
        hrefRegex.findAll(xml).forEach { match ->
            files.add(match.groupValues[1])
        }
        return files
    }
}
