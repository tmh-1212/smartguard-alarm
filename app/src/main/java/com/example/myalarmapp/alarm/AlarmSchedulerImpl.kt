package com.example.myalarmapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.myalarmapp.data.Alarm
import java.util.Calendar

/**
 * Android system AlarmManager implementation for scheduling real OS-level alarms, snoozes, and smart adaptive checks.
 */
class AlarmSchedulerImpl(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmSchedulerImpl", "Cannot schedule exact alarms: permission not granted.")
            }
        }

        val triggerTimeMs = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarm.id)
            putExtra("EXTRA_ALARM_LABEL", alarm.label)
            putExtra("EXTRA_ALARM_SOUND", alarm.soundUri)
            putExtra("EXTRA_HEAVY_VIBRATE", alarm.vibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)

        // If Smart Adaptive is enabled, check weather and traffic conditions and show advisory if needed
        if (alarm.isSmartAdaptive) {
            try {
                val conditions = WeatherTrafficRepository().fetchConditions()
                if (conditions.trafficDelayMinutes > 0) {
                    NotificationHelper(context).showSmartAdvisoryNotification(
                        title = "Smart Weather & Traffic Advisory",
                        message = conditions.advisoryMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("AlarmSchedulerImpl", "Error checking weather/traffic conditions", e)
            }
        }

        Log.d("AlarmSchedulerImpl", "Scheduled Alarm ID ${alarm.id} for $triggerTimeMs ms (SmartAdaptive: ${alarm.isSmartAdaptive})")
    }

    override fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmSchedulerImpl", "Canceled Alarm ID ${alarm.id}")
    }

    override fun scheduleSnooze(alarmId: Int, alarmLabel: String, snoozeMinutes: Int) {
        val triggerTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarmId)
            putExtra("EXTRA_ALARM_LABEL", if (alarmLabel.contains("Snoozed")) alarmLabel else "$alarmLabel (Snoozed)")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)

        Log.d("AlarmSchedulerImpl", "Snoozed Alarm ID $alarmId for $snoozeMinutes min ($triggerTimeMs ms)")
    }

    companion object {
        fun calculateNextTriggerTime(hour: Int, minute: Int, repeatDays: Set<Int>): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (repeatDays.isNotEmpty()) {
                while (!repeatDays.contains(calendar.get(Calendar.DAY_OF_WEEK).toCustomDayInt())) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            return calendar.timeInMillis
        }

        private fun Int.toCustomDayInt(): Int {
            return when (this) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }
    }
}
