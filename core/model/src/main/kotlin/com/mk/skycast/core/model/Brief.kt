package com.mk.skycast.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * A deterministic "be prepared" brief for one day of the user's routine.
 * Every number and every piece of advice here is computed by rules — any
 * natural-language phrasing is layered on top and can never change it.
 */
data class DailyBrief(
    val date: LocalDate,
    val locationId: Long,
    val dayType: DayType,
    val windows: List<WindowOutlook>,
    /** What to wear for the coldest time the user is out; null on days with no outings. */
    val clothing: ClothingLevel?,
    /** True when the day swings enough that the layer should be removable. */
    val layerForSwing: Boolean,
    val carry: List<CarrySuggestion>,
    val hazards: List<HazardAlert>,
    /** Road-specific cautions for trips made by car or motorbike. */
    val driving: List<DrivingAlert> = emptyList(),
    val change: DayChange?,
    val day: DayOutlook,
    val coverage: ForecastCoverage,
    val forecastFetchedAt: Instant,
    /** Personal adjustment learned from feedback; negative = the user feels the cold more. */
    val comfortOffsetC: Double = 0.0,
) {
    val hasOutings: Boolean get() = windows.isNotEmpty()
}

enum class ExposureKind { COMMUTE_OUT, COMMUTE_BACK, OUTING }

/** A span of time the user is (at least partly) outside, derived from the routine. */
data class ExposureWindow(
    val kind: ExposureKind,
    val start: Instant,
    val end: Instant,
    val mode: TravelMode,
    /** Indoor outings only expose the user while getting there and back. */
    val outdoors: Boolean,
    val outing: Outing? = null,
)

/** Forecast summary for one exposure window. */
data class WindowOutlook(
    val window: ExposureWindow,
    val feelsLikeMinC: Double,
    val feelsLikeMaxC: Double,
    val condition: WeatherCondition,
    val precipitationChance: Int?,
    val precipitationMm: Double,
)

enum class ClothingLevel {
    /** ≥ 30° feels-like: breathable, loose, light colors. */
    VERY_LIGHT,

    /** 24–30°: short sleeves. */
    LIGHT,

    /** 18–24°: long sleeves or a light layer. */
    LIGHT_LAYER,

    /** 12–18°: a jacket or sweater. */
    JACKET,

    /** 5–12°: a warm coat. */
    WARM_COAT,

    /** < 5°: heavy coat, hat, gloves. */
    HEAVY,
}

enum class CarryItem { UMBRELLA, RAINCOAT, EXTRA_LAYER, SUNSCREEN, WATER, MASK }

/** An item to carry and the window that makes it necessary (the earliest one). */
data class CarrySuggestion(val item: CarryItem, val because: ExposureKind, val at: Instant)

enum class Hazard {
    THUNDERSTORM,
    HEAVY_RAIN,
    RAIN,
    FOG,
    STRONG_WIND,
    DUST,
    POOR_AIR,
    EXTREME_HEAT,
    HIGH_UV,
    COLD,
}

data class HazardAlert(val hazard: Hazard, val at: Instant, val window: ExposureKind?)

/** Road conditions worth a driver's attention; declared most to least serious. */
enum class DrivingRisk {
    /** Heavy rain: streets, tunnels and underpasses can flood. */
    FLOODED_STREETS,

    /** Fog or mist (visibility under 1 km). */
    LOW_VISIBILITY,

    /** Blowing dust that cuts visibility. */
    DUST_VISIBILITY,

    /** Wet roads; worst with the first rain after dry days, when oil comes up. */
    SLIPPERY_ROAD,

    /** Gusts strong enough to push a car or motorbike sideways. */
    CROSSWIND,

    /** Driving within an hour of sunrise or sunset under clear skies. */
    SUN_GLARE,
}

data class DrivingAlert(
    val risk: DrivingRisk,
    val at: Instant,
    val window: ExposureKind,
    /** For [DrivingRisk.SLIPPERY_ROAD]: the previous day was dry, so roads are extra slick. */
    val firstRain: Boolean = false,
)

enum class DayChangeKind { WARMER, COLDER, WINDIER, RAINIER }

/** A noticeable change versus the previous day (people feel deltas, not absolutes). */
data class DayChange(val kind: DayChangeKind, val deltaC: Double? = null)

data class DayOutlook(
    val condition: WeatherCondition,
    val minC: Double,
    val maxC: Double,
    val precipitationChance: Int?,
    val sunset: Instant?,
)

enum class ForecastCoverage {
    /** Hourly data covers every exposure window. */
    FULL,

    /** Some windows fall outside the stored hourly forecast. */
    PARTIAL,
}

/**
 * What a notified brief told the user to act on. Two briefs with the same
 * fingerprint need no new notification — temperature drift alone stays silent.
 */
data class BriefFingerprint(val date: LocalDate, val keys: Set<String>)
