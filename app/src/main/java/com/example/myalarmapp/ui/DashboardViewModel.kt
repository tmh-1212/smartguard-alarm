package com.example.myalarmapp.ui

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.alarm.AlarmSchedulerImpl
import com.example.myalarmapp.data.AIService
import com.example.myalarmapp.data.AIServiceResult
import com.example.myalarmapp.data.AlarmRepository
import com.example.myalarmapp.data.HabitRepository
import com.example.myalarmapp.data.MockAIService
import com.example.myalarmapp.data.ProductivityStatsRequest
import com.example.myalarmapp.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ViewModel responsible for aggregating Dashboard metrics and managing AI Coach state.
 */
class DashboardViewModel(
    alarmRepository: AlarmRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val aiService: AIService = MockAIService()
) : ViewModel() {

    private val _aiCoachUiState = MutableStateFlow<AICoachUiState>(AICoachUiState.Idle)

    private var fetchJob: Job? = null
    private var requestGeneration = 0L

    val uiState: StateFlow<DashboardUiState> = combine(
        alarmRepository.allAlarms,
        taskRepository.allTasks,
        habitRepository.allHabits,
        habitRepository.allCompletions,
        _aiCoachUiState
    ) { alarms, tasks, habits, completions, aiState ->
        val now = LocalTime.now()
        val greeting = when {
            now.hour < 12 -> "Good Morning"
            now.hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }

        val formattedDate = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.getDefault())
        )

        // Task summaries
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.isCompleted }

        // Habit summaries (only active habits)
        val activeHabits = habits.filter { it.isActive }
        val totalActiveHabits = activeHabits.size
        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val todayStr = LocalDate.now().toString()

        val completedHabitsToday = completions.count {
            it.completionDate == todayStr && activeHabitIds.contains(it.habitId)
        }

        // Overall productivity progress calculation (guarded against division by zero)
        val totalTargets = totalTasks + totalActiveHabits
        val totalCompleted = completedTasks + completedHabitsToday
        val productivityProgress = if (totalTargets > 0) {
            totalCompleted.toFloat() / totalTargets.toFloat()
        } else {
            0f
        }

        // Upcoming active alarm calculation
        val enabledAlarms = alarms.filter { it.isEnabled }
        val upcomingAlarm = if (enabledAlarms.isNotEmpty()) {
            enabledAlarms.minByOrNull { alarm ->
                AlarmSchedulerImpl.calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)
            }
        } else {
            null
        }

        DashboardUiState(
            greeting = greeting,
            formattedDate = formattedDate,
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            totalActiveHabits = totalActiveHabits,
            completedHabitsToday = completedHabitsToday,
            productivityProgress = productivityProgress,
            upcomingAlarm = upcomingAlarm,
            isLoading = false,
            aiCoachUiState = aiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    @MainThread
    fun fetchAiCoachInsight() {
        val requestId = ++requestGeneration
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            if (requestId != requestGeneration) return@launch
            _aiCoachUiState.update { AICoachUiState.Loading }

            try {
                val currentState = uiState.value
                val request = ProductivityStatsRequest(
                    completedTasks = currentState.completedTasks,
                    totalTasks = currentState.totalTasks,
                    completedHabits = currentState.completedHabitsToday,
                    totalHabits = currentState.totalActiveHabits
                )

                val result = aiService.getProductivityInsight(request)
                currentCoroutineContext().ensureActive()

                if (requestId != requestGeneration) return@launch

                when (result) {
                    is AIServiceResult.Success -> {
                        _aiCoachUiState.update { AICoachUiState.Success(result.data.insightText) }
                    }
                    is AIServiceResult.Offline -> {
                        _aiCoachUiState.update { AICoachUiState.Offline("AI Coach is currently offline.", result.fallbackInsight) }
                    }
                    is AIServiceResult.Error -> {
                        val error = result.throwable
                        if (error is CancellationException) throw error
                        _aiCoachUiState.update { AICoachUiState.Error(error.localizedMessage ?: "Unknown error occurred") }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == requestGeneration) {
                    _aiCoachUiState.update { AICoachUiState.Error(e.localizedMessage ?: "Unknown error occurred") }
                }
            }
        }
    }

    @MainThread
    fun resetAiCoachState() {
        requestGeneration++
        fetchJob?.cancel()
        fetchJob = null
        _aiCoachUiState.update { AICoachUiState.Idle }
    }
}

/**
 * Factory for creating DashboardViewModel with repository and AI service dependencies.
 */
class DashboardViewModelFactory(
    private val alarmRepository: AlarmRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val aiService: AIService = MockAIService()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(alarmRepository, taskRepository, habitRepository, aiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
