package com.example.myalarmapp.security

/**
 * Shared state manager for Custom Lockscreen Shield session unlock grace period.
 */
object AppLockState {
    var isUnlockedSession: Boolean = false
    var lastUnlockTimestamp: Long = 0L

    fun unlock() {
        isUnlockedSession = true
        lastUnlockTimestamp = System.currentTimeMillis()
    }

    fun lock() {
        isUnlockedSession = false
    }
}
