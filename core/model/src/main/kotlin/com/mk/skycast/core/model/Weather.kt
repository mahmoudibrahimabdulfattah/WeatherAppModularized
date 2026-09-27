package com.mk.skycast.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Full weather snapshot for a location. All values are stored in metric
 * (°C, km/h, mm, hPa, m) and converted at presentation time.
 */
data class Weather(
    val locationId: Long,
    val zoneId: ZoneId,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val airQuality: AirQuality?,
    val fetchedAt: Instant,
)

data class CurrentWeather(
    val time: Instant,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val relativeHumidity: Int,
    val dewPointC: Double?,
    val precipitationMm: Double,
    val cloudCover: Int,
    val pressureHpa: Double,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val windGustsKmh: Double?,
    val uvIndex: Double?,
    val visibilityMeters: Double?,
)

data class HourlyForecast(
    val time: Instant,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val temperatureC: Double,
    val apparentTemperatureC: Double?,
    val relativeHumidity: Int?,
    val precipitationProbability: Int?,
    val precipitationMm: Double,
    val windSpeedKmh: Double,
    val windGustsKmh: Double?,
    val uvIndex: Double?,
    val visibilityMeters: Double?,
)

data class DailyForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val temperatureMaxC: Double,
    val temperatureMinC: Double,
    val precipitationProbabilityMax: Int?,
    val precipitationSumMm: Double,
    val sunrise: Instant?,
    val sunset: Instant?,
    val uvIndexMax: Double?,
    val windSpeedMaxKmh: Double,
)

data class AirQuality(
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val hourly: List<HourlyAirQuality> = emptyList(),
) {
    val level: AirQualityLevel? get() = usAqi?.let(AirQualityLevel::fromUsAqi)
}

data class HourlyAirQuality(val time: Instant, val usAqi: Int?, val pm10: Double?, val dust: Double?)

enum class AirQualityLevel {
    GOOD,
    MODERATE,
    UNHEALTHY_FOR_SENSITIVE,
    UNHEALTHY,
    VERY_UNHEALTHY,
    HAZARDOUS,
    ;

    companion object {
        fun fromUsAqi(aqi: Int): AirQualityLevel = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> UNHEALTHY_FOR_SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

/** A saved location paired with its (possibly not yet loaded) weather. */
data class LocationWeather(val location: SavedLocation, val weather: Weather?)
