package com.example.myalarmapp.data

import androidx.room.TypeConverter

/**
 * Room TypeConverter for converting complex types (Set<Int> and TaskPriority) into SQLite-compatible types.
 */
class Converters {

    @TypeConverter
    fun fromRepeatDaysSet(days: Set<Int>?): String {
        return days?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toRepeatDaysSet(data: String?): Set<Int> {
        if (data.isNullOrBlank()) return emptySet()
        return data.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority {
        return try {
            value?.let { TaskPriority.valueOf(it) } ?: TaskPriority.MEDIUM
        } catch (e: IllegalArgumentException) {
            TaskPriority.MEDIUM
        }
    }
}
