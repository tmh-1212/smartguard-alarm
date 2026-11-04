package com.example.myalarmapp.data

/**
 * Request model containing aggregate productivity statistics for AI coaching insights.
 */
data class ProductivityStatsRequest(
    val completedTasks: Int,
    val totalTasks: Int,
    val completedHabits: Int,
    val totalHabits: Int
)

/**
 * Response model containing the generated AI productivity insight text.
 */
data class ProductivityInsightResponse(
    val insightText: String
)

/**
 * Type-safe result model for AI service operations, explicitly separating success, offline with fallback, and error.
 */
sealed interface AIServiceResult<out T> {
    data class Success<T>(val data: T) : AIServiceResult<T>
    data class Offline(val fallbackInsight: String) : AIServiceResult<Nothing>
    data class Error(val throwable: Throwable) : AIServiceResult<Nothing>
}

/**
 * Abstract interface for AI service operations, designed for future secure backend proxy integration.
 */
interface AIService {
    suspend fun getProductivityInsight(request: ProductivityStatsRequest): AIServiceResult<ProductivityInsightResponse>
}

/**
 * Mock implementation of AIService returning deterministic sample insights in Amharic and English.
 * Supports explicit simulation of offline conditions with a natural local fallback insight.
 */
class MockAIService(
    private val simulateOffline: Boolean = false
) : AIService {
    override suspend fun getProductivityInsight(request: ProductivityStatsRequest): AIServiceResult<ProductivityInsightResponse> {
        return if (simulateOffline) {
            val fallback = "AI Coach is currently offline. Keep up the great work on your tasks and habits! • AI ሰልጣኝ አሁን ከኢንተርኔት ጋር አልተገናኘም። በስራዎችዎ እና ልማዶችዎ ላይ መስራቱን ይቀጥሉ!"
            AIServiceResult.Offline(fallbackInsight = fallback)
        } else {
            val insight = if (request.completedTasks > 0 || request.completedHabits > 0) {
                "Great progress today! You've completed ${request.completedTasks} tasks and ${request.completedHabits} habits. Keep up the momentum! • ዛሬ ጥሩ ሂደት አሳይተዋል! የቀጠሉትን ስራዎች እና ልማዶች አጠናክረው ይቀጥሉ።"
            } else {
                "Ready for a productive day? Complete your first task or habit to get started! • ለምርታማ ቀን ዝጁ ኖት? ለመጀመር የመጀመሪያውን ስራዎትን ወይም ልማድዎን ያጠናቅቁ!"
            }
            AIServiceResult.Success(ProductivityInsightResponse(insightText = insight))
        }
    }
}
