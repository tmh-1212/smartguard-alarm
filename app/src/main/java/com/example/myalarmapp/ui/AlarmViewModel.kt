package com.example.myalarmapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.alarm.AlarmScheduler
import com.example.myalarmapp.data.Alarm
import com.example.myalarmapp.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing persistent Alarm state and OS scheduling.
 */
class AlarmViewModel(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val uiState: StateFlow<AlarmUiState> = repository.allAlarms
        .map { alarmList -> AlarmUiState(alarms = alarmList) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmUiState(isLoading = true)
        )

    fun toggleAlarm(alarmId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                repository.updateAlarm(updatedAlarm)
                if (isEnabled) {
                    alarmScheduler.schedule(updatedAlarm)
                } else {
                    alarmScheduler.cancel(updatedAlarm)
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val insertedId = if (alarm.id == 0) {
                repository.insertAlarm(alarm).toInt()
            } else {
                repository.updateAlarm(alarm)
                alarm.id
            }

            val savedAlarm = alarm.copy(id = insertedId)
            if (savedAlarm.isEnabled) {
                alarmScheduler.schedule(savedAlarm)
            } else {
                alarmScheduler.cancel(savedAlarm)
            }
        }
    }
}

class AlarmViewModelFactory(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            return AlarmViewModel(repository, alarmScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
