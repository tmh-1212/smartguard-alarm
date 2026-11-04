package com.example.myalarmapp.ui

import com.example.myalarmapp.data.Alarm

/**
 * Data class representing the state of the Productivity Dashboard.
 */
data class DashboardUiState(
    val greeting: String = "",
    val formattedDate: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val totalActiveHabits: Int = 0,
    val completedHabitsToday: Int = 0,
    val productivityProgress: Float = 0f, // 0.0 to 1.0
    val upcomingAlarm: Alarm? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val aiCoachUiState: AICoachUiState = AICoachUiState.Idle
) {
    val taskSummaryText: String
        get() = "$completedTasks of $totalTasks completed"

    val habitSummaryText: String
        get() = "$completedHabitsToday of $totalActiveHabits completed"

    val productivityPercentageText: String
        get() = "${(productivityProgress * 100).toInt()}%"
}
