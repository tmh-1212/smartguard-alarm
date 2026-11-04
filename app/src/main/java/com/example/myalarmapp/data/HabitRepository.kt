package com.example.myalarmapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository class responsible for managing Habit data operations between app and Room database.
 */
class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun getHabitById(id: Int): Habit? {
        return habitDao.getHabitById(id)
    }

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun insertCompletion(completion: HabitCompletion): Long {
        return habitDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(habitId: Int, date: String) {
        habitDao.deleteCompletion(habitId, date)
    }
}
