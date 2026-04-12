package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledgers")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,          // PERSONAL, FAMILY, AA
    val icon: String,
    val color: Long,
    val createdAt: Long,
    val isDefault: Boolean = false
)
