package com.mk.skycast.core.domain.ask

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.model.HourlyAirQuality
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.testing.TestData
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

class AskEngineTest {

    private val now = Instant.parse("2026-09-28T06:00:00Z")

    @Test
    fun `wear asks for routine setup when routine is not configured`() {
        val answer = AskEngine.wear(Routine(), null, weather(), now)

        assertThat(answer).isInstanceOf(AskAnswer.RoutineNeeded::class.java)
    }

    @Test
    fun `wear reuses the deterministic daily brief when routine is configured`() {
        val routine = Routine(isConfigured = true, commute = Routine().commute.copy(mode = TravelMode.CAR))
        val answer = AskEngine.wear(routine, null, weather(), now)

        assertThat(answer).isInstanceOf(AskAnswer.Wear::class.java)
        assertThat((answer as AskAnswer.Wear).brief.dayType).isEqualTo(DayType.AWAY)
    }

    @Test
    fun `exercise rejects rain high uv strong gusts and dirty air`() {
        val start = Instant.parse("2026-09-28T05:00:00Z")
        val hours = listOf(
            hour(start, feels = 20.0, rainChance = 0, uv = 2.0, gust = 12.0),
            hour(start.plus(Duration.ofHours(1)), feels = 20.0, rainChance = 40, uv = 2.0, gust = 12.0),
            hour(start.plus(Duration.ofHours(2)), feels = 20.0, rainChance = 0, uv = 8.0, gust = 12.0),
            hour(start.plus(Duration.ofHours(3)), feels = 20.0, rainChance = 0, uv = 2.0, gust = 40.0),
        )
        val weather = weather(hours = hours, air = AirQuality(usAqi = 50, pm25 = null, pm10 = 20.0))

        val answer = AskEngine.exercise(ExerciseKind.WALK, weather, start)

        assertThat(answer.best).isEqualTo(TimeWindow(start, start.plus(Duration.ofHours(1))))
        assertThat(answer.reasons).contains(AskReason.COMFORTABLE_TEMP)
    }

    @Test
    fun `cycle needs a calmer wind threshold`() {
        val start = Instant.parse("2026-09-28T05:00:00Z")
        val weather = weather(hours = listOf(hour(start, feels = 20.0, gust = 30.0)))

        val answer = AskEngine.exercise(ExerciseKind.CYCLE, weather, start)

        assertThat(answer.best).isNull()
    }

    @Test
    fun `avoid heat returns no strong heat with the day max`() {
        val start = Instant.parse("2026-09-28T05:00:00Z")
        val weather =
            weather(hours = listOf(hour(start, feels = 29.0), hour(start.plus(Duration.ofHours(1)), feels = 30.0)))

        val answer = AskEngine.avoidHeat(weather, start)

        assertThat(answer?.hasStrongHeat).isFalse()
        assertThat(answer?.peakFeelsLikeC).isEqualTo(30.0)
    }

    @Test
    fun `laundry requires a dry three hour window plus next two hours`() {
        val start = Instant.parse("2026-09-28T08:00:00Z")
        val weather = weather(
            hours = (0..5).map { offset ->
                hour(start.plus(Duration.ofHours(offset.toLong())), humidity = 50, wind = 18.0, rainChance = 0)
            },
        )

        val answer = AskEngine.laundry(weather, start)

        assertThat(answer.best).isEqualTo(TimeWindow(start, start.plus(Duration.ofHours(6))))
        assertThat(answer.reasons).contains(AskReason.LOW_HUMIDITY)
    }

    @Test
    fun `rain summarizes next three days and first rainy hour`() {
        val start = Instant.parse("2026-09-28T08:00:00Z")
        val rainy = start.plus(Duration.ofHours(2))
        val weather = weather(hours = listOf(hour(start), hour(rainy, rainChance = 60, rainMm = 0.5)))

        val answer = AskEngine.rain(weather, start)

        assertThat(answer.days).hasSize(3)
        assertThat(answer.days.first().firstRainyHour).isEqualTo(rainy)
    }

    @Test
    fun `air is explicitly unavailable when data is missing`() {
        val answer = AskEngine.air(weather(air = null), now)

        assertThat(answer.unavailable).isTrue()
        assertThat(answer.usAqi).isNull()
    }

    @Test
    fun `air includes current aqi and todays dust peak`() {
        val peak = Instant.parse("2026-09-28T11:00:00Z")
        val weather = weather(
            air = AirQuality(
                usAqi = 75,
                pm25 = null,
                pm10 = 35.0,
                hourly = listOf(
                    HourlyAirQuality(now, usAqi = 60, pm10 = 20.0, dust = 10.0),
                    HourlyAirQuality(peak, usAqi = 80, pm10 = 40.0, dust = 120.0),
                ),
            ),
        )

        val answer = AskEngine.air(weather, now)

        assertThat(answer.unavailable).isFalse()
        assertThat(answer.usAqi).isEqualTo(75)
        assertThat(answer.dustPeak).isEqualTo(peak)
    }

    private fun weather(
        hours: List<HourlyForecast> = (0..72).map { hour(now.plus(Duration.ofHours(it.toLong()))) },
        air: AirQuality? = AirQuality(usAqi = 42, pm25 = null, pm10 = 20.0),
    ): Weather {
        val base = TestData.weather(locationId = 1, fetchedAt = now)
        val startDate = now.atZone(base.zoneId).toLocalDate()
        return base.copy(
            hourly = hours,
            daily = (0..3).map { day ->
                daily(base.daily.first(), startDate.plusDays(day.toLong()))
            },
            airQuality = air,
        )
    }

    private fun daily(base: DailyForecast, date: LocalDate) = base.copy(date = date)

    private fun hour(
        time: Instant,
        feels: Double = 22.0,
        humidity: Int = 45,
        wind: Double = 16.0,
        gust: Double = 20.0,
        rainChance: Int = 0,
        rainMm: Double = 0.0,
        uv: Double = 2.0,
    ): HourlyForecast = TestData.weather().hourly.first().copy(
        time = time.truncatedTo(java.time.temporal.ChronoUnit.HOURS),
        temperatureC = feels,
        apparentTemperatureC = feels,
        relativeHumidity = humidity,
        windSpeedKmh = wind,
        windGustsKmh = gust,
        precipitationProbability = rainChance,
        precipitationMm = rainMm,
        uvIndex = uv,
        isDay = time.atZone(TestData.weather().zoneId).toLocalTime() in LocalTime.of(5, 0)..LocalTime.of(19, 0),
    )
}
