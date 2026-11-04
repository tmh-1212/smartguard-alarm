package com.example.myalarmapp.security

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myalarmapp.AlarmApplication
import com.example.myalarmapp.AudioRecordActivity
import com.example.myalarmapp.IntruderCaptureActivity
import com.example.myalarmapp.SpyCallActivity
import com.example.myalarmapp.data.CloudSyncManager
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Executor responsible for handling anti-theft security actions such as sending location via SMS,
 * capturing intruder photos, sounding a loud siren, recording remote audio, and initiating silent spy calls.
 */
object SecurityActionExecutor {

    private var sirenMediaPlayer: MediaPlayer? = null

    private fun getSmsManager(context: Context, subscriptionId: Int): SmsManager {
        return try {
            if (subscriptionId != -1) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                context.getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
            }
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    fun executeLocate(context: Context, senderPhone: String, subscriptionId: Int = -1) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationHelper = LocationHelper(context)
                val location = locationHelper.getCurrentLocation()

                val message = if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    "MyAlarmApp Anti-Theft: Device Location -> https://maps.google.com/?q=$lat,$lng"
                } else {
                    "MyAlarmApp Anti-Theft: Unable to determine current GPS location."
                }

                // Send SMS reply back silently using exact same SIM subscription slot
                val smsManager = getSmsManager(context, subscriptionId)
                smsManager.sendTextMessage(senderPhone, null, message, null, null)
                Log.d("SecurityActionExecutor", "Location SMS sent silently to $senderPhone using subId $subscriptionId")

                // Log event and automatically sync to cloud
                val app = context.applicationContext as? AlarmApplication
                app?.database?.securityDao()?.let { dao ->
                    val newLog = SecurityLog(
                        eventType = "LOCATION_SENT",
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        details = "Sent to $senderPhone"
                    )
                    SecurityRepository(dao).insertLog(newLog)
                    CloudSyncManager(context.applicationContext).uploadLogToCloud(newLog, null)
                }
            } catch (e: Exception) {
                Log.e("SecurityActionExecutor", "Failed to execute locate action", e)
            }
        }
    }

    suspend fun executePhotoCapture(context: Context, senderPhone: String, subscriptionId: Int = -1): String? {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "intruder_capture_alert_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Intruder Capture Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Trigger intruder photo capture"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val fullScreenIntent = Intent(context, IntruderCaptureActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Security Alert 🚨")
                .setContentText("Capturing intruder photo...")
                .setSmallIcon(R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(2002, notification)

            val message = "MyAlarmApp Anti-Theft: Intruder photo capture triggered via SMS command."
            val smsManager = getSmsManager(context, subscriptionId)
            smsManager.sendTextMessage(senderPhone, null, message, null, null)
            Log.d("SecurityActionExecutor", "Photo capture notification sent and SMS replied silently to $senderPhone")
            null
        } catch (e: Exception) {
            Log.e("SecurityActionExecutor", "Failed to execute photo capture action", e)
            null
        }
    }

    fun executeAudioRecording(context: Context, senderPhone: String, subscriptionId: Int = -1) {
        try {
            val intent = Intent(context, AudioRecordActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val smsManager = getSmsManager(context, subscriptionId)
            smsManager.sendTextMessage(senderPhone, null, "MyAlarmApp Anti-Theft: 10s audio recording triggered.", null, null)
            Log.d("SecurityActionExecutor", "AudioRecordActivity launched and SMS sent silently to $senderPhone")
        } catch (e: Exception) {
            Log.e("SecurityActionExecutor", "Failed to execute audio recording", e)
        }
    }

    fun executeSpyCall(context: Context, targetPhone: String, subscriptionId: Int = -1) {
        try {
            val intent = Intent(context, SpyCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("EXTRA_TARGET_PHONE", targetPhone)
                putExtra("EXTRA_SUB_ID", subscriptionId)
            }
            context.startActivity(intent)
            Log.d("SecurityActionExecutor", "SpyCallActivity launched for $targetPhone with subId $subscriptionId")
        } catch (e: Exception) {
            Log.e("SecurityActionExecutor", "Failed to execute spy call", e)
        }
    }

    fun executeSiren(context: Context) {
        try {
            if (sirenMediaPlayer?.isPlaying == true) return

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            sirenMediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d("SecurityActionExecutor", "Loud siren started playing")

            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as? AlarmApplication
                app?.database?.securityDao()?.let { dao ->
                    val newLog = SecurityLog(
                        eventType = "SIREN_ACTIVATED",
                        details = "Loud alarm siren sounded remotely"
                    )
                    SecurityRepository(dao).insertLog(newLog)
                    CloudSyncManager(context.applicationContext).uploadLogToCloud(newLog, null)
                }
            }
        } catch (e: Exception) {
            Log.e("SecurityActionExecutor", "Failed to play siren", e)
        }
    }

    fun stopSiren(context: Context? = null) {
        try {
            sirenMediaPlayer?.stop()
            sirenMediaPlayer?.release()
            sirenMediaPlayer = null
            Log.d("SecurityActionExecutor", "Siren stopped")
        } catch (e: Exception) {
            Log.e("SecurityActionExecutor", "Failed to stop siren", e)
        }
    }

    fun executeSosAlert(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = SecurityPreferences(context)
                val phone = prefs.emergencyContact
                if (phone.isBlank()) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "⚠️ Please enter an Emergency Phone Number first", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val hasSendSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                if (!hasSendSms) {
                    Log.e("SecurityActionExecutor", "SEND_SMS permission not granted for SOS")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "❌ SEND_SMS permission required. Please enable in Permissions Setup.", Toast.LENGTH_LONG).show()
                    }
                    val app = context.applicationContext as? AlarmApplication
                    app?.database?.securityDao()?.let { dao ->
                        SecurityRepository(dao).insertLog(
                            SecurityLog(
                                eventType = "SOS_FAILED",
                                details = "SEND_SMS permission not granted"
                            )
                        )
                    }
                    return@launch
                }

                val locationHelper = LocationHelper(context)
                val location = locationHelper.getCurrentLocation()

                val message = if (location != null) {
                    "🚨 SOS EMERGENCY ALERT! I need immediate help. My current location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "🚨 SOS EMERGENCY ALERT! I need immediate help. Location unavailable."
                }

                val smsManager = try {
                    context.getSystemService(SmsManager::class.java) ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                } catch (_: Exception) {
                    null
                }

                if (smsManager == null) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "❌ SMS Manager is null on this device", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                smsManager.sendTextMessage(phone, null, message, null, null)
                Log.d("SecurityActionExecutor", "SOS emergency SMS sent to $phone")

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "✅ SOS Emergency SMS sent successfully to $phone", Toast.LENGTH_LONG).show()
                }

                val app = context.applicationContext as? AlarmApplication
                app?.database?.securityDao()?.let { dao ->
                    SecurityRepository(dao).insertLog(
                        SecurityLog(
                            eventType = "SOS_ALERT_SENT",
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            details = "Emergency SOS sent to $phone"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("SecurityActionExecutor", "Failed to execute SOS alert", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "❌ SOS Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
