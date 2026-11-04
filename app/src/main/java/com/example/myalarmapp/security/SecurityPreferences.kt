package com.example.myalarmapp.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferences manager for anti-theft settings, security PIN, custom secret SMS commands,
 * Custom Lockscreen Shield preferences, dark mode persistence, SOS emergency contact, SIM serial, Motion Anti-Theft, Remote Record, Spy Call, and Telegram Bot configuration.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var securityPin: String
        get() = prefs.getString(KEY_SECURITY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_SECURITY_PIN, value.trim()).apply()

    var lockscreenPin: String
        get() = prefs.getString(KEY_LOCKSCREEN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_LOCKSCREEN_PIN, value.trim()).apply()

    var lockscreenType: String
        get() = prefs.getString(KEY_LOCKSCREEN_TYPE, "pin") ?: "pin"
        set(value) = prefs.edit().putString(KEY_LOCKSCREEN_TYPE, value).apply()

    var lockscreenPassword: String
        get() = prefs.getString(KEY_LOCKSCREEN_PASSWORD, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_LOCKSCREEN_PASSWORD, value.trim()).apply()

    var isCustomLockscreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKSCREEN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_ENABLED, value).apply()

    var isMotionAntiTheftEnabled: Boolean
        get() = prefs.getBoolean(KEY_MOTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MOTION_ENABLED, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var emergencyContact: String
        get() = prefs.getString(KEY_EMERGENCY_CONTACT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT, value.trim()).apply()

    var originalSimSerial: String
        get() = prefs.getString(KEY_ORIGINAL_SIM, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ORIGINAL_SIM, value.trim()).apply()

    var locateCommand: String
        get() = prefs.getString(KEY_LOCATE_CMD, DEFAULT_LOCATE_CMD) ?: DEFAULT_LOCATE_CMD
        set(value) = prefs.edit().putString(KEY_LOCATE_CMD, value.trim().uppercase()).apply()

    var photoCommand: String
        get() = prefs.getString(KEY_PHOTO_CMD, DEFAULT_PHOTO_CMD) ?: DEFAULT_PHOTO_CMD
        set(value) = prefs.edit().putString(KEY_PHOTO_CMD, value.trim().uppercase()).apply()

    var sirenCommand: String
        get() = prefs.getString(KEY_SIREN_CMD, DEFAULT_SIREN_CMD) ?: DEFAULT_SIREN_CMD
        set(value) = prefs.edit().putString(KEY_SIREN_CMD, value.trim().uppercase()).apply()

    var stopCommand: String
        get() = prefs.getString(KEY_STOP_CMD, DEFAULT_STOP_CMD) ?: DEFAULT_STOP_CMD
        set(value) = prefs.edit().putString(KEY_STOP_CMD, value.trim().uppercase()).apply()

    var recordCommand: String
        get() = prefs.getString(KEY_RECORD_CMD, DEFAULT_RECORD_CMD) ?: DEFAULT_RECORD_CMD
        set(value) = prefs.edit().putString(KEY_RECORD_CMD, value.trim().uppercase()).apply()

    var callCommand: String
        get() = prefs.getString(KEY_CALL_CMD, DEFAULT_CALL_CMD) ?: DEFAULT_CALL_CMD
        set(value) = prefs.edit().putString(KEY_CALL_CMD, value.trim().uppercase()).apply()

    var telegramToken: String
        get() = prefs.getString(KEY_TELEGRAM_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_TOKEN, value.trim()).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value.trim()).apply()

    fun verifyPin(inputPin: String): Boolean {
        return securityPin == inputPin.trim()
    }

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_SECURITY_PIN = "key_security_pin"
        private const val KEY_LOCKSCREEN_PIN = "key_lockscreen_pin"
        private const val KEY_LOCKSCREEN_TYPE = "key_lockscreen_type"
        private const val KEY_LOCKSCREEN_PASSWORD = "key_lockscreen_password"
        private const val KEY_LOCKSCREEN_ENABLED = "key_lockscreen_enabled"
        private const val KEY_MOTION_ENABLED = "key_motion_enabled"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_EMERGENCY_CONTACT = "key_emergency_contact"
        private const val KEY_ORIGINAL_SIM = "key_original_sim"
        private const val KEY_LOCATE_CMD = "key_locate_cmd"
        private const val KEY_PHOTO_CMD = "key_photo_cmd"
        private const val KEY_SIREN_CMD = "key_siren_cmd"
        private const val KEY_STOP_CMD = "key_stop_cmd"
        private const val KEY_RECORD_CMD = "key_record_cmd"
        private const val KEY_CALL_CMD = "key_call_cmd"
        private const val KEY_TELEGRAM_TOKEN = "key_telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "key_telegram_chat_id"

        const val DEFAULT_PIN = "1234"
        const val DEFAULT_LOCATE_CMD = "#LOCATE#"
        const val DEFAULT_PHOTO_CMD = "#GET_PHOTO#"
        const val DEFAULT_SIREN_CMD = "#SIREN#"
        const val DEFAULT_STOP_CMD = "#STOP#"
        const val DEFAULT_RECORD_CMD = "#RECORD#"
        const val DEFAULT_CALL_CMD = "#CALL#"
    }
}
