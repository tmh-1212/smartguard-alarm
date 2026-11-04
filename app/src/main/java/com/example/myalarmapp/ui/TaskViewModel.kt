package com.example.myalarmapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myalarmapp.data.Task
import com.example.myalarmapp.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing Task list state and business logic.
 */
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<TaskUiState> = repository.allTasks
        .map { taskList ->
            TaskUiState(tasks = taskList, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TaskUiState(isLoading = true)
        )

    fun saveTask(task: Task) {
        if (task.title.isBlank()) {
            _errorMessage.value = "Task title cannot be blank"
            return
        }
        viewModelScope.launch {
            _errorMessage.value = null
            if (task.id == 0) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updatedTask)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for creating TaskViewModel with TaskRepository dependency.
 */
class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
