package com.mewbook.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.mewbook.app.domain.model.AppUpdateAsset
import com.mewbook.app.domain.model.AppUpdateRelease
import com.mewbook.app.domain.policy.AppUpdatePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percent: Int?
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            null
        }
}

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkLatestRelease(currentVersionName: String): AppUpdateRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestReleaseApiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "MewBook-Android")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("检查更新失败：GitHub 返回 ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val release = json.decodeFromString(GitHubReleaseResponse.serializer(), body)
            AppUpdatePolicy.toAvailableReleaseOrNull(
                tagName = release.tagName,
                releaseName = release.name,
                htmlUrl = release.htmlUrl,
                notes = release.body,
                assets = release.assets.map { asset ->
                    AppUpdateAsset(
                        name = asset.name,
                        downloadUrl = asset.browserDownloadUrl,
                        sizeBytes = asset.size
                    )
                },
                currentVersionName = currentVersionName
            )
        }
    }

    suspend fun downloadReleaseApk(
        release: AppUpdateRelease,
        onProgress: (AppUpdateDownloadProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadManager = context.getSystemService(DownloadManager::class.java)
            ?: error("系统下载服务不可用")
        val destinationSubPath = "updates/${safeFileName(release.asset.name)}"
        val downloadRoot = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: error("无法访问下载目录")
        val destinationFile = File(downloadRoot, destinationSubPath)
        destinationFile.parentFile?.mkdirs()
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(release.asset.downloadUrl))
            .setTitle("喵喵记账 ${release.versionName}")
            .setDescription("正在下载更新安装包")
            .setMimeType(apkMimeType)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, destinationSubPath)

        val downloadId = downloadManager.enqueue(request)
        waitForDownload(downloadManager, downloadId, destinationFile, onProgress)
    }

    private suspend fun waitForDownload(
        downloadManager: DownloadManager,
        downloadId: Long,
        destinationFile: File,
        onProgress: (AppUpdateDownloadProgress) -> Unit
    ): File {
        val query = DownloadManager.Query().setFilterById(downloadId)
        while (true) {
            downloadManager.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    error("下载任务已丢失")
                }
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                onProgress(AppUpdateDownloadProgress(downloadedBytes = downloaded, totalBytes = total))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        if (!destinationFile.exists()) {
                            error("下载完成但安装包不存在")
                        }
                        return destinationFile
                    }

                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        error("下载失败：$reason")
                    }

                    else -> Unit
                }
            } ?: error("无法查询下载状态")
            delay(downloadPollIntervalMillis)
        }
    }

    private fun safeFileName(name: String): String {
        val cleaned = name.replace(Regex("""[^\w.\-]"""), "_")
        return cleaned.takeIf { it.endsWith(".apk", ignoreCase = true) } ?: "MewBook-update.apk"
    }

    companion object {
        private const val latestReleaseApiUrl = "https://api.github.com/repos/sylvaire/MewBook/releases/latest"
        private const val apkMimeType = "application/vnd.android.package-archive"
        private const val downloadPollIntervalMillis = 600L
    }
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val body: String? = null,
    val assets: List<GitHubReleaseAssetResponse> = emptyList()
)

@Serializable
private data class GitHubReleaseAssetResponse(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Long = 0
)
