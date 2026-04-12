package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dav_config")
data class DavConfigEntity(
    @PrimaryKey
    val id: Long = 1,
    val serverUrl: String,
    val username: String,
    val password: String,
    val remotePath: String,
    val isEnabled: Boolean,
    val lastSyncTime: Long?
)
