package com.mewbook.app.data.remote

import android.util.Log
import android.util.Xml
import com.mewbook.app.BuildConfig
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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
        const val PROPFIND_BODY = "<?xml version=\"1.0\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"

        fun mapHttpError(code: Int): String = when (code) {
            401, 403 -> "认证失败（$code），请检查用户名和应用密码"
            404 -> "路径不存在（404），请检查服务器地址和远程路径"
            405 -> "服务器不支持此操作（405）"
            409 -> "创建目录失败（409），路径可能已存在但不是目录"
            502, 503, 504 -> "服务器暂时不可用（$code），请稍后重试"
            507 -> "服务器存储空间不足（507）"
            else -> "请求失败（$code）"
        }

        fun mapConnectionError(e: Exception): String = when (e) {
            is SocketTimeoutException -> "连接超时，请检查服务器地址或网络"
            is UnknownHostException -> "无法解析服务器地址，请检查 URL 是否正确"
            is IOException -> "网络错误：${e.message ?: "未知错误"}"
            else -> e.message ?: "未知错误"
        }
    }

    override suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildConnectionProbeRequest(serverUrl, username, password)
            if (BuildConfig.DEBUG) Log.d(TAG, "${request.method} testConnection url=$serverUrl user=${username.take(2)}***")

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "${request.method} response code=${response.code} url=$serverUrl")
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
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
                PROPFIND_BODY
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
            if (BuildConfig.DEBUG) Log.d(TAG, "PROPFIND url=$serverUrl depth=$depth")
            val request = Request.Builder()
                .url(serverUrl)
                .method("PROPFIND", PROPFIND_BODY.toRequestBody("application/xml".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .header("Depth", depth)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "PROPFIND response code=${response.code} url=$serverUrl")
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val files = parsePropfindResponse(body)
                    Result.success(files)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
        }
    }

    override suspend fun getFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "GET url=$serverUrl")
            val request = Request.Builder()
                .url(serverUrl)
                .get()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "GET response code=${response.code} url=$serverUrl")
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Result.success(body)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
        }
    }

    override suspend fun putFile(
        serverUrl: String,
        username: String,
        password: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "PUT url=$serverUrl bytes=${content.toByteArray().size}")
            val request = Request.Builder()
                .url(serverUrl)
                .put(content.toRequestBody("application/json".toMediaType()))
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "PUT response code=${response.code} url=$serverUrl")
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
        }
    }

    override suspend fun deleteFile(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "DELETE url=$serverUrl")
            val request = buildDeleteRequest(serverUrl, username, password)

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "DELETE response code=${response.code} url=$serverUrl")
                if (response.isSuccessful || response.code == 404) {
                    Result.success(true)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
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
            if (BuildConfig.DEBUG) Log.d(TAG, "MKCOL url=$serverUrl")
            val request = Request.Builder()
                .url(serverUrl)
                .method("MKCOL", null)
                .header("Authorization", Credentials.basic(username, password))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "MKCOL response code=${response.code} url=$serverUrl")
                if (response.isSuccessful || response.code == 405) {
                    Result.success(true)
                } else {
                    Result.failure(IOException(mapHttpError(response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(mapConnectionError(e)))
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
