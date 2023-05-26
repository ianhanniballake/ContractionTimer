package com.ianhanniballake.contractiontimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractionDao {
    @Insert
    suspend fun insert(contraction: Contraction)

    @Update
    suspend fun update(contraction: Contraction)

    @Delete
    suspend fun delete(contraction: Contraction)

    @Query("SELECT * FROM contractions ORDER BY start_time DESC")
    fun allContractions(): Flow<List<Contraction>>

    @Query("SELECT * FROM contractions ORDER BY start_time DESC LIMIT 1")
    fun latestContraction(): Flow<Contraction?>

    @Query("""SELECT _id, start_time, end_time,'' as note FROM contractions
        WHERE start_time >= :timeCutOffInMillis ORDER BY start_time""")
    suspend fun contractionsBackTo(timeCutOffInMillis: Long): List<Contraction>
}