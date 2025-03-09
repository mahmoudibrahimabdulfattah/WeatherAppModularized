package com.example.weather_library_formatter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Provides weather icons based on weather conditions
 */
object WeatherIcons {

    /**
     * Returns the appropriate weather icon based on weather code
     */
    fun getWeatherIcon(code: Int): ImageVector {
        return when (code) {
            in 200..232 -> Icons.Rounded.Thunderstorm  // Thunderstorm
            in 300..321, in 500..531 -> Icons.Rounded.WaterDrop  // Rain/Drizzle
            in 600..622 -> Icons.Rounded.AcUnit  // Snow
            in 701..781 -> Icons.Rounded.Cloud  // Fog/Mist
            800 -> Icons.Filled.WbSunny  // Clear
            in 801..804 -> Icons.Rounded.WbCloudy  // Cloudy
            else -> Icons.Filled.WbSunny  // Default
        }
    }
}