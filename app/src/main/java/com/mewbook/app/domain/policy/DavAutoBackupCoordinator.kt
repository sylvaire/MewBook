package com.mewbook.app.domain.policy

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.mewbook.app.domain.repository.DavAutoBackupStatusRepository
import com.mewbook.app.domain.repository.DavRepository
import com.mewbook.app.domain.usecase.dav.ExportDataUseCase
import com.mewbook.app.domain.usecase.dav.GetDavConfigUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DavAutoBackupCoordinator @Inject constructor(
    private val getDavConfigUseCase: GetDavConfigUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val davRepository: DavRepository,
    private val statusRepository: DavAutoBackupStatusRepository
) {
    private companion object {
        const val TAG = "DavAutoBackupCoord"
        const val MAX_RETRY_COUNT = 1
        const val RETRY_DELAY_MS = 5_000L
        const val PRUNE_TIMEOUT_MS = 60_000L
    }

    @VisibleForTesting
    internal var pruneTimeoutMs: Long = PRUNE_TIMEOUT_MS

    private val mutex = Mutex()

    suspend fun runIfDue(now: LocalDateTime = LocalDateTime.now()) {
        mutex.withLock {
            val today = now.toLocalDate()
            val config = getDavConfigUseCase.getOnce()
            val lastAttemptDate = statusRepository.getLastAttemptDateOnce()
            if (!DavAutoBackupPolicy.shouldRun(config, lastAttemptDate, today)) {
                return
            }

            statusRepository.recordAttempt(today, now)
            val result = exportWithRetry(config!!)
            if (result.isFailure) {
                statusRepository.recordFailure(
                    "自动备份失败：${result.exceptionOrNull()?.message ?: "未知错误"}"
                )
                return
            }

            val pruneResult = withTimeoutOrNull(pruneTimeoutMs) {
                davRepository.pruneBackupFiles(config, keepLatestCount = 30)
            }
            val message = when {
                pruneResult == null -> {
                    "自动备份成功，但清理旧备份超时"
                }

                pruneResult.isFailure -> {
                    "自动备份成功，但清理旧备份失败：${pruneResult.exceptionOrNull()?.message ?: "未知错误"}"
                }

                pruneResult.getOrNull()?.failedFiles?.isNotEmpty() == true -> {
                    "自动备份成功，但清理旧备份失败：${pruneResult.getOrThrow().failedFiles.size} 个文件"
                }

                else -> null
            }
            statusRepository.recordSuccess(now, message)
        }
    }

    private suspend fun exportWithRetry(
        config: com.mewbook.app.domain.model.DavConfig,
        retryCount: Int = 0
    ): Result<Boolean> {
        val result = exportDataUseCase.autoBackup(config)
        if (result.isSuccess || retryCount >= MAX_RETRY_COUNT) {
            return result
        }

        val error_msg = result.exceptionOrNull()?.message ?: "未知错误"
        Log.w(TAG, "自动备份失败（第${retryCount + 1}次），${RETRY_DELAY_MS}ms 后重试：$error_msg")
        delay(RETRY_DELAY_MS)
        return exportWithRetry(config, retryCount + 1)
    }
}
