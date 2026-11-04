package com.example.myalarmapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository class responsible for managing security log operations between app and Room database.
 */
class SecurityRepository(private val securityDao: SecurityDao) {

    val allLogs: Flow<List<SecurityLog>> = securityDao.getAllLogs()

    suspend fun insertLog(log: SecurityLog): Long {
        return securityDao.insertLog(log)
    }

    suspend fun deleteLog(log: SecurityLog) {
        securityDao.deleteLog(log)
    }
}
