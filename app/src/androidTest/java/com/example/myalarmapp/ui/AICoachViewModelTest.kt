package com.example.myalarmapp.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myalarmapp.data.AIService
import com.example.myalarmapp.data.AIServiceResult
import com.example.myalarmapp.data.ProductivityInsightResponse
import com.example.myalarmapp.data.ProductivityStatsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AICoachViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAIService(
        private val result: AIServiceResult<ProductivityInsightResponse>
    ) : AIService {
        override suspend fun getProductivityInsight(request: ProductivityStatsRequest): AIServiceResult<ProductivityInsightResponse> {
            return result
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() = runTest(testDispatcher) {
        val fakeService = FakeAIService(AIServiceResult.Success(ProductivityInsightResponse("Test")))
        val viewModel = AICoachViewModel(fakeService)
        assertEquals(AICoachUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun fetchInsight_validInput_producesSuccess() = runTest(testDispatcher) {
        val fakeService = FakeAIService(AIServiceResult.Success(ProductivityInsightResponse("Success Insight")))
        val viewModel = AICoachViewModel(fakeService)

        viewModel.fetchInsight(1, 2, 1, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AICoachUiState.Success)
        assertEquals("Success Insight", (state as AICoachUiState.Success).insight)
    }

    @Test
    fun fetchInsight_negativeCounts_producesError() = runTest(testDispatcher) {
        val fakeService = FakeAIService(AIServiceResult.Success(ProductivityInsightResponse("Test")))
        val viewModel = AICoachViewModel(fakeService)

        viewModel.fetchInsight(-1, 2, 1, 1)

        val state = viewModel.uiState.value
        assertTrue(state is AICoachUiState.Error)
    }

    @Test
    fun resetState_returnsToIdle() = runTest(testDispatcher) {
        val fakeService = FakeAIService(AIServiceResult.Success(ProductivityInsightResponse("Test")))
        val viewModel = AICoachViewModel(fakeService)

        viewModel.fetchInsight(1, 2, 1, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetState()
        assertEquals(AICoachUiState.Idle, viewModel.uiState.value)
    }
}
