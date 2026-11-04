package com.example.myalarmapp.ui

import com.example.myalarmapp.data.Task

/**
 * UI State representing the state of the task list screen.
 */
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
