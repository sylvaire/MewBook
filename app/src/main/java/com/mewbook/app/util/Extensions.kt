package com.mewbook.app.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

fun LocalDate.toDisplayString(): String {
    return format(DateTimeFormatter.ofPattern("yyyy年MM月"))
}

fun LocalDate.toShortDisplayString(): String {
    return format(DateTimeFormatter.ofPattern("MM月dd日"))
}

fun YearMonth.toDisplayString(): String {
    return format(DateTimeFormatter.ofPattern("yyyy年MM月"))
}

fun formatCurrency(amount: Double): String {
    return String.format("%.2f", amount)
}

fun Long.toLocalDate(): LocalDate {
    return LocalDate.ofEpochDay(this)
}

fun LocalDate.toEpochDay(): Long {
    return this.toEpochDay()
}
