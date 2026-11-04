package com.example.myalarmapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining single point of access for Alarm operations.
 */
class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms()
    }

    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }
}
