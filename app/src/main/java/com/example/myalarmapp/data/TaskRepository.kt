package com.example.myalarmapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository class responsible for managing Task data operations between the app and Room database.
 */
class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}
