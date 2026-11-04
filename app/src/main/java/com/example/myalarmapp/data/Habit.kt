package com.example.myalarmapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Data Model and Room Entity representing a single habit to track.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Data Model and Room Entity representing a daily completion record for a habit.
 * Enforces uniqueness on (habitId, completionDate) so a habit can only be completed once per day.
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "completionDate"], unique = true)
    ]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,
    val completionDate: String // Stored as "YYYY-MM-DD"
)
