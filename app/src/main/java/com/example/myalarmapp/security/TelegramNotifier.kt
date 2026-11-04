package com.example.myalarmapp.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Phase 1: Official App Bot Setup & Telegram Notifier configured with verified SmartGuardSecurity_Bot token.
 */
object TelegramNotifier {

    // Verified official Bot Token for SmartGuardSecurity_Bot
    private const val DEFAULT_BOT_TOKEN = "9150239807:AAHoPj7rYhP0rE17VJyehuxkfRM"

    suspend fun sendTelegramMessage(context: Context, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = SecurityPreferences(context)
                val token = prefs.telegramToken.trim().ifBlank { DEFAULT_BOT_TOKEN }
                val chatId = prefs.telegramChatId.trim()

                if (chatId.isBlank() || token.isBlank()) {
                    Log.e("TelegramNotifier", "Token or Chat ID is blank. ChatId: '$chatId'")
                    return@withContext false
                }

                val encodedText = URLEncoder.encode(text, "UTF-8")
                val urlString = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedText"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doInput = true

                val responseCode = connection.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    Log.d("TelegramNotifier", "Telegram message sent successfully!")
                    true
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("TelegramNotifier", "Telegram API Error [$responseCode]: $errorBody")
                    false
                }
            } catch (e: Exception) {
                Log.e("TelegramNotifier", "Exception sending Telegram notification", e)
                false
            }
        }
    }

    suspend fun sendTelegramPhoto(context: Context, caption: String, photoFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = SecurityPreferences(context)
                val token = prefs.telegramToken.trim().ifBlank { DEFAULT_BOT_TOKEN }
                val chatId = prefs.telegramChatId.trim()

                if (chatId.isBlank() || token.isBlank() || !photoFile.exists()) {
                    return@withContext false
                }

                val boundary = "Boundary_" + System.currentTimeMillis()
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val url = URL("https://api.telegram.org/bot$token/sendPhoto")
                val connection = url.openConnection() as HttpsURLConnection
                connection.doInput = true
                connection.doOutput = true
                connection.useCaches = false
                connection.requestMethod = "POST"
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

                val outputStream = DataOutputStream(connection.outputStream)

                // Add chat_id
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"$lineEnd$lineEnd")
                outputStream.writeBytes(chatId + lineEnd)

                // Add caption
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"caption\"$lineEnd$lineEnd")
                outputStream.write(caption.toByteArray(Charsets.UTF_8))
                outputStream.writeBytes(lineEnd)

                // Add photo file
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"${photoFile.name}\"$lineEnd")
                outputStream.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")

                val fileInputStream = FileInputStream(photoFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (bytesRead <= 0) break
                    outputStream.write(buffer, 0, bytesRead)
                }
                fileInputStream.close()

                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                responseCode == HttpsURLConnection.HTTP_OK
            } catch (e: Exception) {
                Log.e("TelegramNotifier", "Error sending Telegram photo", e)
                false
            }
        }
    }
}
