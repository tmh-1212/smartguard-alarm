package com.example.myalarmapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myalarmapp.data.CloudSyncManager
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground activity launched in the background to initiate a silent phone call back to the owner
 * with Proximity Screen-Off WakeLock so the screen turns pitch black and untouchable during the call.
 */
class SpyCallActivity : ComponentActivity() {

    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off screen hardware completely using Proximity Screen-Off WakeLock
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        try {
            @Suppress("DEPRECATION")
            proximityWakeLock = powerManager?.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "SmartGuard::ProximityWakeLock"
            )
            proximityWakeLock?.acquire(30000L) // 30 seconds max screen off
        } catch (e: Exception) {
            // Fallback if proximity wake lock is restricted on some OEM devices
        }

        val targetPhone = intent.getStringExtra("EXTRA_TARGET_PHONE") ?: ""
        val subscriptionId = intent.getIntExtra("EXTRA_SUB_ID", -1)

        executeInvisibleCall(targetPhone, subscriptionId)
    }

    @SuppressLint("MissingPermission")
    private fun executeInvisibleCall(targetPhone: String, subscriptionId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (targetPhone.isNotBlank()) {
                    val hasCall = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    if (hasCall) {
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$targetPhone")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (subscriptionId != -1) {
                                putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subscriptionId)
                                putExtra("subscription", subscriptionId)
                            }
                        }
                        startActivity(callIntent)

                        val app = application as AlarmApplication
                        app.database.securityDao().let { dao ->
                            val newLog = SecurityLog(
                                eventType = "SPY_CALL_INITIATED",
                                details = "Proximity screen-off stealth auto-call initiated to $targetPhone"
                            )
                            SecurityRepository(dao).insertLog(newLog)
                            CloudSyncManager(applicationContext).uploadLogToCloud(newLog, null)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    proximityWakeLock?.release()
                } catch (_: Exception) {}
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            proximityWakeLock?.release()
        } catch (_: Exception) {}
    }
}
