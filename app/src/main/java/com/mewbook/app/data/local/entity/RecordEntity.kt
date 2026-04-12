package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["ledgerId"]),
        Index(value = ["date"])
    ]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String?,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncId: String?,
    val ledgerId: Long,         // 关联账本
    val accountId: Long? = null  // 关联账户（可选）
)
