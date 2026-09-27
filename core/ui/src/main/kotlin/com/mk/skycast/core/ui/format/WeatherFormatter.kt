package com.mk.skycast.core.ui.format

import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UnitConversions
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.WindSpeedUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Locale-aware, unit-aware formatting. Stateless and pure so it can be used from
 * composables, previews and tests alike. Digits follow the given [locale]
 * (e.g. Arabic-Indic digits for "ar").
 */
class WeatherFormatter(
    private val preferences: UserPreferences,
    private val locale: Locale = Locale.getDefault(),
    private val is24HourSystem: Boolean = true,
) {
    private val integer = NumberFormat.getIntegerInstance(locale)
    private val decimal = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }

    /** "23°" */
    fun temperature(celsius: Double): String =
        integer.format(UnitConversions.temperature(celsius, preferences.temperatureUnit).roundToInt()) + "°"

    /** "23°C" */
    fun temperatureWithUnit(celsius: Double): String = temperature(celsius) + temperatureUnitSymbol()

    fun temperatureUnitSymbol(): String = when (preferences.temperatureUnit) {
        TemperatureUnit.CELSIUS -> "C"
        TemperatureUnit.FAHRENHEIT -> "F"
    }

    fun windSpeed(kmh: Double): String = ltr(
        integer.format(UnitConversions.windSpeed(kmh, preferences.windSpeedUnit).roundToInt()) + " " +
            windUnitSymbol(),
    )

    fun windUnitSymbol(): String = when (preferences.windSpeedUnit) {
        WindSpeedUnit.KMH -> "km/h"
        WindSpeedUnit.MPH -> "mph"
        WindSpeedUnit.METERS_PER_SECOND -> "m/s"
        WindSpeedUnit.KNOTS -> "kn"
    }

    fun precipitation(mm: Double): String = ltr(
        when (preferences.precipitationUnit) {
            PrecipitationUnit.MILLIMETERS -> decimal.format(mm) + " mm"

            PrecipitationUnit.INCHES -> NumberFormat.getNumberInstance(locale)
                .apply { maximumFractionDigits = 2 }
                .format(UnitConversions.precipitation(mm, PrecipitationUnit.INCHES)) + " in"
        },
    )

    fun pressure(hpa: Double): String = ltr(
        when (preferences.pressureUnit) {
            PressureUnit.HPA -> integer.format(hpa.roundToInt()) + " hPa"

            PressureUnit.INHG -> NumberFormat.getNumberInstance(locale)
                .apply { maximumFractionDigits = 2 }
                .format(UnitConversions.pressure(hpa, PressureUnit.INHG)) + " inHg"
        },
    )

    fun percent(value: Int): String = integer.format(value) + "%"

    fun visibility(meters: Double): String =
        ltr(if (meters >= 1000) decimal.format(meters / 1000) + " km" else integer.format(meters.roundToInt()) + " m")

    fun uvIndex(value: Double): String = integer.format(value.roundToInt())

    fun number(value: Int): String = integer.format(value)

    /** Wall-clock time in the location's zone, honoring the 12/24h preference. */
    fun time(instant: Instant, zone: ZoneId): String =
        DateTimeFormatter.ofPattern(if (use24Hour()) "HH:mm" else "h:mm a", locale)
            .format(instant.atZone(zone))

    /** A zone-less clock time (e.g. a routine's departure), honoring the 12/24h preference. */
    fun time(time: LocalTime): String =
        DateTimeFormatter.ofPattern(if (use24Hour()) "HH:mm" else "h:mm a", locale).format(time)

    fun uses24HourClock(): Boolean = use24Hour()

    /** Short hour label for hourly strips: "14" / "2 PM". */
    fun hour(instant: Instant, zone: ZoneId): String =
        DateTimeFormatter.ofPattern(if (use24Hour()) "HH:mm" else "h a", locale)
            .format(instant.atZone(zone))

    fun dayOfWeekShort(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    fun dayOfWeekFull(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    /** "Sunday, 27 September" */
    fun fullDate(instant: Instant, zone: ZoneId): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(instant.atZone(zone))

    fun windDirection(degrees: Int): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return directions[((degrees % 360 + 360) % 360 + 22) / 45 % 8]
    }

    /** Keeps "value unit" in reading order inside right-to-left text (e.g. Arabic). */
    private fun ltr(text: String): String = "\u2066" + text + "\u2069"

    private fun use24Hour(): Boolean = when (preferences.timeFormat) {
        TimeFormat.SYSTEM -> is24HourSystem
        TimeFormat.HOUR_12 -> false
        TimeFormat.HOUR_24 -> true
    }
}
