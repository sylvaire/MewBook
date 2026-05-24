package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deleted_records",
    indices = [
        Index(value = ["deletedAt"]),
        Index(value = ["date"]),
        Index(value = ["ledgerId"])
    ]
)
data class DeletedRecordEntity(
    @PrimaryKey
    val recordId: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String?,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String?,
    val ledgerId: Long,
    val accountId: Long? = null,
    val deletedAt: Long
)
