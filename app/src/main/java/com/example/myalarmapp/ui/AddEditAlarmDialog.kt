package com.example.myalarmapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myalarmapp.data.Alarm
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmDialog(
    alarmToEdit: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val initialHour = alarmToEdit?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = alarmToEdit?.minute ?: calendar.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    var label by remember { mutableStateOf(alarmToEdit?.label ?: "") }
    var selectedDays by remember { mutableStateOf(alarmToEdit?.repeatDays ?: emptySet()) }
    var isSmartAdaptive by remember { mutableStateOf(alarmToEdit?.isSmartAdaptive ?: false) }
    var soundUri by remember { mutableStateOf(alarmToEdit?.soundUri ?: "default") }
    var vibrate by remember { mutableStateOf(alarmToEdit?.vibrate ?: true) }
    var dismissStyle by remember { mutableStateOf(alarmToEdit?.dismissStyle ?: "simple") }
    var snoozeMinutes by remember { mutableStateOf(alarmToEdit?.snoozeMinutes ?: 5) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            soundUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (alarmToEdit == null) "Add Alarm" else "Edit Alarm",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Picker
                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label (e.g. Work, Gym)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Alarm Sound Section
                Text(
                    text = "Alarm Sound",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val ringtoneOptions = listOf(
                    "default" to "🔔 Default Ringtone",
                    "content://settings/system/alarm_alert" to "⏰ Classic Alarm",
                    "content://settings/system/notification_sound" to "🎶 Gentle Chime"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ringtoneOptions.forEach { (uri, name) ->
                        FilterChip(
                            selected = soundUri == uri,
                            onClick = { soundUri = uri },
                            label = { Text(name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (soundUri.startsWith("content://") && !soundUri.contains("settings")) "🎵 Custom Audio Selected" else "📁 Choose Custom Audio from Device",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alarm Dismiss Style Section
                Text(
                    text = "Alarm Dismiss Style",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val dismissOptions = listOf(
                    "simple" to "👆 Simple Tap",
                    "math" to "🧩 Math Puzzle",
                    "shake" to "📳 Shake to Stop"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dismissOptions.forEach { (styleKey, styleName) ->
                        FilterChip(
                            selected = dismissStyle == styleKey,
                            onClick = { dismissStyle = styleKey },
                            label = { Text(styleName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snooze Duration Section
                Text(
                    text = "Snooze Duration",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val snoozeOptions = listOf(1, 5, 10, 15)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    snoozeOptions.forEach { mins ->
                        FilterChip(
                            selected = snoozeMinutes == mins,
                            onClick = { snoozeMinutes = mins },
                            label = { Text("$mins min") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Days Selection
                Text(
                    text = "Repeat Days",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val daysOfWeek = listOf(
                    Calendar.SUNDAY to "S",
                    Calendar.MONDAY to "M",
                    Calendar.TUESDAY to "T",
                    Calendar.WEDNESDAY to "W",
                    Calendar.THURSDAY to "T",
                    Calendar.FRIDAY to "F",
                    Calendar.SATURDAY to "S"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { (dayInt, dayName) ->
                        val isSelected = selectedDays.contains(dayInt)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayInt
                                } else {
                                    selectedDays + dayInt
                                }
                            },
                            label = { Text(dayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Heavy Sleeper Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Heavy Sleeper Vibration", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Intense repeating vibration pattern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Smart Adaptive Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Smart Adaptive", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Check weather & traffic before alarm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSmartAdaptive,
                        onCheckedChange = { isSmartAdaptive = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newAlarm = Alarm(
                        id = alarmToEdit?.id ?: 0,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        isEnabled = true,
                        repeatDays = selectedDays,
                        label = label,
                        soundUri = soundUri,
                        vibrate = vibrate,
                        isSmartAdaptive = isSmartAdaptive,
                        dismissStyle = dismissStyle,
                        snoozeMinutes = snoozeMinutes
                    )
                    onSave(newAlarm)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
