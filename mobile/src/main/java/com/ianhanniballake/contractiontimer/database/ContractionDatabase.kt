package com.ianhanniballake.contractiontimer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
        entities = [Contraction::class],
        version = 3,
        autoMigrations = [
            AutoMigration(from = 2, to = 3)
        ]
)
@TypeConverters(DateTypeConverter::class)
abstract class ContractionDatabase : RoomDatabase() {
    abstract fun contractionDao(): ContractionDao

    companion object {
        @Volatile
        private var instance: ContractionDatabase? = null

        fun getInstance(context: Context): ContractionDatabase {
            val applicationContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(applicationContext,
                        ContractionDatabase::class.java, "contractions.db")
                        .fallbackToDestructiveMigrationFrom(1)
                        .build().also { database ->
                            instance = database
                        }
            }
        }
    }
}
