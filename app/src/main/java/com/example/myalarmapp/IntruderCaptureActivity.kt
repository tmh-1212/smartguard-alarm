package com.example.myalarmapp

import android.app.KeyguardManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.myalarmapp.data.CloudSyncManager
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import com.example.myalarmapp.security.LocationHelper
import com.example.myalarmapp.security.SecretCameraHelper
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground transparent activity shown over lockscreen to silently capture real front-camera photo
 * and GPS location concurrently, syncing to Firebase Cloud.
 */
class IntruderCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnOnScreenAndShowOverLockscreen()

        setContent {
            MyAlarmAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                }
            }
        }

        captureAndSyncIntruder()
    }

    private fun captureAndSyncIntruder() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch camera photo and GPS location concurrently for speed and reliability
                val cameraDeferred = async {
                    val cameraHelper = SecretCameraHelper(applicationContext)
                    cameraHelper.captureFrontCameraPhoto()
                }

                val locationDeferred = async {
                    val locationHelper = LocationHelper(applicationContext)
                    locationHelper.getCurrentLocation()
                }

                val photoPath = cameraDeferred.await()
                val location = locationDeferred.await()

                val newLog = SecurityLog(
                    eventType = "INTRUDER_PIN_BREACH",
                    imagePath = photoPath,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    details = "3 failed PIN attempts detected (Foreground Captured)"
                )

                val app = application as AlarmApplication
                app.database.securityDao().let { dao ->
                    SecurityRepository(dao).insertLog(newLog)
                }

                val cloudSyncManager = CloudSyncManager(applicationContext)
                cloudSyncManager.uploadLogToCloud(newLog, photoPath?.let { Uri.parse(it) })
            } catch (_: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
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
}
