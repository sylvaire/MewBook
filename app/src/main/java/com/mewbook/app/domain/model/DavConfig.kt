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
    fun isConfigured(): Boolean = serverUrl.isNotBlank() && username.isNotBlank()
}
