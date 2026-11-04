package com.example.myalarmapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for executing SQL queries on the security_logs table.
 */
@Dao
interface SecurityDao {

    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SecurityLog>>

    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogsSync(): List<SecurityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog): Long

    @Delete
    suspend fun deleteLog(log: SecurityLog)
}
