package com.example.myalarmapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.data.FocusRepository
import com.example.myalarmapp.data.FocusSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Session types for the Pomodoro timer.
 */
enum class SessionType(val durationMinutes: Int, val title: String) {
    FOCUS(25, "Focus Time"),
    SHORT_BREAK(5, "Short Break"),
    LONG_BREAK(15, "Long Break")
}

/**
 * UI State for the Pomodoro Timer including focus session history and stats.
 */
data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val timeLeftSeconds: Long = SessionType.FOCUS.durationMinutes * 60L,
    val totalTimeSeconds: Long = SessionType.FOCUS.durationMinutes * 60L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessions: List<FocusSession> = emptyList(),
    val totalFocusMinutes: Int = 0
) {
    val progress: Float
        get() = if (totalTimeSeconds > 0) timeLeftSeconds.toFloat() / totalTimeSeconds.toFloat() else 0f

    val formattedTime: String
        get() {
            val minutes = timeLeftSeconds / 60
            val seconds = timeLeftSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
}

/**
 * ViewModel responsible for managing Pomodoro timer state and countdown logic,
 * with persistence via FocusRepository.
 */
class TimerViewModel(
    private val focusRepository: FocusRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        if (focusRepository != null) {
            viewModelScope.launch {
                focusRepository.allSessions.collect { sessionList ->
                    _uiState.update { it.copy(sessions = sessionList) }
                }
            }
            viewModelScope.launch {
                focusRepository.totalFocusMinutes.collect { totalMinutes ->
                    _uiState.update { it.copy(totalFocusMinutes = totalMinutes ?: 0) }
                }
            }
        }
    }

    fun setSessionType(type: SessionType) {
        stopTimer()
        _uiState.update {
            TimerUiState(
                sessionType = type,
                timeLeftSeconds = type.durationMinutes * 60L,
                totalTimeSeconds = type.durationMinutes * 60L,
                isRunning = false,
                isPaused = false,
                sessions = _uiState.value.sessions,
                totalFocusMinutes = _uiState.value.totalFocusMinutes
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0 && isActive) {
                delay(1000L)
                _uiState.update { state ->
                    if (state.timeLeftSeconds > 1) {
                        state.copy(timeLeftSeconds = state.timeLeftSeconds - 1)
                    } else {
                        stopTimer()
                        // Save completed session to Room database
                        focusRepository?.let { repo ->
                            viewModelScope.launch {
                                repo.insertSession(
                                    FocusSession(
                                        durationMinutes = state.sessionType.durationMinutes,
                                        sessionType = state.sessionType.name,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        state.copy(timeLeftSeconds = 0, isRunning = false, isPaused = false)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        val currentType = _uiState.value.sessionType
        _uiState.update {
            TimerUiState(
                sessionType = currentType,
                timeLeftSeconds = currentType.durationMinutes * 60L,
                totalTimeSeconds = currentType.durationMinutes * 60L,
                isRunning = false,
                isPaused = false,
                sessions = _uiState.value.sessions,
                totalFocusMinutes = _uiState.value.totalFocusMinutes
            )
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        stopTimer()
    }
}

/**
 * Factory for creating TimerViewModel with FocusRepository dependency.
 */
class TimerViewModelFactory(
    private val focusRepository: FocusRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            return TimerViewModel(focusRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
