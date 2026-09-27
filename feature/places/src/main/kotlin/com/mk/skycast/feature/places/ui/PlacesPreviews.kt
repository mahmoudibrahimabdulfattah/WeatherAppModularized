package com.mk.skycast.feature.places.ui

import android.content.res.Configuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.mk.skycast.core.designsystem.theme.SkycastTheme
import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.GeoPoint
import com.mk.skycast.core.model.LocationWeather
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.feature.places.PlacesState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Deterministic fixtures for previews only. */
private object PlacesPreviewData {
    private val now: Instant = Instant.parse("2026-09-27T12:00:00Z")

    private val cairo = SavedLocation(
        id = 1, name = "Cairo", region = null, country = "Egypt", countryCode = "EG",
        point = GeoPoint(30.04, 31.24), timezone = "Africa/Cairo", isDeviceLocation = true, sortOrder = 0,
    )
    private val london = SavedLocation(
        id = 2, name = "London", region = null, country = "United Kingdom", countryCode = "GB",
        point = GeoPoint(51.5, -0.12), timezone = "Europe/London", isDeviceLocation = false, sortOrder = 1,
    )

    private fun weather(location: SavedLocation, condition: WeatherCondition, temperature: Double): Weather {
        val zone = ZoneId.of(location.timezone)
        return Weather(
            locationId = location.id,
            zoneId = zone,
            current = CurrentWeather(
                time = now, condition = condition, isDay = true, temperatureC = temperature,
                apparentTemperatureC = temperature + 1, relativeHumidity = 48, dewPointC = 16.0,
                precipitationMm = 0.0, cloudCover = 25, pressureHpa = 1013.0, windSpeedKmh = 14.0,
                windDirectionDegrees = 315, windGustsKmh = 23.0, uvIndex = 6.0, visibilityMeters = 16_000.0,
            ),
            hourly = emptyList(),
            daily = listOf(
                DailyForecast(
                    date = now.atZone(zone).toLocalDate(), condition = condition,
                    temperatureMaxC = temperature + 2, temperatureMinC = temperature - 6,
                    precipitationProbabilityMax = 20, precipitationSumMm = 0.0,
                    sunrise = now.minus(Duration.ofHours(6)), sunset = now.plus(Duration.ofHours(6)),
                    uvIndexMax = 6.0, windSpeedMaxKmh = 18.0,
                ),
            ),
            airQuality = null,
            fetchedAt = now,
        )
    }

    val state = PlacesState(
        savedLocations = listOf(
            LocationWeather(cairo, weather(cairo, WeatherCondition.PARTLY_CLOUDY, 28.0)),
            LocationWeather(london, weather(london, WeatherCondition.RAIN, 16.0)),
        ),
        selectedLocationId = 1,
        now = now,
    )

    val searchState = state.copy(
        query = "Ca",
        suggestions = listOf(
            PlaceSuggestion(360630, "Cairo", "Cairo", "Egypt", "EG", GeoPoint(30.06, 31.25), "Africa/Cairo"),
            PlaceSuggestion(4235193, "Cairo", "Illinois", "United States", "US", GeoPoint(37.0, -89.17), null),
        ),
    )
}

@Composable
private fun PlacesPreview(state: PlacesState, darkTheme: Boolean = false) {
    SkycastTheme(darkTheme = darkTheme) {
        PlacesScreen(state = state, snackbarHostState = remember { SnackbarHostState() }, onIntent = {})
    }
}

@Preview(name = "Places · light", widthDp = 393, heightDp = 852)
@Composable
private fun PlacesLightPreview() = PlacesPreview(PlacesPreviewData.state)

@Preview(name = "Places · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 393, heightDp = 852)
@Composable
private fun PlacesDarkPreview() = PlacesPreview(PlacesPreviewData.state, darkTheme = true)

@Preview(name = "Places · Arabic", locale = "ar", widthDp = 393, heightDp = 852)
@Composable
private fun PlacesArabicPreview() = PlacesPreview(PlacesPreviewData.state)

@Preview(name = "Places · search", widthDp = 393, heightDp = 852)
@Composable
private fun PlacesSearchPreview() = PlacesPreview(PlacesPreviewData.searchState)
