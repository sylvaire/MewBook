package com.mewbook.app.data.local.database

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long {
        return date.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate {
        return LocalDate.ofEpochDay(epochDay)
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): Long {
        return dateTime.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun toLocalDateTime(epochSecond: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC)
    }
}
