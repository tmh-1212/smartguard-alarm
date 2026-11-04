package com.example.myalarmapp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AIServiceTest {

    @Test
    fun mockAIService_success_returnsInsight() = runTest {
        val service = MockAIService(simulateOffline = false)
        val request = ProductivityStatsRequest(
            completedTasks = 2,
            totalTasks = 3,
            completedHabits = 1,
            totalHabits = 1
        )
        val result = service.getProductivityInsight(request)
        assertTrue(result is AIServiceResult.Success)
        val success = result as AIServiceResult.Success
        assertTrue(success.data.insightText.contains("Great progress today"))
    }

    @Test
    fun mockAIService_offline_returnsOfflineResult() = runTest {
        val service = MockAIService(simulateOffline = true)
        val request = ProductivityStatsRequest(0, 0, 0, 0)
        val result = service.getProductivityInsight(request)
        assertTrue(result is AIServiceResult.Offline)
        val offline = result as AIServiceResult.Offline
        assertTrue(offline.fallbackInsight.contains("offline"))
    }
}
