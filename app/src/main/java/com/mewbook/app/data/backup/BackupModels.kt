package com.mewbook.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: String,
    val payload: BackupPayload
)

@Serializable
data class BackupPayload(
    val records: List<BackupRecord> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val accounts: List<BackupAccount> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val ledgers: List<BackupLedger> = emptyList(),
    val davConfig: BackupDavConfig? = null,
    val themeMode: String? = null
)

@Serializable
data class BackupRecord(
    val id: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String?,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String?,
    val ledgerId: Long,
    val accountId: Long? = null
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val parentId: Long? = null
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val icon: String,
    val color: Long,
    val isDefault: Boolean,
    val sortOrder: Int,
    val ledgerId: Long
)

@Serializable
data class BackupBudget(
    val id: Long,
    val categoryId: Long?,
    val periodType: String = "MONTH",
    val periodKey: String? = null,
    val month: String? = null,
    val amount: Double,
    val ledgerId: Long
)

@Serializable
data class BackupLedger(
    val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val color: Long,
    val createdAt: Long,
    val isDefault: Boolean = false
)

@Serializable
data class BackupDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val remotePath: String,
    val isEnabled: Boolean,
    val lastSyncTime: Long? = null
)
