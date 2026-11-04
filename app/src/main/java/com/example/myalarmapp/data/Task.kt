package com.example.myalarmapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Priority levels for a task.
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Data Model and Room Entity representing a single productivity task stored in the SQLite database.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val dueDateTime: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
