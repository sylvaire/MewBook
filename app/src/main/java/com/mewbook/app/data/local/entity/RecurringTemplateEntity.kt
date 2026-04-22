package com.mewbook.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_templates",
    indices = [
        Index(value = ["ledgerId"]),
        Index(value = ["ledgerId", "isEnabled", "nextDueDate"])
    ]
)
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val noteTemplate: String? = null,
    val ledgerId: Long,
    val accountId: Long? = null,
    val scheduleType: String,
    val intervalCount: Int,
    val startDate: Long,
    val nextDueDate: Long,
    val endDate: Long? = null,
    val isEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val lastGeneratedDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
