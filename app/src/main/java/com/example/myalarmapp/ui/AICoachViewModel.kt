package com.example.myalarmapp.ui

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.data.AIService
import com.example.myalarmapp.data.AIServiceResult
import com.example.myalarmapp.data.ProductivityStatsRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State representing the state of the AI Productivity Coach.
 */
sealed interface AICoachUiState {
    data object Idle : AICoachUiState
    data object Loading : AICoachUiState
    data class Success(val insight: String) : AICoachUiState
    data class Offline(val message: String, val insight: String) : AICoachUiState
    data class Error(val message: String) : AICoachUiState
}

/**
 * ViewModel responsible for managing AICoach states and requesting insights via AIService.
 *
 * Threading Policy & Confinement Contract:
 * All public methods ([fetchInsight] and [resetState]) enforce Main-thread confinement
 * via runtime assertions (`check(Looper.myLooper() == Looper.getMainLooper())`) before
 * accessing state, generation counters, or jobs.
 */
class AICoachViewModel(
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AICoachUiState>(AICoachUiState.Idle)
    val uiState: StateFlow<AICoachUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var requestGeneration = 0L

    /**
     * Requests productivity insights from the AI service.
     * Enforces runtime Main-thread confinement.
     */
    fun fetchInsight(completedTasks: Int, totalTasks: Int, completedHabits: Int, totalHabits: Int) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "AICoachViewModel.fetchInsight must be called on the Android Main (UI) thread."
        }

        val requestId = ++requestGeneration

        // Explicit input validation according to requirements
        if (completedTasks < 0 || totalTasks < 0 || completedHabits < 0 || totalHabits < 0 ||
            completedTasks > totalTasks || completedHabits > totalHabits) {
            fetchJob?.cancel()
            fetchJob = null
            _uiState.update { 
                AICoachUiState.Error("Invalid productivity statistics provided. Counts cannot be negative and completed counts cannot exceed total counts.") 
            }
            return
        }

        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            if (requestId != requestGeneration) {
                return@launch
            }

            _uiState.update { AICoachUiState.Loading }

            try {
                val request = ProductivityStatsRequest(
                    completedTasks = completedTasks,
                    totalTasks = totalTasks,
                    completedHabits = completedHabits,
                    totalHabits = totalHabits
                )
                val result = aiService.getProductivityInsight(request)

                currentCoroutineContext().ensureActive()

                if (requestId != requestGeneration) {
                    return@launch
                }

                when (result) {
                    is AIServiceResult.Success -> {
                        _uiState.update { AICoachUiState.Success(result.data.insightText) }
                    }
                    is AIServiceResult.Offline -> {
                        _uiState.update { AICoachUiState.Offline("AI Coach is currently offline.", result.fallbackInsight) }
                    }
                    is AIServiceResult.Error -> {
                        val error = result.throwable
                        if (error is CancellationException) {
                            throw error
                        }
                        _uiState.update {
                            AICoachUiState.Error(
                                error.localizedMessage ?: "Unknown error occurred"
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == requestGeneration) {
                    _uiState.update {
                        AICoachUiState.Error(
                            e.localizedMessage ?: "Unknown error occurred"
                        )
                    }
                }
            }
        }
    }

    /**
     * Resets the AI coach state to Idle and cancels any active request.
     * Enforces runtime Main-thread confinement.
     */
    fun resetState() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "AICoachViewModel.resetState must be called on the Android Main (UI) thread."
        }

        requestGeneration++
        fetchJob?.cancel()
        fetchJob = null
        _uiState.update { AICoachUiState.Idle }
    }
}

/**
 * Factory for creating AICoachViewModel with AIService dependency.
 */
class AICoachViewModelFactory(
    private val aiService: AIService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AICoachViewModel::class.java)) {
            return AICoachViewModel(aiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
