package com.example.myalarmapp

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myalarmapp.security.SecurityActionExecutor
import com.example.myalarmapp.security.SecurityPreferences
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme
import kotlinx.coroutines.delay

class MotionVerifyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val prefs = SecurityPreferences(this)
        val correctPin = prefs.lockscreenPin

        setContent {
            MyAlarmAppTheme {
                BackHandler {}

                var timeLeft by remember { mutableStateOf(15) }
                var pinInput by remember { mutableStateOf("") }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(timeLeft) {
                    if (timeLeft > 0) {
                        delay(1000L)
                        timeLeft--
                    } else {
                        SecurityActionExecutor.executeSiren(this@MotionVerifyActivity)
                        val intent = Intent(this@MotionVerifyActivity, IntruderCaptureActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        finish()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 48.dp)
                    ) {
                        Text(
                            text = "⚠️ MOTION DETECTED!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Verify ownership within $timeLeft seconds or siren will sound!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "$timeLeft s",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // PIN Dots
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Enter Security PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in 1..4) {
                                val filled = pinInput.length >= i
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(16.dp)
                                        .background(
                                            if (filled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                )
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

                    // Keypad
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
                                                    if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                }
                                                "✓" -> {
                                                    if (pinInput == correctPin) {
                                                        finish()
                                                    } else {
                                                        errorMessage = "Incorrect PIN."
                                                        pinInput = ""
                                                    }
                                                }
                                                else -> {
                                                    if (pinInput.length < 8) {
                                                       pinInput += digit
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
                }
            }
        }
    }
}
