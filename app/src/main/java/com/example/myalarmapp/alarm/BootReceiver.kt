package com.example.myalarmapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myalarmapp.AlarmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver listening for device reboot to reschedule active alarms from Room database.
 */
class BootReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Device rebooted. Rescheduling active alarms...")
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    val app = context.applicationContext as? AlarmApplication
                    val repository = app?.repository
                    if (repository != null) {
                        val enabledAlarms = repository.getEnabledAlarms()
                        val scheduler = AlarmSchedulerImpl(context.applicationContext)
                        for (alarm in enabledAlarms) {
                            scheduler.schedule(alarm)
                        }
                        Log.d("BootReceiver", "Successfully rescheduled ${enabledAlarms.size} active alarms after reboot.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
