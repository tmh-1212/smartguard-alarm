package com.example.myalarmapp.ui

import com.example.myalarmapp.data.SecurityLog

/**
 * UI State representing security logs, anti-theft PIN lock status, custom commands, and Cloud Sync v2.0 status.
 */
data class SecurityUiState(
    val logs: List<SecurityLog> = emptyList(),
    val isAntiTheftEnabled: Boolean = true,
    val isUnlocked: Boolean = false,
    val wrongPinCount: Int = 0,
    val pinErrorMessage: String? = null,
    val locateCommand: String = "#LOCATE#",
    val photoCommand: String = "#GET_PHOTO#",
    val sirenCommand: String = "#SIREN#",
    val stopCommand: String = "#STOP#",
    val recordCommand: String = "#RECORD#",
    val callCommand: String = "#CALL#",
    val telegramToken: String = "",
    val telegramChatId: String = "",
    val cloudSyncMessage: String? = null,
    val currentUserEmail: String? = null,
    val showAuthDialog: Boolean = false,
    val lockscreenPin: String = "1234",
    val lockscreenType: String = "pin",
    val lockscreenPassword: String = "1234",
    val isCustomLockscreenEnabled: Boolean = false,
    val isMotionAntiTheftEnabled: Boolean = false,
    val emergencyContact: String = "",
    val isLoading: Boolean = false
)
