package com.example.myalarmapp.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Singleton player for playing continuous alarm audio with robust gradual volume increase using AudioAttributes.USAGE_ALARM.
 */
object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var volumeRunnable: Runnable? = null

    fun playAlarmSound(context: Context, soundUriString: String = "default") {
        try {
            stopAlarmSound()

            val alarmUri: Uri = if (soundUriString == "default" || soundUriString.isBlank() || soundUriString.contains("settings")) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(soundUriString)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(0.05f, 0.05f) // Start very quiet
                prepare()
                start()
            }

            // Gradually increase volume over 15 seconds smoothly
            var volume = 0.05f
            volumeRunnable = object : Runnable {
                override fun run() {
                    if (volume < 1.0f && mediaPlayer != null) {
                        volume += 0.08f
                        if (volume > 1.0f) volume = 1.0f
                        try {
                            mediaPlayer?.setVolume(volume, volume)
                        } catch (_: Exception) {}
                        if (volume < 1.0f) {
                            handler.postDelayed(this, 1000L)
                        }
                    }
                }
            }
            handler.postDelayed(volumeRunnable!!, 1000L)

            Log.d("AlarmSoundPlayer", "Playing gradual volume alarm audio from URI: $alarmUri")
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Error playing alarm sound", e)
        }
    }

    fun stopAlarmSound() {
        try {
            volumeRunnable?.let { handler.removeCallbacks(it) }
            volumeRunnable = null

            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                Log.d("AlarmSoundPlayer", "Stopped alarm sound")
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Error stopping alarm sound", e)
        }
    }
}
