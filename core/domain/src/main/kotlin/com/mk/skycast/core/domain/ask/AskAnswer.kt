package com.mk.skycast.core.domain.ask

import com.mk.skycast.core.model.AirQualityLevel
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.ForecastCoverage
import java.time.Instant
import java.time.LocalDate

enum class AskQuestion {
    WHAT_TO_WEAR,
    BEST_EXERCISE_TIME,
    AVOID_HEAT,
    LAUNDRY,
    RAIN_NEXT_DAYS,
    AIR_QUALITY,
}

enum class ExerciseKind {
    WALK,
    RUN,
    CYCLE,
}

enum class AskReason {
    COMFORTABLE_TEMP,
    NO_RAIN,
    LOW_UV,
    CALM_WIND,
    CLEAN_AIR,
    LOW_HUMIDITY,
    GOOD_DRYING_WIND,
    AVOIDS_PEAK_HEAT,
    COOLER_TIME,
    RAIN_EXPECTED,
    AIR_DATA_UNAVAILABLE,
    LIMITED_FORECAST,
}

data class TimeWindow(val start: Instant, val end: Instant)

sealed interface AskAnswer {
    val forecastFetchedAt: Instant?
    val coverage: ForecastCoverage

    data class RoutineNeeded(
        override val forecastFetchedAt: Instant? = null,
        override val coverage: ForecastCoverage = ForecastCoverage.PARTIAL,
    ) : AskAnswer

    data class Wear(val brief: DailyBrief) : AskAnswer {
        override val forecastFetchedAt: Instant = brief.forecastFetchedAt
        override val coverage: ForecastCoverage = brief.coverage
    }

    data class ExerciseWindow(
        val kind: ExerciseKind,
        val best: TimeWindow?,
        val alternatives: List<TimeWindow>,
        val feelsLikeRangeC: ClosedFloatingPointRange<Double>?,
        val reasons: List<AskReason>,
        override val forecastFetchedAt: Instant,
        override val coverage: ForecastCoverage,
    ) : AskAnswer

    data class AvoidHeat(
        val dates: List<LocalDate>,
        val hottestWindow: TimeWindow,
        val peakFeelsLikeC: Double,
        val coolerWindows: List<TimeWindow>,
        val hasStrongHeat: Boolean,
        override val forecastFetchedAt: Instant,
        override val coverage: ForecastCoverage,
    ) : AskAnswer

    data class Laundry(
        val best: TimeWindow?,
        val reasons: List<AskReason>,
        override val forecastFetchedAt: Instant,
        override val coverage: ForecastCoverage,
    ) : AskAnswer

    data class Rain(
        val days: List<RainDay>,
        override val forecastFetchedAt: Instant,
        override val coverage: ForecastCoverage,
    ) : AskAnswer

    data class Air(
        val usAqi: Int?,
        val level: AirQualityLevel?,
        val pm10: Double?,
        val dust: Double?,
        val at: Instant?,
        val dustPeak: Instant?,
        val unavailable: Boolean,
        override val forecastFetchedAt: Instant,
        override val coverage: ForecastCoverage,
    ) : AskAnswer
}

data class RainDay(val date: LocalDate, val chance: Int?, val mm: Double, val firstRainyHour: Instant?)
