package com.mewbook.app.domain.policy

import com.mewbook.app.domain.model.DavConfig
import java.time.LocalDate

object DavAutoBackupPolicy {
    private const val AUTO_BACKUP_PREFIX = "mewbook_auto_backup_"

    fun shouldRun(
        config: DavConfig?,
        lastAttemptDate: LocalDate?,
        today: LocalDate
    ): Boolean {
        return config != null &&
            config.isEnabled &&
            config.isConfigured() &&
            lastAttemptDate != today
    }

    fun backupFilesToDelete(
        files: List<String>,
        keepLatestCount: Int
    ): List<String> {
        if (keepLatestCount < 0) {
            return emptyList()
        }
        val backupFiles = files
            .filter(::isAutomaticBackupFile)
            .sorted()
        val deleteCount = backupFiles.size - keepLatestCount
        return if (deleteCount > 0) backupFiles.take(deleteCount) else emptyList()
    }

    private fun isAutomaticBackupFile(value: String): Boolean {
        return value.substringAfterLast('/').startsWith(AUTO_BACKUP_PREFIX) &&
            value.endsWith(".json", ignoreCase = true)
    }
}
