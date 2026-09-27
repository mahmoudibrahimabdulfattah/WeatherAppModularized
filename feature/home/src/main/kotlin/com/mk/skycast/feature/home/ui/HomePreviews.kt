package com.mk.skycast.feature.home.ui

import android.content.res.Configuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.mk.skycast.core.designsystem.theme.SkycastTheme
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.GeoPoint
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.feature.home.HomeState
import com.mk.skycast.feature.home.WeatherPage
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Deterministic fixtures for previews only. */
private object HomePreviewData {
    val now: Instant = Instant.parse("2026-09-27T12:00:00Z")
    private val zone: ZoneId = ZoneId.of("Africa/Cairo")
    private val hourlyTemperatures = listOf(
        28.0, 29.0, 30.0, 29.0, 28.0, 27.0, 26.0, 25.0, 24.0, 24.0, 23.0, 22.0,
        22.0, 21.0, 22.0, 23.0, 24.0, 25.0, 27.0, 28.0, 29.0, 30.0, 30.0, 29.0,
    )

    val weather = Weather(
        locationId = 1,
        zoneId = zone,
        current = CurrentWeather(
            time = now,
            condition = WeatherCondition.PARTLY_CLOUDY,
            isDay = true,
            temperatureC = 28.0,
            apparentTemperatureC = 29.0,
            relativeHumidity = 48,
            dewPointC = 16.0,
            precipitationMm = 0.0,
            cloudCover = 25,
            pressureHpa = 1013.0,
            windSpeedKmh = 14.0,
            windDirectionDegrees = 315,
            windGustsKmh = 23.0,
            uvIndex = 6.0,
            visibilityMeters = 16_000.0,
        ),
        hourly = hourlyTemperatures.mapIndexed { index, temperature ->
            val rainy = index in 5..9
            HourlyForecast(
                time = now.plus(Duration.ofHours(index.toLong())),
                condition = if (rainy) WeatherCondition.RAIN_SHOWERS else WeatherCondition.PARTLY_CLOUDY,
                isDay = index < 5 || index > 16,
                temperatureC = temperature,
                precipitationProbability = if (rainy) 35 else 5,
                precipitationMm = 0.0,
                windSpeedKmh = 14.0,
                uvIndex = 6.0,
            )
        },
        daily = (0..9).map { index ->
            val rainy = index in 3..4
            DailyForecast(
                date = now.atZone(zone).toLocalDate().plusDays(index.toLong()),
                condition = if (rainy) WeatherCondition.RAIN else WeatherCondition.PARTLY_CLOUDY,
                temperatureMaxC = 30.0 - index % 4,
                temperatureMinC = 21.0 - index % 3,
                precipitationProbabilityMax = if (rainy) 65 else 10,
                precipitationSumMm = 0.0,
                sunrise = now.minus(Duration.ofHours(9)).plus(Duration.ofDays(index.toLong())),
                sunset = now.plus(Duration.ofHours(3)).plus(Duration.ofDays(index.toLong())),
                uvIndexMax = 6.0,
                windSpeedMaxKmh = 18.0,
            )
        },
        airQuality = AirQuality(usAqi = 42, pm25 = 8.0, pm10 = 16.0),
        fetchedAt = now.minus(Duration.ofMinutes(3)),
    )

    private val location = SavedLocation(
        id = 1,
        name = "Cairo",
        region = "Cairo Governorate",
        country = "Egypt",
        countryCode = "EG",
        point = GeoPoint(30.04, 31.24),
        timezone = zone.id,
        isDeviceLocation = true,
        sortOrder = 0,
    )

    fun state(weather: Weather = this.weather) = HomeState(
        isLoading = false,
        pages = listOf(WeatherPage(location, weather, weather.hourly, weather.daily, isStale = false)),
        now = now,
    )
}

@Composable
private fun HomePreview(state: HomeState, darkTheme: Boolean = false) {
    SkycastTheme(darkTheme = darkTheme) {
        HomeScreen(state = state, snackbarHostState = remember { SnackbarHostState() }, onIntent = {})
    }
}

@Preview(name = "Home · day", widthDp = 393, heightDp = 852)
@Composable
private fun HomeDayPreview() = HomePreview(HomePreviewData.state())

@Preview(name = "Home · night", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 393, heightDp = 852)
@Composable
private fun HomeNightPreview() {
    val weather = HomePreviewData.weather
    val night = weather.copy(current = weather.current.copy(condition = WeatherCondition.CLEAR, isDay = false))
    HomePreview(HomePreviewData.state(night), darkTheme = true)
}

@Preview(name = "Home · Arabic", locale = "ar", widthDp = 393, heightDp = 852)
@Composable
private fun HomeArabicPreview() = HomePreview(HomePreviewData.state())

@Preview(name = "Home · tablet", widthDp = 1000, heightDp = 800)
@Composable
private fun HomeTabletPreview() = HomePreview(HomePreviewData.state())

@Preview(name = "Home · welcome", widthDp = 393, heightDp = 852)
@Composable
private fun HomeWelcomePreview() = HomePreview(HomeState(isLoading = false))

@Preview(name = "Home · offline", widthDp = 393, heightDp = 852)
@Composable
private fun HomeOfflinePreview() = HomePreview(HomePreviewData.state().copy(isOffline = true))
