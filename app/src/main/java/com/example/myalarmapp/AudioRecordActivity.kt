package com.example.myalarmapp

import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Foreground activity launched in the background to record 10 seconds of microphone audio
 * using MPEG_4 AAC format for seamless web playback, with robust error logging.
 */
class AudioRecordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            MyAlarmAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        recordAndSyncAudio()
    }

    private fun recordAndSyncAudio() {
        lifecycleScope.launch(Dispatchers.IO) {
            var mediaRecorder: MediaRecorder? = null
            var audioFile: File? = null
            try {
                audioFile = File(cacheDir, "intruder_audio_${System.currentTimeMillis()}.m4a")
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(applicationContext)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }

                Log.d("AudioRecord", "Recording started, waiting 10 seconds...")
                delay(10000L) // Record for 10 seconds

                mediaRecorder.stop()
                mediaRecorder.release()
                mediaRecorder = null

                Log.d("AudioRecord", "Recording finished. File size: ${audioFile.length()} bytes")

                if (audioFile.exists() && audioFile.length() > 0) {
                    val app = application as AlarmApplication
                    app.database.securityDao().let { dao ->
                        val newLog = SecurityLog(
                            eventType = "AUDIO_RECORDED",
                            imagePath = audioFile.absolutePath,
                            details = "Remote 10s audio recording (${audioFile.length()} bytes)"
                        )
                        SecurityRepository(dao).insertLog(newLog)
                        CloudSyncManager(applicationContext).uploadLogToCloud(newLog, Uri.fromFile(audioFile))
                    }
                } else {
                    Log.e("AudioRecord", "Audio file is empty or does not exist")
                }
            } catch (e: Exception) {
                Log.e("AudioRecord", "Error during audio recording or sync", e)
            } finally {
                try {
                    mediaRecorder?.release()
                } catch (_: Exception) {}
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }
}
