package com.example.myalarmapp.ui

import com.example.myalarmapp.data.Alarm

/**
 * UI State representing the Alarm Home Screen data.
 */
data class AlarmUiState(
    val alarms: List<Alarm> = emptyList(),
    val isLoading: Boolean = false
)
