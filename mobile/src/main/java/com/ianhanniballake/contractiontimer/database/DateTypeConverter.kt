package com.ianhanniballake.contractiontimer.database

import androidx.room.TypeConverter
import java.util.Date

object DateTypeConverter {
    @TypeConverter
    fun fromTimestamp(timestamp: Long?): Date? {
        return if (timestamp == null || timestamp == 0L) null else Date(timestamp)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return if (date == null || date.time == 0L) null else date.time
    }
}