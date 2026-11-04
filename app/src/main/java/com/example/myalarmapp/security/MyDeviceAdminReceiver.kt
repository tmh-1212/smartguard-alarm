package com.example.myalarmapp.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myalarmapp.IntruderCaptureActivity

/**
 * Receiver enabling Device Administrator status to prevent uninstallation and
 * detect failed lockscreen password attempts to capture intruder photo and GPS.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    private var failedAttempts = 0

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("DeviceAdminReceiver", "Device Admin uninstall protection enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("DeviceAdminReceiver", "Device Admin uninstall protection disabled")
    }

    @Suppress("DEPRECATION")
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        failedAttempts++
        Log.w("DeviceAdminReceiver", "Lockscreen password failed attempt: $failedAttempts")

        if (failedAttempts >= 3) {
            failedAttempts = 0 // reset
            try {
                val captureIntent = Intent(context, IntruderCaptureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(captureIntent)
                Log.d("DeviceAdminReceiver", "Triggered IntruderCaptureActivity from lockscreen failure")
            } catch (e: Exception) {
                Log.e("DeviceAdminReceiver", "Failed to launch IntruderCaptureActivity", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        failedAttempts = 0
    }
}
