package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.model.CarryItem
import com.mk.skycast.core.model.CarrySuggestion
import com.mk.skycast.core.model.ClothingLevel
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.DayChange
import com.mk.skycast.core.model.DayChangeKind
import com.mk.skycast.core.model.DayOutlook
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.ExposureWindow
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.model.Hazard
import com.mk.skycast.core.model.HazardAlert
import com.mk.skycast.core.model.HourlyAirQuality
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.model.WindowOutlook
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Pure rules that turn forecast + routine into a [DailyBrief]. No I/O, no clock:
 * the same inputs always give the same advice, which is what makes it testable
 * and safe to phrase with a language model later.
 */
object BriefEngine {

    /** Returns null when the forecast has no day entry for [date]. */
    fun build(
        date: LocalDate,
        locationId: Long,
        routine: Routine,
        override: DayPlanOverride?,
        weather: Weather,
        comfortOffsetC: Double = 0.0,
    ): DailyBrief? {
        val daily = weather.daily.firstOrNull { it.date == date } ?: return null
        val windows = ExposurePlanner.plan(routine, override, date, weather.zoneId)
        val samples = windows.associateWith { samplesFor(it, weather) }

        val outlooks = windows.mapNotNull { window -> samples.getValue(window).toOutlook(window) }
        val clothingByWindow = outlooks.associate { it.window to clothingFor(it.feelsLikeMinC + comfortOffsetC) }
        val firstLevel = outlooks.firstOrNull()?.let { clothingByWindow.getValue(it.window) }
        val hazardsByWindow = windows.associateWith { hazardsFor(it, samples.getValue(it)) }
        val hazards = hazardsByWindow.values.flatten()
        val layerForSwing = firstLevel != null && clothingByWindow.values.any { it.ordinal <= firstLevel.ordinal - 2 }

        return DailyBrief(
            date = date,
            locationId = locationId,
            dayType = ExposurePlanner.dayType(routine, override, date),
            windows = outlooks,
            clothing = firstLevel,
            layerForSwing = layerForSwing,
            carry = carryFor(outlooks, clothingByWindow, firstLevel, samples, hazardsByWindow),
            hazards = hazards
                .groupBy { it.hazard }
                .map { (_, alerts) -> alerts.minBy { it.at } }
                .sortedWith(compareBy({ it.hazard.ordinal }, { it.at })),
            change = changeFrom(weather.daily.firstOrNull { it.date == date.minusDays(1) }, daily),
            day = DayOutlook(
                daily.condition,
                daily.temperatureMinC,
                daily.temperatureMaxC,
                daily.precipitationProbabilityMax,
                daily.sunset,
            ),
            coverage = if (windows.all {
                    samples.getValue(it).hours.isNotEmpty()
                }
            ) {
                ForecastCoverage.FULL
            } else {
                ForecastCoverage.PARTIAL
            },
            forecastFetchedAt = weather.fetchedAt,
        )
    }

    fun clothingFor(feelsLikeC: Double): ClothingLevel = when {
        feelsLikeC >= 30 -> ClothingLevel.VERY_LIGHT
        feelsLikeC >= 24 -> ClothingLevel.LIGHT
        feelsLikeC >= 18 -> ClothingLevel.LIGHT_LAYER
        feelsLikeC >= 12 -> ClothingLevel.JACKET
        feelsLikeC >= 5 -> ClothingLevel.WARM_COAT
        else -> ClothingLevel.HEAVY
    }

    private class Samples(val hours: List<HourlyForecast>, val air: List<HourlyAirQuality>)

    /**
     * Hours the user is exposed. Outdoor windows use every overlapping hour; indoor
     * outings only the hours of leaving and coming back.
     */
    private fun samplesFor(window: ExposureWindow, weather: Weather): Samples {
        val first = window.start.truncatedTo(ChronoUnit.HOURS)
        val last = window.end.truncatedTo(ChronoUnit.HOURS)
        val times: (Instant) -> Boolean = if (window.outdoors) {
            { it >= first && it < window.end }
        } else {
            { it == first || it == last }
        }
        return Samples(
            hours = weather.hourly.filter { times(it.time) },
            air = weather.airQuality?.hourly.orEmpty().filter { times(it.time) },
        )
    }

    private fun Samples.toOutlook(window: ExposureWindow): WindowOutlook? {
        if (hours.isEmpty()) return null
        val feelsLike = hours.map { it.feelsLikeC }
        return WindowOutlook(
            window = window,
            feelsLikeMinC = feelsLike.min(),
            feelsLikeMaxC = feelsLike.max(),
            condition = hours.maxBy { it.condition.severity() }.condition,
            precipitationChance = hours.mapNotNull { it.precipitationProbability }.maxOrNull(),
            precipitationMm = hours.sumOf { it.precipitationMm },
        )
    }

    private fun hazardsFor(window: ExposureWindow, samples: Samples): List<HazardAlert> {
        val exposed = window.isExposed()
        val twoWheels = window.mode == TravelMode.BICYCLE || window.mode == TravelMode.MOTORBIKE
        val gustLimit = if (twoWheels || window.mode == TravelMode.WALK) GUST_EXPOSED_KMH else GUST_KMH
        val alerts = mutableListOf<HazardAlert>()
        fun add(hazard: Hazard, at: Instant) {
            alerts += HazardAlert(hazard, at, window.kind)
        }
        samples.hours.forEach { hour ->
            when {
                hour.condition.isThunder() -> add(Hazard.THUNDERSTORM, hour.time)
                hour.precipitationMm >= HEAVY_RAIN_MM -> add(Hazard.HEAVY_RAIN, hour.time)
                hour.isRainy() -> add(Hazard.RAIN, hour.time)
            }
            if (hour.condition == WeatherCondition.FOG ||
                (hour.visibilityMeters ?: Double.MAX_VALUE) < FOG_VISIBILITY_M
            ) {
                add(Hazard.FOG, hour.time)
            }
            if ((hour.windGustsKmh ?: hour.windSpeedKmh) >= gustLimit) add(Hazard.STRONG_WIND, hour.time)
            if (exposed && hour.feelsLikeC >= EXTREME_HEAT_C) add(Hazard.EXTREME_HEAT, hour.time)
            if (exposed && hour.isDay && (hour.uvIndex ?: 0.0) >= HIGH_UV) add(Hazard.HIGH_UV, hour.time)
            if (hour.feelsLikeC <= COLD_C) add(Hazard.COLD, hour.time)
        }
        samples.air.forEach { air ->
            when {
                (air.dust ?: 0.0) >= DUST_UG || (air.pm10 ?: 0.0) >= PM10_DUST_UG -> add(Hazard.DUST, air.time)
                (air.usAqi ?: 0) >= POOR_AQI -> add(Hazard.POOR_AIR, air.time)
            }
        }
        return alerts
    }

    private fun carryFor(
        outlooks: List<WindowOutlook>,
        clothing: Map<ExposureWindow, ClothingLevel>,
        firstLevel: ClothingLevel?,
        samples: Map<ExposureWindow, Samples>,
        hazards: Map<ExposureWindow, List<HazardAlert>>,
    ): List<CarrySuggestion> {
        val carry = linkedMapOf<CarryItem, CarrySuggestion>()
        fun add(item: CarryItem, window: ExposureWindow, at: Instant = window.start) {
            carry.putIfAbsent(item, CarrySuggestion(item, window.kind, at))
        }
        val wet = setOf(Hazard.RAIN, Hazard.HEAVY_RAIN, Hazard.THUNDERSTORM)
        outlooks.forEach { outlook ->
            val window = outlook.window
            val windowHazards = hazards.getValue(window)
            windowHazards.firstOrNull { it.hazard in wet }?.let {
                val twoWheels = window.mode == TravelMode.BICYCLE || window.mode == TravelMode.MOTORBIKE
                add(if (twoWheels) CarryItem.RAINCOAT else CarryItem.UMBRELLA, window, it.at)
            }
            // Dressed for the first trip; a much colder later window needs something extra.
            if (firstLevel != null && clothing.getValue(window).ordinal >= firstLevel.ordinal + 2) {
                add(CarryItem.EXTRA_LAYER, window)
            }
            if (window.isExposed()) {
                val hours = samples.getValue(window).hours
                hours.firstOrNull {
                    it.isDay && (it.uvIndex ?: 0.0) >= SUNSCREEN_UV
                }?.let { add(CarryItem.SUNSCREEN, window, it.time) }
                hours.firstOrNull { it.feelsLikeC >= WATER_C }?.let { add(CarryItem.WATER, window, it.time) }
                windowHazards.firstOrNull { it.hazard == Hazard.DUST || it.hazard == Hazard.POOR_AIR }
                    ?.let { add(CarryItem.MASK, window, it.at) }
            }
        }
        return carry.values.toList()
    }

    private fun changeFrom(previous: DailyForecast?, day: DailyForecast): DayChange? {
        previous ?: return null
        val tempDelta = day.temperatureMaxC - previous.temperatureMaxC
        return when {
            abs(tempDelta) >= CHANGE_TEMP_C ->
                DayChange(if (tempDelta > 0) DayChangeKind.WARMER else DayChangeKind.COLDER, abs(tempDelta))

            day.windSpeedMaxKmh - previous.windSpeedMaxKmh >= CHANGE_WIND_KMH -> DayChange(DayChangeKind.WINDIER)

            (day.precipitationProbabilityMax ?: 0) >= CHANGE_RAIN_NOW &&
                (previous.precipitationProbabilityMax ?: 0) < CHANGE_RAIN_BEFORE -> DayChange(DayChangeKind.RAINIER)

            else -> null
        }
    }

    /** Car trips are sheltered except for the walk to and from the car. */
    private fun ExposureWindow.isExposed() = mode != TravelMode.CAR || (outing != null && outdoors)

    private val HourlyForecast.feelsLikeC get() = apparentTemperatureC ?: temperatureC

    private fun HourlyForecast.isRainy() = (precipitationProbability ?: 0) >= RAIN_CHANCE ||
        precipitationMm >= RAIN_MM ||
        condition in setOf(
            WeatherCondition.DRIZZLE,
            WeatherCondition.FREEZING_DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.FREEZING_RAIN,
            WeatherCondition.RAIN_SHOWERS,
        )

    private fun WeatherCondition.isThunder() =
        this == WeatherCondition.THUNDERSTORM || this == WeatherCondition.THUNDERSTORM_HAIL

    /** Higher = more noteworthy, used to pick one condition to describe a window. */
    private fun WeatherCondition.severity(): Int = when (this) {
        WeatherCondition.THUNDERSTORM_HAIL -> 9
        WeatherCondition.THUNDERSTORM -> 8
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS, WeatherCondition.SNOW_GRAINS -> 7
        WeatherCondition.FREEZING_RAIN, WeatherCondition.FREEZING_DRIZZLE -> 6
        WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> 5
        WeatherCondition.DRIZZLE -> 4
        WeatherCondition.FOG -> 3
        WeatherCondition.OVERCAST -> 2
        WeatherCondition.PARTLY_CLOUDY -> 1
        else -> 0
    }

    private const val RAIN_CHANCE = 40
    private const val RAIN_MM = 0.3
    private const val HEAVY_RAIN_MM = 4.0
    private const val FOG_VISIBILITY_M = 1_000.0
    private const val GUST_KMH = 55.0
    private const val GUST_EXPOSED_KMH = 40.0
    private const val EXTREME_HEAT_C = 40.0
    private const val WATER_C = 32.0
    private const val COLD_C = 5.0
    private const val HIGH_UV = 8.0
    private const val SUNSCREEN_UV = 6.0
    private const val DUST_UG = 150.0
    private const val PM10_DUST_UG = 250.0
    private const val POOR_AQI = 151
    private const val CHANGE_TEMP_C = 5.0
    private const val CHANGE_WIND_KMH = 15.0
    private const val CHANGE_RAIN_NOW = 50
    private const val CHANGE_RAIN_BEFORE = 30
}
