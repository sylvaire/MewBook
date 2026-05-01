package com.mewbook.app.domain.model

import java.time.LocalDateTime

data class DavConfig(
    val id: Long = 1,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/MewBook",
    val isEnabled: Boolean = false,
    val lastSyncTime: LocalDateTime? = null
) {
    fun isConfigured(): Boolean = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

data class DavBackupPruneResult(
    val deletedFiles: List<String>,
    val failedFiles: List<String>
)

data class DavBackupFile(
    val displayName: String,
    val fileUrl: String
)

data class DavAutoBackupStatus(
    val lastAttemptDate: java.time.LocalDate? = null,
    val lastAttemptTime: LocalDateTime? = null,
    val lastSuccessTime: LocalDateTime? = null,
    val lastMessage: String? = null,
    val lastMessageIsError: Boolean = false
)
