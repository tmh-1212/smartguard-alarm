package com.example.myalarmapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data Model and Room Entity representing a completed Pomodoro/Focus session.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val durationMinutes: Int,
    val sessionType: String, // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    val timestamp: Long = System.currentTimeMillis()
)
