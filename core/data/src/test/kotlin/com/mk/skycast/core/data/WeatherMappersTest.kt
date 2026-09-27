package com.mk.skycast.core.data

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.data.mapper.toCurrentEntity
import com.mk.skycast.core.data.mapper.toDailyEntities
import com.mk.skycast.core.data.mapper.toDomain
import com.mk.skycast.core.data.mapper.toHourlyEntities
import com.mk.skycast.core.database.entity.PopulatedWeather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.network.model.NetworkAirQuality
import com.mk.skycast.core.network.model.NetworkAirQualityCurrent
import com.mk.skycast.core.network.model.NetworkAirQualityHourly
import com.mk.skycast.core.network.model.NetworkCurrent
import com.mk.skycast.core.network.model.NetworkDaily
import com.mk.skycast.core.network.model.NetworkForecast
import com.mk.skycast.core.network.model.NetworkHourly
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class WeatherMappersTest {

    // 2026-09-27T00:00 in Cairo (UTC+3) == 2026-09-26T21:00Z
    private val cairoMidnight = 1_790_456_400L

    private val forecast = NetworkForecast(
        latitude = 30.0,
        longitude = 31.0,
        timezone = "Africa/Cairo",
        utcOffsetSeconds = 10_800,
        current = NetworkCurrent(
            time = cairoMidnight + 36_000,
            temperature = 27.4,
            relativeHumidity = 40,
            apparentTemperature = 28.0,
            isDay = 1,
            precipitation = 0.0,
            weatherCode = 2,
            cloudCover = 30,
            pressureMsl = 1012.0,
            windSpeed = 14.0,
            windDirection = 320,
        ),
        hourly = NetworkHourly(
            time = listOf(cairoMidnight + 7_200, cairoMidnight, cairoMidnight + 3_600),
            temperature = listOf(20.0, 19.0, null),
            apparentTemperature = listOf(21.5, 20.5, 19.5),
            relativeHumidity = listOf(51, 52, 53),
            weatherCode = listOf(0, 1, 2),
            isDay = listOf(0, 0, 0),
            precipitationProbability = listOf(0, 5, null),
            precipitation = listOf(0.0, null, 0.0),
            windSpeed = listOf(5.0, 6.0, 7.0),
            windGusts = listOf(11.0, 12.0, 13.0),
            uvIndex = listOf(null, null, null),
            visibility = listOf(10_000.0, 9_000.0, 8_000.0),
        ),
        daily = NetworkDaily(
            time = listOf(cairoMidnight),
            weatherCode = listOf(61),
            temperatureMax = listOf(31.0),
            temperatureMin = listOf(19.0),
            precipitationProbabilityMax = listOf(40),
            precipitationSum = listOf(1.2),
            sunrise = listOf(cairoMidnight + 20_000),
            sunset = listOf(cairoMidnight + 63_000),
            uvIndexMax = listOf(7.5),
            windSpeedMax = listOf(20.0),
        ),
    )

    @Test
    fun `network forecast round-trips to a sorted domain model`() {
        val fetchedAt = Instant.parse("2026-09-27T08:00:00Z")
        val populated = PopulatedWeather(
            current = forecast.toCurrentEntity(7, fetchedAt, NetworkAirQuality(NetworkAirQualityCurrent(usAqi = 88))),
            hourly = forecast.toHourlyEntities(7),
            daily = forecast.toDailyEntities(7),
            hourlyAirQuality = emptyList(),
        )

        val weather = populated.toDomain()

        assertThat(weather.locationId).isEqualTo(7)
        assertThat(weather.zoneId).isEqualTo(ZoneId.of("Africa/Cairo"))
        assertThat(weather.fetchedAt).isEqualTo(fetchedAt)
        assertThat(weather.current.condition).isEqualTo(WeatherCondition.PARTLY_CLOUDY)
        assertThat(weather.current.isDay).isTrue()
        // Null temperature row dropped, remaining rows sorted by time.
        assertThat(weather.hourly.map { it.temperatureC }).containsExactly(19.0, 20.0).inOrder()
        assertThat(weather.hourly.first().precipitationMm).isEqualTo(0.0)
        assertThat(weather.hourly.first().apparentTemperatureC).isEqualTo(20.5)
        assertThat(weather.hourly.first().relativeHumidity).isEqualTo(52)
        assertThat(weather.hourly.first().windGustsKmh).isEqualTo(12.0)
        assertThat(weather.hourly.first().visibilityMeters).isEqualTo(9_000.0)
        // Daily date resolved in the location's own offset, not UTC.
        assertThat(weather.daily.single().date).isEqualTo(LocalDate.of(2026, 9, 27))
        assertThat(weather.daily.single().condition).isEqualTo(WeatherCondition.RAIN)
        assertThat(weather.airQuality?.usAqi).isEqualTo(88)
    }

    @Test
    fun `air quality is null when no values are present`() {
        val populated = PopulatedWeather(
            current = forecast.toCurrentEntity(1, Instant.EPOCH, null),
            hourly = emptyList(),
            daily = emptyList(),
            hourlyAirQuality = emptyList(),
        )
        assertThat(populated.toDomain().airQuality).isNull()
    }

    @Test
    fun `hourly air quality maps nulls and length mismatches safely`() {
        val air = NetworkAirQuality(
            current = NetworkAirQualityCurrent(usAqi = null, pm25 = null, pm10 = null),
            hourly = NetworkAirQualityHourly(
                time = listOf(cairoMidnight + 3_600, cairoMidnight),
                usAqi = listOf(55),
                pm10 = listOf(null, 32.0),
                dust = emptyList(),
            ),
        )
        val populated = PopulatedWeather(
            current = forecast.toCurrentEntity(7, Instant.EPOCH, air),
            hourly = forecast.toHourlyEntities(7),
            daily = emptyList(),
            hourlyAirQuality = air.toHourlyEntities(7),
        )

        val hourly = populated.toDomain().airQuality?.hourly

        assertThat(hourly).hasSize(2)
        assertThat(hourly?.map { it.time }).containsExactly(
            Instant.ofEpochSecond(cairoMidnight),
            Instant.ofEpochSecond(cairoMidnight + 3_600),
        ).inOrder()
        assertThat(hourly?.first()?.usAqi).isNull()
        assertThat(hourly?.first()?.pm10).isEqualTo(32.0)
        assertThat(hourly?.first()?.dust).isNull()
        assertThat(hourly?.last()?.usAqi).isEqualTo(55)
    }
}
