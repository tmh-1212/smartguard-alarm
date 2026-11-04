package com.example.myalarmapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * BroadcastReceiver triggered by Android OS when a scheduled alarm fires.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("EXTRA_ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("EXTRA_ALARM_LABEL") ?: "Alarm"
        val soundUri = intent.getStringExtra("EXTRA_ALARM_SOUND") ?: "default"
        val vibrate = intent.getBooleanExtra("EXTRA_HEAVY_VIBRATE", true)

        Log.d("AlarmReceiver", "========================================")
        Log.d("AlarmReceiver", "⏰ REAL OS ALARM TRIGGERED! ID: $alarmId, Label: '$alarmLabel', Sound: $soundUri, Vibrate: $vibrate")
        Log.d("AlarmReceiver", "========================================")

        // Play continuous alarm ringtone audio
        AlarmSoundPlayer.playAlarmSound(context, soundUri)

        // Trigger Heavy Sleeper Enhanced Vibration if enabled
        if (vibrate) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                // Heavy sleeper pattern: repeating pulse
                val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error triggering vibration", e)
            }
        }

        // Show high priority alarm notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showAlarmNotification(alarmId, alarmLabel)
    }
}
