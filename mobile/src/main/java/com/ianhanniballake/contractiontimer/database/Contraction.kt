package com.ianhanniballake.contractiontimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "contractions")
data class Contraction(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        val id: Long = 0,
        @ColumnInfo(name = "start_time")
        val startTime: Date = Date(),
        @ColumnInfo(name = "end_time")
        val endTime: Date? = null,
        val note: String = ""
)