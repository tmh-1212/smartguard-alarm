package com.example.myalarmapp

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myalarmapp.security.AppLockState
import com.example.myalarmapp.security.SecurityPreferences
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Secure Lockscreen Shield Activity supporting PIN, Alphanumeric Password, and Device Biometric/Fingerprint/Face unlock.
 * Captures intruder photo and GPS on 3 failed attempts.
 */
class CustomLockscreenActivity : ComponentActivity() {

    private var failedAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnOnScreenAndShowOverLockscreen()

        val prefs = SecurityPreferences(this)
        val lockType = prefs.lockscreenType
        val correctSecret = if (lockType == "password") prefs.lockscreenPassword else prefs.lockscreenPin

        if (lockType == "system") {
            showSystemBiometricPrompt()
        }

        setContent {
            MyAlarmAppTheme {
                BackHandler {}

                CustomLockscreen(
                    lockType = lockType,
                    correctSecret = correctSecret,
                    onUnlockSuccess = {
                        AppLockState.unlock()
                        finish()
                    },
                    onUnlockFailed = {
                        failedAttempts++
                        if (failedAttempts >= 3) {
                            failedAttempts = 0
                            val intent = Intent(this, IntruderCaptureActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                    },
                    onRetrySystemLock = {
                        if (lockType == "system") {
                            showSystemBiometricPrompt()
                        }
                    }
                )
            }
        }
    }

    private fun showSystemBiometricPrompt() {
        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "SmartGuard Security Shield",
                "Verify your Fingerprint, Face, PIN, or Pattern to unlock"
            )
            if (intent != null) {
                startActivityForResult(intent, 2002)
            }
        } catch (_: Exception) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2002) {
            if (resultCode == RESULT_OK) {
                AppLockState.unlock()
                finish()
            }
        }
    }

    private fun turnOnScreenAndShowOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun CustomLockscreen(
    lockType: String,
    correctSecret: String,
    onUnlockSuccess: () -> Unit,
    onUnlockFailed: () -> Unit,
    onRetrySystemLock: () -> Unit
) {
    var secretInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    val currentTime = timeFormat.format(Date())
    val currentDate = dateFormat.format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Time & Date
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = currentTime,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🛡️ SmartGuard Protected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Input Section based on lockType
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (lockType == "password") {
                Text(
                    text = "Enter Security Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = secretInput,
                    onValueChange = { secretInput = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (secretInput.trim() == correctSecret.trim()) {
                            onUnlockSuccess()
                        } else {
                            errorMessage = "Incorrect Password. Try again."
                            secretInput = ""
                            onUnlockFailed()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
            } else if (lockType == "system") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Device Biometric / Fingerprint / Face Lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = onRetrySystemLock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Verify Identity (Fingerprint / Face / Pattern)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                // PIN Mode (Default)
                Text(
                    text = "Enter Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val dotCount = correctSecret.length.coerceIn(4, 8)
                    for (i in 1..dotCount) {
                        val filled = secretInput.length >= i
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(14.dp)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Keypad for PIN mode
        if (lockType == "pin") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("⌫", "0", "✓")
                )

                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        row.forEach { digit ->
                            Button(
                                onClick = {
                                    when (digit) {
                                        "⌫" -> {
                                            if (secretInput.isNotEmpty()) secretInput = secretInput.dropLast(1)
                                        }
                                        "✓" -> {
                                            if (secretInput == correctSecret) {
                                                onUnlockSuccess()
                                            } else {
                                                errorMessage = "Incorrect PIN. Try again."
                                                secretInput = ""
                                                onUnlockFailed()
                                            }
                                        }
                                        else -> {
                                            if (secretInput.length < 8) {
                                                secretInput += digit
                                                errorMessage = null
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(72.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
