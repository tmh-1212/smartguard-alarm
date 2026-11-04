package com.example.myalarmapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myalarmapp.R
import com.example.myalarmapp.RingingActivity

/**
 * Helper class for managing Notification Channels and displaying high-priority alarm notifications.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority channel for active alarm notifications"
                setBypassDnd(true)
                enableVibration(true)
                setSound(alarmUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)

            val advisoryChannel = NotificationChannel(
                ADVISORY_CHANNEL_ID,
                "Smart Alarm Advisories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Advisory channel for traffic and weather alarm adjustments"
            }
            notificationManager.createNotificationChannel(advisoryChannel)
        }
    }

    fun showAlarmNotification(alarmId: Int, alarmLabel: String) {
        val ringingIntent = Intent(context, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ALARM_ID", alarmId)
            putExtra("EXTRA_ALARM_LABEL", alarmLabel)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Alarm Ringing")
            .setContentText(if (alarmLabel.isBlank()) "Alarm is ringing!" else alarmLabel)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        notificationManager.notify(alarmId, notification)
    }

    fun showSmartAdvisoryNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, ADVISORY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(99999, notification)
    }

    fun cancelNotification(alarmId: Int) {
        notificationManager.cancel(alarmId)
    }

    companion object {
        const val CHANNEL_ID = "ALARM_NOTIFICATION_CHANNEL"
        const val ADVISORY_CHANNEL_ID = "ADVISORY_NOTIFICATION_CHANNEL"
    }
}
