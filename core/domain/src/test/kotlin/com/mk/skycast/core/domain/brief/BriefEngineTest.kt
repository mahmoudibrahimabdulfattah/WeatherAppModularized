package com.mk.skycast.core.domain.brief

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.CarryItem
import com.mk.skycast.core.model.ClothingLevel
import com.mk.skycast.core.model.Commute
import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.DayChangeKind
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.ExposureKind
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.model.Hazard
import com.mk.skycast.core.model.HourlyAirQuality
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Test

class BriefEngineTest {

    /** Monday 28 Sep 2026, planned in UTC to keep instants readable. */
    private val date = LocalDate.of(2026, 9, 28)
    private val zone = ZoneOffset.UTC

    private fun at(hour: Int, minute: Int = 0, dayOffset: Long = 0): Instant =
        date.plusDays(dayOffset).atTime(hour, minute).toInstant(zone)

    private fun hour(
        time: Instant,
        feelsLike: Double = 25.0,
        chance: Int = 0,
        mm: Double = 0.0,
        gusts: Double = 15.0,
        uv: Double = 2.0,
        visibility: Double = 20_000.0,
        condition: WeatherCondition = WeatherCondition.CLEAR,
    ) = HourlyForecast(
        time = time, condition = condition, isDay = true, temperatureC = feelsLike, apparentTemperatureC = feelsLike,
        relativeHumidity = 40, precipitationProbability = chance, precipitationMm = mm, windSpeedKmh = 10.0,
        windGustsKmh = gusts, uvIndex = uv, visibilityMeters = visibility,
    )

    private fun daily(day: LocalDate, max: Double = 30.0, rain: Int = 0, wind: Double = 20.0) = DailyForecast(
        date = day, condition = WeatherCondition.CLEAR, temperatureMaxC = max, temperatureMinC = max - 10,
        precipitationProbabilityMax = rain, precipitationSumMm = 0.0, sunrise = null, sunset = at(18),
        uvIndexMax = 7.0, windSpeedMaxKmh = wind,
    )

    private fun weather(
        hourCount: Long = 48,
        today: DailyForecast = daily(date.minusDays(1)),
        tomorrow: DailyForecast = daily(date),
        air: List<HourlyAirQuality> = emptyList(),
        hourly: (Instant) -> HourlyForecast = { hour(it) },
    ) = Weather(
        locationId = 1,
        zoneId = zone,
        current = CurrentWeather(
            at(0), WeatherCondition.CLEAR, true, 25.0, 25.0, 40, null, 0.0, 0, 1013.0, 10.0, 0, null, null, null,
        ),
        hourly = (0 until hourCount).map { hourly(at(0).plusSeconds(it * 3600)) },
        daily = listOf(today, tomorrow),
        airQuality = AirQuality(null, null, null, air),
        fetchedAt = at(0, dayOffset = -1),
    )

    private val commuter = Routine(
        isConfigured = true,
        commute = Commute(
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            travelMinutes = 30,
            mode = TravelMode.PUBLIC_TRANSPORT,
        ),
    )

    private fun outing(
        id: String = "o",
        depart: LocalTime = LocalTime.of(19, 0),
        back: LocalTime = LocalTime.of(22, 0),
        setting: OutingSetting = OutingSetting.OUTDOORS,
        mode: TravelMode = TravelMode.WALK,
    ) = Outing(id, OutingKind.OUTING, null, setOf(DayOfWeek.MONDAY), depart, back, mode, setting)

    private fun build(routine: Routine = commuter, override: DayPlanOverride? = null, weather: Weather = weather()) =
        BriefEngine.build(date, 1, routine, override, weather)!!

    @Test
    fun `away day plans a commute out and back`() {
        val windows = ExposurePlanner.plan(commuter, null, date, zone)

        assertThat(windows.map { Triple(it.kind, it.start, it.end) }).containsExactly(
            Triple(ExposureKind.COMMUTE_OUT, at(8), at(8, 30)),
            Triple(ExposureKind.COMMUTE_BACK, at(17), at(17, 30)),
        ).inOrder()
    }

    @Test
    fun `staying home drops the commute but keeps the evening outing`() {
        val routine = commuter.copy(outings = listOf(outing()))
        val windows = ExposurePlanner.plan(routine, DayPlanOverride(date, dayType = DayType.HOME), date, zone)

        assertThat(windows.map { it.kind }).containsExactly(ExposureKind.OUTING)
    }

    @Test
    fun `overrides cancel and add outings, and late returns end the next day`() {
        val routine = commuter.copy(
            week = commuter.week + (DayOfWeek.MONDAY to DayType.HOME),
            outings = listOf(outing("gym")),
        )
        val late = outing("late", depart = LocalTime.of(22, 0), back = LocalTime.of(1, 0))
        val override = DayPlanOverride(date, cancelledOutingIds = setOf("gym"), addedOutings = listOf(late))

        val windows = ExposurePlanner.plan(routine, override, date, zone)

        assertThat(windows.single().outing?.id).isEqualTo("late")
        assertThat(windows.single().end).isEqualTo(at(1, dayOffset = 1))
    }

    @Test
    fun `night shift returns the next day`() {
        val routine = commuter.copy(
            commute = commuter.commute.copy(
                leaveHome = LocalTime.of(21, 0),
                leaveWork = LocalTime.of(6, 0),
                returnsNextDay = true,
            ),
        )

        val back = ExposurePlanner.plan(routine, null, date, zone).last()

        assertThat(back.start).isEqualTo(at(6, dayOffset = 1))
    }

    @Test
    fun `clothing follows feels-like thresholds`() {
        assertThat(BriefEngine.clothingFor(33.0)).isEqualTo(ClothingLevel.VERY_LIGHT)
        assertThat(BriefEngine.clothingFor(26.0)).isEqualTo(ClothingLevel.LIGHT)
        assertThat(BriefEngine.clothingFor(20.0)).isEqualTo(ClothingLevel.LIGHT_LAYER)
        assertThat(BriefEngine.clothingFor(14.0)).isEqualTo(ClothingLevel.JACKET)
        assertThat(BriefEngine.clothingFor(8.0)).isEqualTo(ClothingLevel.WARM_COAT)
        assertThat(BriefEngine.clothingFor(0.0)).isEqualTo(ClothingLevel.HEAVY)
    }

    @Test
    fun `rain on the way home means an umbrella, tied to the return trip`() {
        val weather = weather { if (it == at(17)) hour(it, chance = 70, mm = 1.2) else hour(it) }

        val brief = build(weather = weather)

        assertThat(brief.carry.single { it.item == CarryItem.UMBRELLA }.because).isEqualTo(ExposureKind.COMMUTE_BACK)
        assertThat(brief.hazards.map { it.hazard }).contains(Hazard.RAIN)
    }

    @Test
    fun `two-wheelers get a raincoat instead of an umbrella`() {
        val routine = commuter.copy(commute = commuter.commute.copy(mode = TravelMode.MOTORBIKE))
        val weather = weather { if (it == at(8)) hour(it, chance = 80, mm = 2.0) else hour(it) }

        val carry = build(routine, weather = weather).carry.map { it.item }

        assertThat(carry).contains(CarryItem.RAINCOAT)
        assertThat(carry).doesNotContain(CarryItem.UMBRELLA)
    }

    @Test
    fun `dressed for a cool morning, the warm afternoon makes the layer removable`() {
        val weather = weather { hour(it, feelsLike = if (it < at(12)) 14.0 else 29.0) }

        val brief = build(weather = weather)

        assertThat(brief.clothing).isEqualTo(ClothingLevel.JACKET)
        assertThat(brief.layerForSwing).isTrue()
    }

    @Test
    fun `a much colder evening outing needs an extra layer`() {
        val routine = commuter.copy(outings = listOf(outing()))
        val weather = weather { hour(it, feelsLike = if (it >= at(19)) 13.0 else 27.0) }

        val brief = build(routine, weather = weather)

        assertThat(brief.clothing).isEqualTo(ClothingLevel.LIGHT)
        assertThat(brief.carry.single { it.item == CarryItem.EXTRA_LAYER }.because).isEqualTo(ExposureKind.OUTING)
    }

    @Test
    fun `heat matters for walkers, not for a car commute`() {
        val hot = weather { hour(it, feelsLike = 41.0) }
        val car = commuter.copy(commute = commuter.commute.copy(mode = TravelMode.CAR))

        val walking = build(weather = hot)
        val driving = build(car, weather = hot)

        assertThat(walking.carry.map { it.item }).contains(CarryItem.WATER)
        assertThat(walking.hazards.map { it.hazard }).contains(Hazard.EXTREME_HEAT)
        assertThat(driving.carry.map { it.item }).doesNotContain(CarryItem.WATER)
        assertThat(driving.hazards.map { it.hazard }).doesNotContain(Hazard.EXTREME_HEAT)
    }

    @Test
    fun `dawn fog is flagged even for drivers`() {
        val car = commuter.copy(commute = commuter.commute.copy(mode = TravelMode.CAR))
        val weather = weather { if (it == at(8)) hour(it, visibility = 400.0) else hour(it) }

        val fog = build(car, weather = weather).hazards.single { it.hazard == Hazard.FOG }

        assertThat(fog.window).isEqualTo(ExposureKind.COMMUTE_OUT)
    }

    @Test
    fun `dust during the commute suggests a mask`() {
        val air = listOf(HourlyAirQuality(at(17), usAqi = 120, pm10 = 300.0, dust = 400.0))

        val brief = build(weather = weather(air = air))

        assertThat(brief.hazards.map { it.hazard }).contains(Hazard.DUST)
        assertThat(brief.carry.map { it.item }).contains(CarryItem.MASK)
    }

    @Test
    fun `indoor outings only look at leaving and coming back`() {
        val routine = commuter.copy(
            week = commuter.week + (DayOfWeek.MONDAY to DayType.HOME),
            outings = listOf(outing(setting = OutingSetting.INDOORS)),
        )
        val weather = weather { if (it == at(20)) hour(it, chance = 90, mm = 5.0) else hour(it) }

        val brief = build(routine, weather = weather)

        assertThat(brief.hazards).isEmpty()
        assertThat(brief.carry.map { it.item }).doesNotContain(CarryItem.UMBRELLA)
    }

    @Test
    fun `a big drop from today is reported as colder`() {
        val weather = weather(today = daily(date.minusDays(1), max = 31.0), tomorrow = daily(date, max = 24.0))

        val change = build(weather = weather).change

        assertThat(change?.kind).isEqualTo(DayChangeKind.COLDER)
        assertThat(change?.deltaC).isEqualTo(7.0)
    }

    @Test
    fun `a day with no outings has no clothing advice`() {
        val routine = commuter.copy(week = commuter.week + (DayOfWeek.MONDAY to DayType.OFF))

        val brief = build(routine)

        assertThat(brief.hasOutings).isFalse()
        assertThat(brief.clothing).isNull()
        assertThat(brief.carry).isEmpty()
        assertThat(brief.dayType).isEqualTo(DayType.OFF)
    }

    @Test
    fun `windows beyond the hourly forecast make coverage partial`() {
        val brief = build(weather = weather(hourCount = 12))

        assertThat(brief.coverage).isEqualTo(ForecastCoverage.PARTIAL)
    }

    @Test
    fun `no daily forecast for the date means no brief`() {
        val weather = weather().let { it.copy(daily = it.daily.take(1)) }

        assertThat(BriefEngine.build(date, 1, commuter, null, weather)).isNull()
    }

    @Test
    fun `the brief is about today until noon and tomorrow after`() {
        assertThat(ObserveDailyBriefUseCase.briefDate(at(9), zone)).isEqualTo(date)
        assertThat(ObserveDailyBriefUseCase.briefDate(at(12), zone)).isEqualTo(date.plusDays(1))
        assertThat(ObserveDailyBriefUseCase.briefDate(at(23, 30), zone)).isEqualTo(date.plusDays(1))
    }
}
