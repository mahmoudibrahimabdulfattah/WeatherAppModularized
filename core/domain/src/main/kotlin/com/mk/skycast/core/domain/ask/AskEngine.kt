package com.mk.skycast.core.domain.ask

import com.mk.skycast.core.domain.brief.BriefEngine
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.AirQualityLevel
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.model.HourlyAirQuality
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.Weather
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object AskEngine {

    fun wear(
        routine: Routine,
        override: DayPlanOverride?,
        weather: Weather,
        now: Instant,
        comfortOffsetC: Double = 0.0,
    ): AskAnswer {
        if (!routine.isConfigured) return AskAnswer.RoutineNeeded(weather.fetchedAt, ForecastCoverage.PARTIAL)
        val date = ObserveDailyBriefUseCase.briefDate(now, weather.zoneId)
        val brief = BriefEngine.build(date, weather.locationId, routine, override, weather, comfortOffsetC)
            ?: return AskAnswer.RoutineNeeded(weather.fetchedAt, ForecastCoverage.PARTIAL)
        return AskAnswer.Wear(brief)
    }

    fun exercise(
        kind: ExerciseKind,
        weather: Weather,
        now: Instant,
        onDate: LocalDate? = null,
    ): AskAnswer.ExerciseWindow {
        val candidates = weather.searchHours(now, days = 3, onDate)
            .filter { it.localTime(weather.zoneId) in EXERCISE_START..EXERCISE_END }
        val airByHour = weather.airByHour()
        val scored = candidates.mapNotNull { hour ->
            val score = exerciseScore(kind, hour, airByHour[hour.time], weather.zoneId) ?: return@mapNotNull null
            ScoredHour(hour, score)
        }
        val minMinutes = if (kind == ExerciseKind.CYCLE) CYCLE_MIN_MINUTES else EXERCISE_MIN_MINUTES
        val windows = scored.contiguousWindows(weather.zoneId)
            .filter { it.durationMinutes >= minMinutes }
            .map { it.bestSlice(EXERCISE_MAX_HOURS) }
            .sortedWith(compareByDescending<ScoredWindow> { it.averageScore }.thenBy { it.start })
        val best = windows.firstOrNull()
        val reasons = if (best == null) {
            listOf(AskReason.LIMITED_FORECAST)
        } else {
            listOf(
                AskReason.COMFORTABLE_TEMP,
                AskReason.NO_RAIN,
                AskReason.LOW_UV,
                AskReason.CALM_WIND,
            ) + if (best.hasAirData(airByHour)) listOf(AskReason.CLEAN_AIR) else emptyList()
        }
        return AskAnswer.ExerciseWindow(
            kind = kind,
            best = best?.toTimeWindow(),
            alternatives = windows.drop(1).take(MAX_ALTERNATIVES).map { it.toTimeWindow() },
            feelsLikeRangeC = best?.hours?.feelsRange(),
            reasons = reasons,
            forecastFetchedAt = weather.fetchedAt,
            coverage = weather.coverageUntil(now, days = 3),
        )
    }

    fun avoidHeat(weather: Weather, now: Instant, onDate: LocalDate? = null): AskAnswer.AvoidHeat? {
        val hours = weather.searchHours(now, days = if (onDate == null) 2 else 3, onDate)
            .filter { it.localTime(weather.zoneId) in EXERCISE_START..EXERCISE_END }
        val peak = hours.maxByOrNull { it.feelsLikeC } ?: return null
        val hotHours = hours.filter { it.feelsLikeC >= HEAT_C }
        val hotDates = hotHours.map { it.time.atZone(weather.zoneId).toLocalDate() }.distinct()
        val cooler = hours
            .filter { it.feelsLikeC <= COOLER_HEAT_C }
            .map { ScoredHour(it, COOLER_HEAT_C - it.feelsLikeC) }
            .contiguousWindows(weather.zoneId)
            .filter { it.durationMinutes >= EXERCISE_MIN_MINUTES }
            .sortedWith(compareByDescending<ScoredWindow> { it.averageScore }.thenBy { it.start })
            .take(MAX_ALTERNATIVES)
        return AskAnswer.AvoidHeat(
            dates = if (hotDates.isEmpty()) listOf(peak.time.atZone(weather.zoneId).toLocalDate()) else hotDates,
            hottestWindow = TimeWindow(peak.time, peak.time.plus(1, ChronoUnit.HOURS)),
            peakFeelsLikeC = peak.feelsLikeC,
            coolerWindows = cooler.map { it.toTimeWindow() },
            hasStrongHeat = hotHours.isNotEmpty(),
            forecastFetchedAt = weather.fetchedAt,
            coverage = weather.coverageUntil(now, days = 2),
        )
    }

    fun laundry(weather: Weather, now: Instant, onDate: LocalDate? = null): AskAnswer.Laundry {
        val airByHour = weather.airByHour()
        val scored = weather.searchHours(now, days = 3, onDate)
            .filter { it.localTime(weather.zoneId) in LAUNDRY_START..LAUNDRY_END }
            .mapNotNull { hour ->
                val score = laundryScore(hour, airByHour[hour.time]) ?: return@mapNotNull null
                ScoredHour(hour, score)
            }
        val windows = scored.contiguousWindows(weather.zoneId)
            .filter { it.durationMinutes >= LAUNDRY_MIN_MINUTES && noRainAfter(it, weather.hourly) }
            .sortedWith(compareByDescending<ScoredWindow> { it.averageScore }.thenBy { it.start })
        val best = windows.firstOrNull()
        return AskAnswer.Laundry(
            best = best?.toTimeWindow(),
            reasons = if (best == null) {
                listOf(AskReason.LIMITED_FORECAST)
            } else {
                listOf(AskReason.NO_RAIN, AskReason.LOW_HUMIDITY, AskReason.GOOD_DRYING_WIND) +
                    if (best.hasAirData(airByHour)) listOf(AskReason.CLEAN_AIR) else emptyList()
            },
            forecastFetchedAt = weather.fetchedAt,
            coverage = weather.coverageUntil(now, days = 3),
        )
    }

    fun rain(weather: Weather, now: Instant, onDate: LocalDate? = null): AskAnswer.Rain {
        val today = now.atZone(weather.zoneId).toLocalDate()
        val days = (0L..2L).filter { onDate == null || today.plusDays(it) == onDate }.map { offset ->
            val date = today.plusDays(offset)
            val hours = weather.hourly.filter { it.time.atZone(weather.zoneId).toLocalDate() == date }
            val daily = weather.daily.firstOrNull { it.date == date }
            RainDay(
                date = date,
                chance = listOfNotNull(
                    daily?.precipitationProbabilityMax,
                    hours.mapNotNull { it.precipitationProbability }.maxOrNull(),
                ).maxOrNull(),
                mm = daily?.precipitationSumMm ?: hours.sumOf { it.precipitationMm },
                firstRainyHour = hours.firstOrNull { it.isRainy() }?.time,
            )
        }
        return AskAnswer.Rain(days, weather.fetchedAt, weather.coverageUntil(now, days = 3))
    }

    fun air(weather: Weather, now: Instant): AskAnswer.Air {
        val today = now.atZone(weather.zoneId).toLocalDate()
        val hourly = weather.airQuality?.hourly.orEmpty()
        val todayHourly = hourly.filter { it.time.atZone(weather.zoneId).toLocalDate() == today }
        val current = todayHourly.lastOrNull { !it.time.isAfter(now) } ?: todayHourly.firstOrNull()
        val dustPeak = todayHourly.maxByOrNull { maxOf(it.dust ?: 0.0, it.pm10 ?: 0.0) }
        val air = weather.airQuality
        val currentAqi = air?.usAqi ?: current?.usAqi
        val unavailable = air == null || currentAqi == null
        return AskAnswer.Air(
            usAqi = currentAqi,
            level = currentAqi?.let(AirQualityLevel::fromUsAqi),
            pm10 = air?.pm10 ?: current?.pm10,
            dust = current?.dust,
            at = current?.time ?: weather.current.time,
            dustPeak = dustPeak?.time,
            unavailable = unavailable,
            forecastFetchedAt = weather.fetchedAt,
            coverage = if (todayHourly.isEmpty()) ForecastCoverage.PARTIAL else ForecastCoverage.FULL,
        )
    }

    private fun exerciseScore(
        kind: ExerciseKind,
        hour: HourlyForecast,
        air: HourlyAirQuality?,
        zone: ZoneId,
    ): Double? {
        val comfort = when (kind) {
            ExerciseKind.WALK -> WALK_COMFORT
            ExerciseKind.RUN -> RUN_COMFORT
            ExerciseKind.CYCLE -> CYCLE_COMFORT
        }
        if (hour.feelsLikeC !in comfort) return null
        if (hour.isRainy()) return null
        if (!hour.isLowUv(zone)) return null
        val gustLimit = if (kind == ExerciseKind.CYCLE) CYCLE_GUST_KMH else EXERCISE_GUST_KMH
        if ((hour.windGustsKmh ?: hour.windSpeedKmh) >= gustLimit) return null
        if (!air.isCleanEnough()) return null
        val comfortMidpoint = (comfort.start + comfort.endInclusive) / 2.0
        return 100.0 -
            kotlin.math.abs(hour.feelsLikeC - comfortMidpoint) * 3.0 -
            (hour.uvIndex ?: 0.0) -
            (hour.windGustsKmh ?: hour.windSpeedKmh) / 5.0
    }

    private fun laundryScore(hour: HourlyForecast, air: HourlyAirQuality?): Double? {
        if (hour.isRainy()) return null
        val humidity = hour.relativeHumidity ?: return null
        val wind = hour.windSpeedKmh
        if (wind !in LAUNDRY_WIND) return null
        if (!air.isCleanEnough()) return null
        return 100.0 -
            maxOf(humidity - LAUNDRY_HUMIDITY, 0) * 1.5 -
            kotlin.math.abs(wind - LAUNDRY_WIND_IDEAL) * 1.2
    }

    private fun noRainAfter(window: ScoredWindow, hours: List<HourlyForecast>): Boolean {
        val through = window.end.plus(2, ChronoUnit.HOURS)
        return hours.filter { it.time >= window.start && it.time < through }.none { it.isRainy() }
    }

    private fun List<ScoredHour>.contiguousWindows(zone: ZoneId): List<ScoredWindow> {
        if (isEmpty()) return emptyList()
        val sorted = sortedBy { it.hour.time }
        val groups = mutableListOf<MutableList<ScoredHour>>()
        sorted.forEach { item ->
            val current = groups.lastOrNull()
            val expected = current?.lastOrNull()?.hour?.time?.plus(1, ChronoUnit.HOURS)
            if (current == null || item.hour.time != expected || item.hour.time.atZone(zone).toLocalDate() !=
                current.last().hour.time.atZone(zone).toLocalDate()
            ) {
                groups += mutableListOf(item)
            } else {
                current += item
            }
        }
        return groups.map { group ->
            ScoredWindow(
                start = group.first().hour.time,
                end = group.last().hour.time.plus(1, ChronoUnit.HOURS),
                hours = group.map { it.hour },
                averageScore = group.map { it.score }.average(),
                scores = group.map { it.score },
            )
        }
    }

    /** Upcoming hours over [days] days, or only those on [onDate] when the question named a day. */
    private fun Weather.searchHours(now: Instant, days: Long, onDate: LocalDate? = null): List<HourlyForecast> {
        val start = now.truncatedTo(ChronoUnit.HOURS)
        val endDate = now.atZone(zoneId).toLocalDate().plusDays(days)
        return hourly.filter { hour ->
            val date = hour.time.atZone(zoneId).toLocalDate()
            !hour.time.isBefore(start) && date.isBefore(endDate) && (onDate == null || date == onDate)
        }
    }

    private fun Weather.airByHour(): Map<Instant, HourlyAirQuality> =
        airQuality?.hourly.orEmpty().associateBy { it.time.truncatedTo(ChronoUnit.HOURS) }

    private fun Weather.coverageUntil(now: Instant, days: Long): ForecastCoverage {
        val end = now.atZone(zoneId).toLocalDate().plusDays(days).atStartOfDay(zoneId).toInstant()
        val lastHour = hourly.maxOfOrNull { it.time } ?: return ForecastCoverage.PARTIAL
        return if (lastHour >= end.minus(1, ChronoUnit.HOURS)) ForecastCoverage.FULL else ForecastCoverage.PARTIAL
    }

    private fun HourlyForecast.localTime(zone: ZoneId): LocalTime = time.atZone(zone).toLocalTime()

    private val HourlyForecast.feelsLikeC get() = apparentTemperatureC ?: temperatureC

    private fun HourlyForecast.isRainy() = (precipitationProbability ?: 0) >= RAIN_CHANCE ||
        precipitationMm >= RAIN_MM

    private fun HourlyForecast.isLowUv(zone: ZoneId): Boolean {
        val local = time.atZone(zone).toLocalTime()
        return (uvIndex ?: 0.0) < UV_LIMIT || local < UV_FREE_BEFORE || local >= UV_FREE_AFTER
    }

    private fun HourlyAirQuality?.isCleanEnough(): Boolean {
        this ?: return true
        return (usAqi ?: 0) < POOR_AQI && (pm10 ?: 0.0) < PM10_DUST_UG && (dust ?: 0.0) < DUST_UG
    }

    private fun ScoredWindow.hasAirData(airByHour: Map<Instant, HourlyAirQuality>): Boolean = hours.any { hour ->
        airByHour[hour.time]?.let { it.usAqi != null || it.pm10 != null || it.dust != null } == true
    }

    private fun List<HourlyForecast>.feelsRange(): ClosedFloatingPointRange<Double> {
        val feels = map { it.feelsLikeC }
        return feels.min()..feels.max()
    }

    /** A long comfortable span is narrowed to its best-scoring [hours]-hour slice. */
    private fun ScoredWindow.bestSlice(hours: Int): ScoredWindow {
        if (scores.size <= hours) return this
        val start = (0..scores.size - hours).maxBy { i -> scores.subList(i, i + hours).sum() }
        val sliceHours = this.hours.subList(start, start + hours)
        return ScoredWindow(
            start = sliceHours.first().time,
            end = sliceHours.last().time.plus(1, ChronoUnit.HOURS),
            hours = sliceHours,
            averageScore = scores.subList(start, start + hours).average(),
            scores = scores.subList(start, start + hours),
        )
    }

    private val ScoredWindow.durationMinutes: Long get() = ChronoUnit.MINUTES.between(start, end)

    private fun ScoredWindow.toTimeWindow() = TimeWindow(start, end)

    private data class ScoredHour(val hour: HourlyForecast, val score: Double)

    private data class ScoredWindow(
        val start: Instant,
        val end: Instant,
        val hours: List<HourlyForecast>,
        val averageScore: Double,
        val scores: List<Double>,
    )

    private val EXERCISE_START: LocalTime = LocalTime.of(5, 0)
    private val EXERCISE_END: LocalTime = LocalTime.of(21, 0)
    private val LAUNDRY_START: LocalTime = LocalTime.of(8, 0)
    private val LAUNDRY_END: LocalTime = LocalTime.of(18, 0)
    private val UV_FREE_BEFORE: LocalTime = LocalTime.of(9, 0)
    private val UV_FREE_AFTER: LocalTime = LocalTime.of(17, 0)
    private val WALK_COMFORT = 12.0..28.0
    private val RUN_COMFORT = 8.0..24.0
    private val CYCLE_COMFORT = 10.0..26.0
    private val LAUNDRY_WIND = 8.0..30.0
    private const val RAIN_CHANCE = 30
    private const val RAIN_MM = 0.2
    private const val UV_LIMIT = 6.0
    private const val EXERCISE_GUST_KMH = 35.0
    private const val CYCLE_GUST_KMH = 25.0
    private const val POOR_AQI = 101
    private const val DUST_UG = 100.0
    private const val PM10_DUST_UG = 150.0
    private const val HEAT_C = 32.0
    private const val COOLER_HEAT_C = 30.0
    private const val LAUNDRY_HUMIDITY = 60
    private const val LAUNDRY_WIND_IDEAL = 18.0
    private const val EXERCISE_MIN_MINUTES = 45
    private const val CYCLE_MIN_MINUTES = 60
    private const val LAUNDRY_MIN_MINUTES = 180
    private const val MAX_ALTERNATIVES = 2
    private const val EXERCISE_MAX_HOURS = 2
}
