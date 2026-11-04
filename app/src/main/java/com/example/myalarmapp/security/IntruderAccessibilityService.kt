package com.example.myalarmapp.security

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.myalarmapp.CustomLockscreenActivity

/**
 * Accessibility Service monitoring app launches across the device to provide
 * App Lock / App Shield protection (including Settings app) that remains unlocked until screen turns off.
 */
class IntruderAccessibilityService : AccessibilityService() {

    private var lastPackageTime = 0L
    private var lastPackage = ""

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                AppLockState.lock() // Re-arm lock when screen turns off
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            val prefs = SecurityPreferences(this)

            if (prefs.isCustomLockscreenEnabled) {
                // If already unlocked in this session (until screen turns off), do not re-lock
                if (AppLockState.isUnlockedSession) {
                    return
                }

                // Exclude home screen launchers and system UI, but allow Settings so it can be locked by App Shield!
                val isLauncher = packageName.contains("launcher") || packageName.contains("systemui") || packageName.contains("resolver")
                val isOurApp = packageName == "com.example.myalarmapp"

                if (!isLauncher && !isOurApp && packageName.isNotBlank()) {
                    val currentTime = System.currentTimeMillis()
                    if (packageName != lastPackage || currentTime - lastPackageTime > 3000) {
                        lastPackage = packageName
                        lastPackageTime = currentTime
                        Log.d("IntruderAccessService", "App launched: $packageName. Triggering CustomLockscreenActivity.")

                        try {
                            val intent = Intent(this, CustomLockscreenActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("IntruderAccessService", "Failed to launch CustomLockscreenActivity", e)
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("IntruderAccessService", "Accessibility Service interrupted")
    }
}
