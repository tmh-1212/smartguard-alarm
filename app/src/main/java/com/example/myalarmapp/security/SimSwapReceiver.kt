package com.example.myalarmapp.security

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.myalarmapp.AlarmApplication
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver monitoring SIM card state changes to detect unauthorized SIM swaps
 * and instantly text the owner with the thief's new phone number.
 */
class SimSwapReceiver : BroadcastReceiver() {

    @SuppressLint("HardwareIds", "MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED" || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

            try {
                val currentSimSerial = telephonyManager.simSerialNumber ?: return
                val currentPhoneNumber = try { telephonyManager.line1Number ?: "Unknown" } catch (_: Exception) { "Unknown" }
                val prefs = SecurityPreferences(context)

                val savedSim = prefs.originalSimSerial
                if (savedSim.isBlank()) {
                    prefs.originalSimSerial = currentSimSerial
                    return
                }

                if (savedSim != currentSimSerial) {
                    Log.w("SimSwapReceiver", "⚠️ SIM SWAP DETECTED! Original: $savedSim, Current: $currentSimSerial")

                    CoroutineScope(Dispatchers.IO).launch {
                        val emergencyPhone = prefs.emergencyContact
                        if (emergencyPhone.isNotBlank()) {
                            val alertMessage = "🚨 SmartGuard Anti-Theft Alert: SIM card has been changed! The thief's new phone number/serial is: $currentPhoneNumber ($currentSimSerial)"
                            val smsManager = context.getSystemService(SmsManager::class.java)
                            smsManager.sendTextMessage(emergencyPhone, null, alertMessage, null, null)
                            Log.d("SimSwapReceiver", "SIM swap alert SMS sent to emergency contact: $emergencyPhone")
                        }

                        val app = context.applicationContext as? AlarmApplication
                        app?.database?.securityDao()?.let { dao ->
                            val newLog = SecurityLog(
                                eventType = "SIM_SWAP_DETECTED",
                                details = "Unauthorized SIM card inserted! New number/serial: $currentPhoneNumber ($currentSimSerial)"
                            )
                            SecurityRepository(dao).insertLog(newLog)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SimSwapReceiver", "Error checking SIM state", e)
            }
        }
    }
}
