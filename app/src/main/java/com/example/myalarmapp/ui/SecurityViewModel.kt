package com.example.myalarmapp.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.IntruderCaptureActivity
import com.example.myalarmapp.data.CloudSyncManager
import com.example.myalarmapp.data.CloudSyncResult
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import com.example.myalarmapp.security.AntiTheftForegroundService
import com.example.myalarmapp.security.FirebaseAuthManager
import com.example.myalarmapp.security.MotionAntiTheftService
import com.example.myalarmapp.security.SecurityActionExecutor
import com.example.myalarmapp.security.SecurityPreferences
import com.example.myalarmapp.security.TelegramNotifier
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class SecurityStatus(
    val enabled: Boolean,
    val unlocked: Boolean,
    val wrongCount: Int,
    val errorMsg: String?,
    val syncMsg: String?,
    val email: String?,
    val showAuth: Boolean,
    val lockPin: String,
    val lockType: String,
    val lockPass: String,
    val lockEnabled: Boolean,
    val motionEnabled: Boolean,
    val emergencyPhone: String
)

/**
 * ViewModel responsible for managing security logs, PIN lock, custom secret SMS commands, and Firebase Cloud Sync (v2.2).
 */
class SecurityViewModel(
    application: Application,
    private val securityRepository: SecurityRepository
) : AndroidViewModel(application) {

    private val prefs = SecurityPreferences(application)
    private val cloudSyncManager = CloudSyncManager(application)
    private val authManager = FirebaseAuthManager()

    init {
        if (prefs.isCustomLockscreenEnabled) {
            AntiTheftForegroundService.start(application)
        }
        if (prefs.isMotionAntiTheftEnabled) {
            MotionAntiTheftService.start(application)
        }
    }

    private val _isAntiTheftEnabled = MutableStateFlow(true)
    private val _isUnlocked = MutableStateFlow(false)
    private val _wrongPinCount = MutableStateFlow(0)
    private val _pinErrorMessage = MutableStateFlow<String?>(null)
    private val _locateCmd = MutableStateFlow(prefs.locateCommand)
    private val _photoCmd = MutableStateFlow(prefs.photoCommand)
    private val _sirenCmd = MutableStateFlow(prefs.sirenCommand)
    private val _stopCmd = MutableStateFlow(prefs.stopCommand)
    private val _recordCmd = MutableStateFlow(prefs.recordCommand)
    private val _callCmd = MutableStateFlow(prefs.callCommand)
    private val _telegramToken = MutableStateFlow(prefs.telegramToken)
    private val _telegramChatId = MutableStateFlow(prefs.telegramChatId)
    private val _cloudSyncMessage = MutableStateFlow<String?>(null)
    private val _currentUserEmail = MutableStateFlow<String?>(authManager.currentUserEmail)
    private val _showAuthDialog = MutableStateFlow(false)
    private val _lockscreenPin = MutableStateFlow(prefs.lockscreenPin)
    private val _lockscreenType = MutableStateFlow(prefs.lockscreenType)
    private val _lockscreenPassword = MutableStateFlow(prefs.lockscreenPassword)
    private val _isCustomLockscreenEnabled = MutableStateFlow(prefs.isCustomLockscreenEnabled)
    private val _isMotionAntiTheftEnabled = MutableStateFlow(prefs.isMotionAntiTheftEnabled)
    private val _emergencyContact = MutableStateFlow(prefs.emergencyContact)

    private val statusFlow = combine(
        _isAntiTheftEnabled,
        _isUnlocked,
        _wrongPinCount,
        _pinErrorMessage,
        _cloudSyncMessage
    ) { enabled, unlocked, wrongCount, errorMsg, syncMsg ->
        SecurityStatus(enabled, unlocked, wrongCount, errorMsg, syncMsg, _currentUserEmail.value, _showAuthDialog.value, _lockscreenPin.value, _lockscreenType.value, _lockscreenPassword.value, _isCustomLockscreenEnabled.value, _isMotionAntiTheftEnabled.value, _emergencyContact.value)
    }

    private val commandsFlow = combine(
        listOf(_locateCmd, _photoCmd, _sirenCmd, _stopCmd, _recordCmd, _callCmd)
    ) { it.map { v -> v as String } }

    val uiState: StateFlow<SecurityUiState> = combine(
        securityRepository.allLogs,
        statusFlow,
        commandsFlow
    ) { logs, status, commands ->
        SecurityUiState(
            logs = logs,
            isAntiTheftEnabled = status.enabled,
            isUnlocked = status.unlocked,
            wrongPinCount = status.wrongCount,
            pinErrorMessage = status.errorMsg,
            locateCommand = commands[0],
            photoCommand = commands[1],
            sirenCommand = commands[2],
            stopCommand = commands[3],
            recordCommand = commands[4],
            callCommand = commands[5],
            telegramToken = prefs.telegramToken,
            telegramChatId = prefs.telegramChatId,
            cloudSyncMessage = status.syncMsg,
            currentUserEmail = status.email,
            showAuthDialog = status.showAuth,
            lockscreenPin = status.lockPin,
            lockscreenType = status.lockType,
            lockscreenPassword = status.lockPass,
            isCustomLockscreenEnabled = status.lockEnabled,
            isMotionAntiTheftEnabled = status.motionEnabled,
            emergencyContact = status.emergencyPhone,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecurityUiState(isLoading = true)
    )

    fun setShowAuthDialog(show: Boolean) {
        _showAuthDialog.update { show }
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        if (prefs.verifyPin(inputPin)) {
            _isUnlocked.update { true }
            _wrongPinCount.update { 0 }
            _pinErrorMessage.update { null }
            return true
        } else {
            val newCount = _wrongPinCount.value + 1
            _wrongPinCount.update { newCount }
            _pinErrorMessage.update { "Incorrect Security PIN. Attempt $newCount of 3." }

            if (newCount >= 3) {
                try {
                    val intent = Intent(context, IntruderCaptureActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("SecurityViewModel", "Failed to launch IntruderCaptureActivity", e)
                }
            }
            return false
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            val result = authManager.signInWithEmail(email, pass)
            if (result.isSuccess) {
                _currentUserEmail.update { email }
                _cloudSyncMessage.update { "✅ Successfully signed in as $email" }
                _showAuthDialog.update { false }
            } else {
                val errorMsg = getFriendlyErrorMessage(result.exceptionOrNull())
                _cloudSyncMessage.update { "❌ $errorMsg" }
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            val result = authManager.signUpWithEmail(email, pass)
            if (result.isSuccess) {
                _currentUserEmail.update { email }
                _cloudSyncMessage.update { "✅ Account created & signed in as $email" }
                _showAuthDialog.update { false }
            } else {
                val errorMsg = getFriendlyErrorMessage(result.exceptionOrNull())
                _cloudSyncMessage.update { "❌ $errorMsg" }
            }
        }
    }

    private fun getFriendlyErrorMessage(e: Throwable?): String {
        val msg = e?.localizedMessage ?: "Unknown error"
        return when {
            msg.contains("email-already-in-use", ignoreCase = true) -> "This email is already registered. Please sign in instead."
            msg.contains("invalid-credential", ignoreCase = true) || msg.contains("wrong-password", ignoreCase = true) || msg.contains("expired", ignoreCase = true) || msg.contains("malformed", ignoreCase = true) -> "Incorrect email or password. Please check your details or register a new account."
            msg.contains("user-not-found", ignoreCase = true) -> "No account found with this email address. Please register first."
            msg.contains("weak-password", ignoreCase = true) -> "Password must be at least 8 characters with uppercase, lowercase, number, and symbol."
            msg.contains("invalid-email", ignoreCase = true) -> "Please enter a valid email address."
            msg.contains("network", ignoreCase = true) -> "Network error. Please check your internet connection."
            else -> "Incorrect email or password. Please verify your credentials."
        }
    }

    fun signOut() {
        authManager.signOut()
        _currentUserEmail.update { null }
        _cloudSyncMessage.update { "🔒 Signed out from cloud sync." }
        _showAuthDialog.update { false }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim()
                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                    _cloudSyncMessage.update { "❌ Please enter your email address above first." }
                    return@launch
                }
                FirebaseAuth.getInstance().sendPasswordResetEmail(trimmedEmail).await()
                _cloudSyncMessage.update { "✅ Password reset email sent to $trimmedEmail. Check your inbox!" }
            } catch (e: Exception) {
                val errorMsg = getFriendlyErrorMessage(e)
                _cloudSyncMessage.update { "❌ Reset failed: $errorMsg" }
            }
        }
    }

    fun syncLogToCloud(log: SecurityLog) {
        viewModelScope.launch {
            _cloudSyncMessage.update { "☁️ Syncing security log to Firebase Cloud (v2.0)..." }
            val photoUri = log.imagePath?.let { Uri.parse(it) }
            when (val result = cloudSyncManager.uploadLogToCloud(log, photoUri)) {
                is CloudSyncResult.Success -> {
                    _cloudSyncMessage.update { "✅ Cloud Sync Success! Record ID: ${result.cloudRecordId}" }
                }
                is CloudSyncResult.Offline -> {
                    _cloudSyncMessage.update { "📴 ${result.message}" }
                }
                is CloudSyncResult.Error -> {
                    _cloudSyncMessage.update { "❌ Cloud Sync Error: ${result.throwable.localizedMessage}" }
                }
            }
        }
    }

    fun updateSecurityPin(newPin: String) {
        if (newPin.trim().length >= 4) {
            prefs.securityPin = newPin
            _pinErrorMessage.update { null }
            _cloudSyncMessage.update { "✅ Security PIN updated successfully!" }
        } else {
            _cloudSyncMessage.update { "❌ PIN must be at least 4 digits." }
        }
    }

    fun updateLockscreenPin(newPin: String) {
        if (newPin.trim().length >= 4) {
            prefs.lockscreenPin = newPin
            _lockscreenPin.update { newPin }
            _cloudSyncMessage.update { "✅ Shield PIN updated successfully!" }
        } else {
            _cloudSyncMessage.update { "❌ Shield PIN must be at least 4 digits." }
        }
    }

    fun updateLockscreenType(type: String) {
        prefs.lockscreenType = type
        _lockscreenType.update { type }
        _cloudSyncMessage.update { "✅ Lockscreen type updated to $type!" }
    }

    fun updateLockscreenPassword(password: String) {
        if (password.trim().length >= 4) {
            prefs.lockscreenPassword = password
            _lockscreenPassword.update { password }
            _cloudSyncMessage.update { "✅ Shield Password updated successfully!" }
        } else {
            _cloudSyncMessage.update { "❌ Password must be at least 4 characters." }
        }
    }

    fun updateEmergencyContact(phone: String) {
        prefs.emergencyContact = phone
        _emergencyContact.update { phone }
        _cloudSyncMessage.update { "✅ Emergency contact updated to $phone" }
    }

    fun updateTelegramSettings(token: String, chatId: String) {
        prefs.telegramToken = token
        prefs.telegramChatId = chatId
        _telegramToken.update { token }
        _telegramChatId.update { chatId }
        _cloudSyncMessage.update { "✅ Telegram Bot settings saved successfully!" }
    }

    fun testTelegramAlert(context: Context) {
        viewModelScope.launch {
            _cloudSyncMessage.update { "🤖 Sending test message to Telegram Bot..." }
            val success = TelegramNotifier.sendTelegramMessage(
                context,
                "🤖 SmartGuard Test Alert: Telegram Bot notification is working perfectly!"
            )
            if (success) {
                _cloudSyncMessage.update { "✅ Telegram test alert sent successfully!" }
            } else {
                _cloudSyncMessage.update { "❌ Telegram failed. Make sure you tapped 'START' on your bot in Telegram!" }
            }
        }
    }

    fun triggerSos(context: Context) {
        viewModelScope.launch {
            _cloudSyncMessage.update { "🚨 Sending SOS emergency alert..." }
            try {
                SecurityActionExecutor.executeSosAlert(context)
                _cloudSyncMessage.update { "✅ SOS Emergency Alert sent successfully!" }
            } catch (e: Exception) {
                _cloudSyncMessage.update { "❌ SOS failed: ${e.localizedMessage}" }
            }
        }
    }

    fun toggleCustomLockscreen(enabled: Boolean) {
        prefs.isCustomLockscreenEnabled = enabled
        _isCustomLockscreenEnabled.update { enabled }
        if (enabled) {
            AntiTheftForegroundService.start(getApplication())
            _cloudSyncMessage.update { "🛡️ Custom Lockscreen Shield enabled!" }
        } else {
            AntiTheftForegroundService.stop(getApplication())
            _cloudSyncMessage.update { "🛡️ Custom Lockscreen Shield disabled." }
        }
    }

    fun toggleMotionAntiTheft(enabled: Boolean) {
        prefs.isMotionAntiTheftEnabled = enabled
        _isMotionAntiTheftEnabled.update { enabled }
        if (enabled) {
            MotionAntiTheftService.start(getApplication())
            _cloudSyncMessage.update { "🏃‍♂️ Motion Anti-Theft enabled!" }
        } else {
            MotionAntiTheftService.stop(getApplication())
            _cloudSyncMessage.update { "🏃‍♂️ Motion Anti-Theft disabled." }
        }
    }

    fun toggleAntiTheft(enabled: Boolean) {
        _isAntiTheftEnabled.update { enabled }
        _cloudSyncMessage.update { if (enabled) "🛡️ Anti-Theft protection active!" else "⚠️ Anti-Theft protection disabled." }
    }

    fun updateCustomCommands(locate: String, photo: String, siren: String, stop: String, record: String, call: String) {
        prefs.locateCommand = locate
        prefs.photoCommand = photo
        prefs.sirenCommand = siren
        prefs.stopCommand = stop
        prefs.recordCommand = record
        prefs.callCommand = call

        _locateCmd.update { prefs.locateCommand }
        _photoCmd.update { prefs.photoCommand }
        _sirenCmd.update { prefs.sirenCommand }
        _stopCmd.update { prefs.stopCommand }
        _recordCmd.update { prefs.recordCommand }
        _callCmd.update { prefs.callCommand }
        _cloudSyncMessage.update { "✅ Secret SMS commands saved successfully!" }
    }

    fun deleteLog(log: SecurityLog) {
        viewModelScope.launch {
            securityRepository.deleteLog(log)
        }
    }

    fun testPhotoCaptureAndSync(context: Context) {
        viewModelScope.launch {
            _cloudSyncMessage.update { "📸 Capturing test photo & syncing to cloud..." }
            try {
                val photoPath = SecurityActionExecutor.executePhotoCapture(context, "Manual Test")
                val newLog = SecurityLog(
                    eventType = "TEST_PHOTO_CAPTURE",
                    imagePath = photoPath,
                    details = "Manual test from Anti-Theft settings"
                )
                securityRepository.insertLog(newLog)
                syncLogToCloud(newLog)
            } catch (e: Exception) {
                _cloudSyncMessage.update { "❌ Test failed: ${e.localizedMessage}" }
            }
        }
    }
}

/**
 * Factory for creating SecurityViewModel with Application and SecurityRepository dependencies.
 */
class SecurityViewModelFactory(
    private val application: Application,
    private val securityRepository: SecurityRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            return SecurityViewModel(application, securityRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
