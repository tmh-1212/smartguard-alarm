package com.example.myalarmapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data Model and Room Entity representing a security breach log (e.g., wrong PIN entry).
 */
@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "WRONG_PIN", "POCKET_TRIGGER", etc.
    val imagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val details: String? = null
)
