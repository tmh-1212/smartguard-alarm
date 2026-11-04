package com.example.myalarmapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Data Model and Room Entity representing a single Alarm stored in SQLite database.
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val soundUri: String = "default",
    val vibrate: Boolean = true,
    val isSmartAdaptive: Boolean = false,
    val dismissStyle: String = "simple", // "simple", "math", or "shake"
    val snoozeMinutes: Int = 5
) {
    /**
     * Formats hour and minute into 12-hour display string (e.g., "07:05").
     */
    val formattedTime: String
        get() {
            val hour12 = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            return String.format(Locale.getDefault(), "%02d:%02d", hour12, minute)
        }

    /**
     * Returns "AM" or "PM".
     */
    val amPm: String
        get() = if (hour >= 12) "PM" else "AM"

    /**
     * Returns human-readable string for repeat days.
     */
    val formattedDays: String
        get() {
            if (repeatDays.isEmpty()) return "Once"
            if (repeatDays.size == 7) return "Everyday"
            if (repeatDays == setOf(1, 2, 3, 4, 5)) return "Weekdays"
            if (repeatDays == setOf(6, 7)) return "Weekends"

            val dayNames = mapOf(
                1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
                5 to "Fri", 6 to "Sat", 7 to "Sun"
            )
            return repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        }
}
