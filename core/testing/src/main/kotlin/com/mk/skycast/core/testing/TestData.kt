package com.mk.skycast.core.testing

import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.GeoPoint
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TestData {
    val NOW: Instant = Instant.parse("2026-09-27T10:15:00Z")

    fun place(id: Long = 1, name: String = "Cairo") = PlaceSuggestion(
        externalId = id,
        name = name,
        region = "Cairo Governorate",
        country = "Egypt",
        countryCode = "EG",
        point = GeoPoint(30.06, 31.25),
        timezone = "Africa/Cairo",
    )

    fun location(id: Long = 1, name: String = "Cairo", sortOrder: Int = 0, isDevice: Boolean = false) = SavedLocation(
        id = id,
        name = name,
        region = null,
        country = "Egypt",
        countryCode = "EG",
        point = GeoPoint(30.06, 31.25),
        timezone = "Africa/Cairo",
        isDeviceLocation = isDevice,
        sortOrder = sortOrder,
    )

    fun weather(locationId: Long = 1, fetchedAt: Instant = NOW, temperatureC: Double = 24.0) = Weather(
        locationId = locationId,
        zoneId = ZoneId.of("Africa/Cairo"),
        current = CurrentWeather(
            time = fetchedAt,
            condition = WeatherCondition.CLEAR,
            isDay = true,
            temperatureC = temperatureC,
            apparentTemperatureC = temperatureC + 1,
            relativeHumidity = 40,
            dewPointC = 10.0,
            precipitationMm = 0.0,
            cloudCover = 5,
            pressureHpa = 1013.0,
            windSpeedKmh = 12.0,
            windDirectionDegrees = 45,
            windGustsKmh = 20.0,
            uvIndex = 6.0,
            visibilityMeters = 10_000.0,
        ),
        hourly = (-2..47).map { offset ->
            HourlyForecast(
                time = NOW.plus(Duration.ofHours(offset.toLong())).truncatedTo(java.time.temporal.ChronoUnit.HOURS),
                condition = WeatherCondition.PARTLY_CLOUDY,
                isDay = true,
                temperatureC = temperatureC + offset % 5,
                apparentTemperatureC = temperatureC + offset % 5 + 1,
                relativeHumidity = 40,
                precipitationProbability = 10,
                precipitationMm = 0.0,
                windSpeedKmh = 10.0,
                windGustsKmh = 16.0,
                uvIndex = 3.0,
                visibilityMeters = 10_000.0,
            )
        },
        daily = (0..9).map { day ->
            DailyForecast(
                date = LocalDate.of(2026, 9, 27).plusDays(day.toLong()),
                condition = WeatherCondition.CLEAR,
                temperatureMaxC = 30.0,
                temperatureMinC = 19.0,
                precipitationProbabilityMax = 5,
                precipitationSumMm = 0.0,
                sunrise = null,
                sunset = null,
                uvIndexMax = 8.0,
                windSpeedMaxKmh = 20.0,
            )
        },
        airQuality = null,
        fetchedAt = fetchedAt,
    )
}
