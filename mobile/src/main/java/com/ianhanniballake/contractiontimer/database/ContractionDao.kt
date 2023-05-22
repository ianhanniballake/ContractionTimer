package com.ianhanniballake.contractiontimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

@Dao
interface ContractionDao {
    @Insert
    suspend fun insert(contraction: Contraction)

    @Update
    suspend fun update(contraction: Contraction)

    @Delete
    suspend fun delete(contraction: Contraction)
}