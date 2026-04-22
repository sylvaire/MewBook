package com.mewbook.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class RecurringTemplate(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: RecordType,
    val categoryId: Long,
    val noteTemplate: String? = null,
    val ledgerId: Long = 1,
    val accountId: Long? = null,
    val scheduleType: RecurringTemplateScheduleType,
    val intervalCount: Int,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val endDate: LocalDate? = null,
    val isEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val lastGeneratedDate: LocalDate? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

enum class RecurringTemplateScheduleType {
    WEEKLY,
    MONTHLY,
    CUSTOM_DAYS
}
