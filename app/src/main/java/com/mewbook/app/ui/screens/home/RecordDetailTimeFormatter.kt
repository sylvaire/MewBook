package com.mewbook.app.ui.screens.home

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object RecordDetailTimeFormatter {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm:ss", Locale.SIMPLIFIED_CHINESE)

    fun format(date: LocalDate, timeSource: LocalDateTime): String {
        return date.atTime(timeSource.toLocalTime()).format(formatter)
    }
}
