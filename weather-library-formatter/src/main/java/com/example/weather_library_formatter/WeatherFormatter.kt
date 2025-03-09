package com.example.weather_library_formatter

import kotlin.math.roundToInt

/**
 * Utility class for formatting weather data
 */
object WeatherFormatter {

    /**
     * Formats temperature from Kelvin to Celsius
     */
    fun formatTemperature(kelvin: Double): String {
        val celsius = kelvin - 273.15
        return "${celsius.roundToInt()}°C"
    }

    /**
     * Formats wind speed with unit
     */
    fun formatWindSpeed(speed: Double): String {
        return "$speed m/s"
    }

    /**
     * Formats humidity with percentage
     */
    fun formatHumidity(humidity: Int): String {
        return "$humidity%"
    }

    /**
     * Formats date string from API format to user-friendly format
     */
    fun formatDate(date: String): String {
        // Simple implementation - would be improved in a real app
        return date.split(" ")[0].replace("-", "/")
    }

    /**
     * Returns appropriate weather condition description based on weather code
     */
    fun getWeatherCondition(code: Int): String {
        return when (code) {
            in 200..232 -> "Thunderstorm"
            in 300..321 -> "Drizzle"
            in 500..531 -> "Rain"
            in 600..622 -> "Snow"
            in 701..781 -> "Fog/Mist"
            800 -> "Clear"
            in 801..804 -> "Cloudy"
            else -> "Unknown"
        }
    }
}