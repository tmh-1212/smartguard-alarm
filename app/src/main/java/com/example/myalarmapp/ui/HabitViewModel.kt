package com.example.myalarmapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.data.Habit
import com.example.myalarmapp.data.HabitCompletion
import com.example.myalarmapp.data.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel responsible for managing Habit state, completion tracking, and streak calculation.
 */
class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<HabitUiState> = combine(
        repository.allHabits,
        repository.allCompletions
    ) { habits, completions ->
        val completionsMap = completions.groupBy { it.habitId }
        val todayStr = LocalDate.now().toString()

        val habitsWithStreak = habits.map { habit ->
            val habitCompletions = completionsMap[habit.id] ?: emptyList()
            val isCompletedToday = habitCompletions.any { it.completionDate == todayStr }
            val streak = calculateStreak(habitCompletions, todayStr)
            HabitWithStreak(habit, isCompletedToday, streak)
        }
        HabitUiState(habitsWithStreak = habitsWithStreak, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState(isLoading = true)
    )

    fun saveHabit(habit: Habit) {
        if (habit.title.isBlank()) {
            _errorMessage.value = "Habit title cannot be blank"
            return
        }
        viewModelScope.launch {
            _errorMessage.value = null
            if (habit.id == 0) {
                repository.insertHabit(habit)
            } else {
                repository.updateHabit(habit)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun toggleHabitToday(habitId: Int, isCompletedToday: Boolean) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            if (isCompletedToday) {
                repository.deleteCompletion(habitId, todayStr)
            } else {
                repository.insertCompletion(
                    HabitCompletion(habitId = habitId, completionDate = todayStr)
                )
            }
        }
    }

    fun toggleHabitActiveStatus(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isActive = !habit.isActive)
            repository.updateHabit(updated)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun calculateStreak(completions: List<HabitCompletion>, todayStr: String): Int {
        if (completions.isEmpty()) return 0
        val completedDates = completions.map { it.completionDate }.toSet()
        var currentDate = LocalDate.parse(todayStr)

        // If not completed today, check if completed yesterday to keep streak alive
        if (!completedDates.contains(currentDate.toString())) {
            currentDate = currentDate.minusDays(1)
            if (!completedDates.contains(currentDate.toString())) {
                return 0
            }
        }

        var streak = 0
        while (completedDates.contains(currentDate.toString())) {
            streak++
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }
}

/**
 * Factory for creating HabitViewModel with HabitRepository dependency.
 */
class HabitViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
