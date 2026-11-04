package com.example.myalarmapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for executing SQL queries on the focus_sessions table.
 */
@Dao
interface FocusDao {

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE sessionType = 'FOCUS'")
    fun getTotalFocusMinutes(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long
}
