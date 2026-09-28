package com.mk.skycast.core.domain.ai

import com.mk.skycast.core.model.Weather
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A compact, location-free forecast summary for grounding free answers: the next
 * three days in 3-hour steps plus daily highs/lows and current air quality.
 */
object ForecastContext {
    private const val HOURS = 72L
    private const val STEP_HOURS = 3
    private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val TIME = DateTimeFormatter.ofPattern("EEE HH:00", Locale.ENGLISH)

    fun build(weather: Weather, now: Instant): String = buildString {
        val zone = weather.zoneId
        appendLine("Local now: ${TIME.format(now.atZone(zone))}. Units: °C, km/h, mm, %.")
        val today = now.atZone(zone).toLocalDate()
        weather.daily.filter { !it.date.isBefore(today) }.take(3).forEach { day ->
            appendLine(
                "${DAY.format(day.date)}: high ${day.temperatureMaxC.roundToInt()}, " +
                    "low ${day.temperatureMinC.roundToInt()}, " +
                    "rain chance ${day.precipitationProbabilityMax ?: 0}%, " +
                    "max wind ${day.windSpeedMaxKmh.roundToInt()}, " +
                    "UV max ${day.uvIndexMax?.roundToInt() ?: 0}",
            )
        }
        val from = now.truncatedTo(ChronoUnit.HOURS)
        weather.hourly
            .filter { it.time >= from && it.time < from.plus(HOURS, ChronoUnit.HOURS) }
            .filterIndexed { index, _ -> index % STEP_HOURS == 0 }
            .forEach { hour ->
                appendLine(
                    "${TIME.format(hour.time.atZone(zone))}: ${hour.temperatureC.roundToInt()}, " +
                        "feels ${(hour.apparentTemperatureC ?: hour.temperatureC).roundToInt()}, " +
                        "rain ${hour.precipitationProbability ?: 0}% ${"%.1f".format(
                            Locale.ENGLISH,
                            hour.precipitationMm,
                        )}mm, " +
                        "wind ${hour.windSpeedKmh.roundToInt()} gusts ${hour.windGustsKmh?.roundToInt() ?: 0}, " +
                        "humidity ${hour.relativeHumidity ?: 0}%, UV ${hour.uvIndex?.roundToInt() ?: 0}",
                )
            }
        weather.airQuality?.usAqi?.let { append("Air quality now: US AQI $it") }
    }
}
