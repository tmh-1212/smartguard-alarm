package com.example.myalarmapp.security

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.myalarmapp.MotionVerifyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Foreground service monitoring accelerometer with WakeLock to reliably detect pickup/movement
 * even when the screen is off, triggering the 15-second verification countdown.
 */
class MotionAntiTheftService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isTriggered = false
    private val startTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartGuard::MotionWakeLock")
        try {
            wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes max */)
        } catch (_: Exception) {}

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Motion Guard Active 🏃‍♂️")
            .setContentText("Pocket & Motion Anti-Theft Protection is running")
            .setSmallIcon(R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(102, notification)

        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isTriggered) return

        // Ignore sensor events for the first 3 seconds to prevent startup false triggers
        if (System.currentTimeMillis() - startTime < 3000L) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // If sudden movement occurs (gForce > 1.5)
            if (gForce > 1.5f) {
                isTriggered = true
                val prefs = SecurityPreferences(this)
                if (prefs.isMotionAntiTheftEnabled) {
                    try {
                        val intent = Intent(this, MotionVerifyActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                }

                // Reset trigger after 10 seconds so it can re-arm
                CoroutineScope(Dispatchers.IO).launch {
                    delay(10000L)
                    isTriggered = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        try {
            wakeLock?.release()
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Motion Guard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "motion_guard_channel"

        fun start(context: Context) {
            val intent = Intent(context, MotionAntiTheftService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MotionAntiTheftService::class.java)
            context.stopService(intent)
        }
    }
}
