package com.mewbook.app.data.remote

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DavClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : DavRemoteDataSource {

    private companion object {
        const val TAG = "DavClient"
    }

    override suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildConnectionProbeRequest(serverUrl, username, password)
            Log.d(TAG, "${request.method} testConnection url=$serverUrl user=${username.take(2)}***")

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "${request.method} response code=${response.code} url=$serverUrl")
                if (response.isSuccessful) {
                    Result.success(true)
                } else if (response.code == 401 || response.code == 403) {
                    Result.failure(IOException("Authentication failed: ${response.code}"))
                } else if (response.code == 404) {
                    Result.failure(IOException("WebDAV endpoint not found: ${response.code}"))
                } else {
                    Result.failure(IOException("Connection failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun buildConnectionProbeRequest(
        serverUrl: String,
        username: String,
        password: String
    ): Request {
        return Request.Builder()
            .url(serverUrl)
            .method(
                "PROPFIND",
                "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"
                    .toRequestBody("application/xml".toMediaType())
            )
            .header("Authorization", Credentials.basic(username, password))
            .header("Depth", "0")
            .build()
    }

    override suspend fun propfind(
        serverUrl: String,
        username: String,
        password: String,
        depth: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "PROPFIND url=$serverUrl depth=$depth")
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>".toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .header("Depth", depth)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "PROPFIND response code=${response.code} url=$serverUrl")
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

    override suspend fun getFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "GET url=$serverUrl")
            val request = Request.Builder()
                .url(serverUrl)
                .get()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "GET response code=${response.code} url=$serverUrl")
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

    override suspend fun putFile(
        serverUrl: String,
        username: String,
        password: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "PUT url=$serverUrl bytes=${content.toByteArray().size}")
            val request = Request.Builder()
                .url(serverUrl)
                .put(content.toRequestBody("application/json".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "PUT response code=${response.code} url=$serverUrl")
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

    override suspend fun deleteFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "DELETE url=$serverUrl")
            val request = buildDeleteRequest(serverUrl, username, password)

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "DELETE response code=${response.code} url=$serverUrl")
                if (response.isSuccessful || response.code == 404) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("DELETE failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun buildDeleteRequest(
        serverUrl: String,
        username: String,
        password: String
    ): Request {
        return Request.Builder()
            .url(serverUrl)
            .delete()
            .header("Authorization", Credentials.basic(username, password))
            .build()
    }

    override suspend fun mkcol(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "MKCOL url=$serverUrl")
            val request = Request.Builder()
                .url(serverUrl)
                .method("MKCOL", null)
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "MKCOL response code=${response.code} url=$serverUrl")
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

    override fun generateBackupFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = LocalDateTime.now().format(formatter)
        return "mewbook_backup_$timestamp.json"
    }

    override fun generateAutoBackupFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = LocalDateTime.now().format(formatter)
        return "mewbook_auto_backup_$timestamp.json"
    }

    override fun buildDirectoryUrl(serverUrl: String, remotePath: String): String {
        val normalizedBase = serverUrl.trim().trimEnd('/')
        val normalizedPath = remotePath.trim().trim('/').takeIf { it.isNotEmpty() }
        return if (normalizedPath == null) {
            normalizedBase
        } else {
            "$normalizedBase/$normalizedPath"
        }
    }

    override fun buildFileUrl(serverUrl: String, remotePath: String, fileName: String): String {
        return "${buildDirectoryUrl(serverUrl, remotePath).trimEnd('/')}/${fileName.trimStart('/')}"
    }

    private fun parsePropfindResponse(xml: String): List<String> {
        val files = mutableListOf<String>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml))

        var insideHref = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    insideHref = parser.name == "href" &&
                        (parser.namespace == null || parser.namespace == "DAV:" || parser.prefix == "d")
                }
                XmlPullParser.TEXT -> {
                    if (insideHref) {
                        val href = parser.text?.trim().orEmpty()
                        if (href.isNotEmpty()) {
                            runCatching {
                                if (href.startsWith("http")) href else URI(href).path
                            }.getOrElse { href }.let { files.add(it) }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    insideHref = false
                }
            }
            eventType = parser.next()
        }
        return files
    }
}
