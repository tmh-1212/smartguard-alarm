package com.example.myalarmapp.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.example.myalarmapp.AlarmApplication
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.data.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver listening for incoming SMS messages to execute custom secret security commands
 * instantly and reliably with WakeLock and priority.
 */
class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartGuard::SmsReceiverWakeLock")
            try {
                wakeLock?.acquire(15000L)
            } catch (_: Exception) {}

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val prefs = SecurityPreferences(context)

            val locateCmd = prefs.locateCommand.trim().uppercase()
            val photoCmd = prefs.photoCommand.trim().uppercase()
            val sirenCmd = prefs.sirenCommand.trim().uppercase()
            val stopCmd = prefs.stopCommand.trim().uppercase()
            val recordCmd = prefs.recordCommand.trim().uppercase()
            val callCmd = prefs.callCommand.trim().uppercase()

            val subscriptionId = intent.getIntExtra("subscription", intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1))

            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""

                Log.d("SmsReceiver", "Received SMS from $sender: '$body' (subId: $subscriptionId)")

                val trimmedBody = body.trim().uppercase()
                
                val matchesAny = listOf(locateCmd, photoCmd, sirenCmd, stopCmd, recordCmd, callCmd).any { cmd ->
                    cmd.isNotBlank() && trimmedBody.contains(cmd)
                }

                if (matchesAny) {
                    val pendingResult = goAsync()
                    receiverScope.launch {
                        try {
                            val app = context.applicationContext as? AlarmApplication
                            val securityDao = app?.database?.securityDao()
                            if (securityDao != null) {
                                val securityRepo = SecurityRepository(securityDao)
                                securityRepo.insertLog(
                                    SecurityLog(
                                        eventType = "REMOTE_COMMAND_RECEIVED",
                                        details = "Command: $trimmedBody from $sender"
                                    )
                                )

                                when {
                                    stopCmd.isNotBlank() && trimmedBody.contains(stopCmd) -> {
                                        SecurityActionExecutor.stopSiren(context)
                                    }
                                    locateCmd.isNotBlank() && trimmedBody.contains(locateCmd) -> {
                                        SecurityActionExecutor.executeLocate(context, sender, subscriptionId)
                                    }
                                    photoCmd.isNotBlank() && trimmedBody.contains(photoCmd) -> {
                                        SecurityActionExecutor.executePhotoCapture(context, sender, subscriptionId)
                                    }
                                    sirenCmd.isNotBlank() && trimmedBody.contains(sirenCmd) -> {
                                        SecurityActionExecutor.executeSiren(context)
                                    }
                                    recordCmd.isNotBlank() && trimmedBody.contains(recordCmd) -> {
                                        SecurityActionExecutor.executeAudioRecording(context, sender, subscriptionId)
                                    }
                                    callCmd.isNotBlank() && trimmedBody.contains(callCmd) -> {
                                        SecurityActionExecutor.executeSpyCall(context, sender)
                                    }
                                }

                                Log.d("SmsReceiver", "Successfully executed command: $trimmedBody")
                            }
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Error processing remote SMS command", e)
                        } finally {
                            try {
                                wakeLock?.release()
                            } catch (_: Exception) {}
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
