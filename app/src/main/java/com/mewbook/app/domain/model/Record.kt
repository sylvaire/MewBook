package com.mewbook.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Record(
    val id: Long = 0,
    val amount: Double,
    val type: RecordType,
    val categoryId: Long,
    val note: String?,
    val date: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncId: String?,
    val ledgerId: Long = 1,
    val accountId: Long? = null
)

enum class RecordType {
    INCOME,
    EXPENSE
}

fun Record.isExpense() = type == RecordType.EXPENSE
fun Record.isIncome() = type == RecordType.INCOME
