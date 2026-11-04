package com.example.myalarmapp.alarm

/**
 * Data model representing weather and traffic conditions.
 */
data class WeatherTrafficConditions(
    val weatherCondition: String, // e.g., "Clear", "Rainy", "Stormy"
    val trafficDelayMinutes: Int,  // e.g., 0, 15, 30
    val advisoryMessage: String
)

/**
 * Repository providing simulated weather and traffic advisory data for smart alarm scheduling.
 */
class WeatherTrafficRepository {
    fun fetchConditions(): WeatherTrafficConditions {
        // Simulated adaptive check (in production, this would call Weather/Traffic APIs)
        return WeatherTrafficConditions(
            weatherCondition = "Rainy",
            trafficDelayMinutes = 15,
            advisoryMessage = "Heavy rain and moderate traffic detected. Consider waking up 15 minutes earlier."
        )
    }
}
