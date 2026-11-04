package com.example.myalarmapp.ui

import com.example.myalarmapp.data.Habit

/**
 * Represents a habit along with its today's completion status and current consecutive streak.
 */
data class HabitWithStreak(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val currentStreak: Int
)

/**
 * UI State representing the state of habits list screen.
 */
data class HabitUiState(
    val habitsWithStreak: List<HabitWithStreak> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
