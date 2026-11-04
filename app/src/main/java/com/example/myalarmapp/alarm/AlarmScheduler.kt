package com.example.myalarmapp.alarm

import com.example.myalarmapp.data.Alarm

/**
 * Interface defining contract for scheduling, canceling, and snoozing OS-level alarms.
 */
interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
    fun scheduleSnooze(alarmId: Int, alarmLabel: String, snoozeMinutes: Int = 5)
}
