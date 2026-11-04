package com.example.myalarmapp

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.myalarmapp.alarm.AlarmSchedulerImpl
import com.example.myalarmapp.alarm.AlarmSoundPlayer
import com.example.myalarmapp.alarm.NotificationHelper
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * Full-screen Ringing Activity shown over lockscreen when an alarm fires, supporting Simple Tap, Math Puzzle, and Shake-to-Stop.
 */
class RingingActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var dismissStyle = "simple"
    private var alarmId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnOnScreenAndShowOverLockscreen()
        enableEdgeToEdge()

        alarmId = intent.getIntExtra("EXTRA_ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("EXTRA_ALARM_LABEL") ?: "Alarm"

        val app = application as AlarmApplication
        lifecycleScope.launch {
            if (alarmId != -1) {
                val alarm = app.repository.getAlarmById(alarmId)
                if (alarm != null) {
                    dismissStyle = alarm.dismissStyle
                }
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        setContent {
            MyAlarmAppTheme {
                RingingScreen(
                    alarmLabel = alarmLabel,
                    dismissStyle = dismissStyle,
                    onDismiss = {
                        dismissAlarm(alarmId)
                    },
                    onSnooze = {
                        snoozeAlarm(alarmId, alarmLabel)
                    }
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (dismissStyle == "shake" && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
            if (gForce > 2.5f) {
                Toast.makeText(this, "Phone shaken! Alarm dismissed 📳", Toast.LENGTH_SHORT).show()
                dismissAlarm(alarmId)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun dismissAlarm(alarmId: Int) {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}

        AlarmSoundPlayer.stopAlarmSound()
        NotificationHelper(this).cancelNotification(alarmId)

        val app = application as AlarmApplication
        lifecycleScope.launch {
            if (alarmId != -1) {
                val alarm = app.repository.getAlarmById(alarmId)
                if (alarm != null && alarm.repeatDays.isEmpty()) {
                    app.repository.updateAlarm(alarm.copy(isEnabled = false))
                }
            }
            finish()
        }
    }

    private fun snoozeAlarm(alarmId: Int, alarmLabel: String) {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}

        AlarmSoundPlayer.stopAlarmSound()
        NotificationHelper(this).cancelNotification(alarmId)

        val scheduler = AlarmSchedulerImpl(applicationContext)
        scheduler.scheduleSnooze(alarmId, alarmLabel, snoozeMinutes = 5)
        Toast.makeText(this, "Alarm snoozed for 5 minutes 💤", Toast.LENGTH_SHORT).show()
        finish()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        AlarmSoundPlayer.stopAlarmSound()
    }
}

@Composable
fun RingingScreen(
    alarmLabel: String,
    dismissStyle: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val currentTime = timeFormat.format(Date())

    val num1 = remember { (10..50).random() }
    val num2 = remember { (10..50).random() }
    val correctAnswer = num1 + num2

    var showMathDialog by remember { mutableStateOf(false) }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    if (showMathDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🧩 Math Puzzle Challenge", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Solve this math problem to stop the alarm:")
                    Text(
                        text = "$num1 + $num2 = ?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = mathInput,
                        onValueChange = { mathInput = it },
                        label = { Text("Enter Answer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mathError) {
                        Text("Incorrect answer. Try again!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mathInput.trim().toIntOrNull() == correctAnswer) {
                            onDismiss()
                        } else {
                            mathError = true
                            mathInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify & Stop Alarm", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⏰ ALARM RINGING",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentTime,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (alarmLabel.isNotBlank()) {
                Text(
                    text = alarmLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dismissStyle == "shake") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📳 Shake your phone vigorously to stop!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(56.dp)
            ) {
                Text("Snooze (5 min)", fontSize = 16.sp)
            }

            Button(
                onClick = {
                    if (dismissStyle == "math") {
                        showMathDialog = true
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (dismissStyle == "shake") "Tap or Shake" else "Dismiss",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
