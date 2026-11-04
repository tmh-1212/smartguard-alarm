package com.example.myalarmapp

import android.app.Application
import com.example.myalarmapp.data.AlarmDatabase
import com.example.myalarmapp.data.AlarmRepository
import com.example.myalarmapp.security.AntiTheftForegroundService
import com.example.myalarmapp.security.SecurityPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AlarmApplication : Application() {

    val database by lazy { AlarmDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao()) }

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDfdgMrAcpZVOOQEltdIQr_Q5FUmvgq8JE")
                    .setApplicationId("1:751781465999:android:b644923cea7e71def185fa")
                    .setProjectId("smartguard-alarm")
                    .setStorageBucket("smartguard-alarm.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }

            val prefs = SecurityPreferences(this)
            if (prefs.isCustomLockscreenEnabled) {
                AntiTheftForegroundService.start(this)
            }
        } catch (e: Exception) {
            // Handle initialization exception if any
        }
    }
}
