package com.example.myalarmapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository class responsible for managing Focus session data operations between app and Room database.
 */
class FocusRepository(private val focusDao: FocusDao) {

    val allSessions: Flow<List<FocusSession>> = focusDao.getAllSessions()
    val totalFocusMinutes: Flow<Int?> = focusDao.getTotalFocusMinutes()

    suspend fun insertSession(session: FocusSession): Long {
        return focusDao.insertSession(session)
    }
}
