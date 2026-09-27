package com.mk.skycast.core.ui.ask

import android.content.res.Resources
import androidx.annotation.StringRes
import com.mk.skycast.core.domain.ask.AskAnswer
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.domain.ask.AskReason
import com.mk.skycast.core.domain.ask.ExerciseKind
import com.mk.skycast.core.domain.ask.TimeWindow
import com.mk.skycast.core.model.AirQualityLevel
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.ui.R
import com.mk.skycast.core.ui.brief.BriefText
import com.mk.skycast.core.ui.format.WeatherFormatter
import java.time.Instant
import java.time.ZoneId

class AskText(private val resources: Resources, private val formatter: WeatherFormatter, private val zone: ZoneId) {

    fun questionLabel(question: AskQuestion): String = resources.getString(question.labelRes())

    fun exerciseLabel(kind: ExerciseKind): String = resources.getString(kind.labelRes())

    fun dateRange(now: Instant): String {
        val start = now.atZone(zone).toLocalDate()
        val end = start.plusDays(2)
        return resources.getString(
            R.string.ask_date_range,
            formatter.dayOfWeekShort(start),
            formatter.dayOfWeekShort(end),
        )
    }

    fun display(answer: AskAnswer, now: Instant): AskDisplay = AskDisplay(
        headline = headline(answer, now),
        chips = chips(answer, now),
        reasons = reasons(answer, now),
        freshness = freshness(answer),
    )

    private fun headline(answer: AskAnswer, now: Instant): String = when (answer) {
        is AskAnswer.RoutineNeeded -> resources.getString(R.string.ask_wear_setup_headline)

        is AskAnswer.Wear -> BriefText(resources, formatter, zone).headline(answer.brief, now)

        is AskAnswer.ExerciseWindow -> answer.best?.let {
            resources.getString(
                when (answer.kind) {
                    ExerciseKind.WALK -> R.string.ask_exercise_headline_walk
                    ExerciseKind.RUN -> R.string.ask_exercise_headline_run
                    ExerciseKind.CYCLE -> R.string.ask_exercise_headline_cycle
                },
                window(it, now),
            )
        } ?: resources.getString(R.string.ask_exercise_none)

        is AskAnswer.AvoidHeat -> if (answer.hasStrongHeat) {
            resources.getString(
                R.string.ask_heat_headline,
                temp(answer.peakFeelsLikeC),
                window(answer.hottestWindow, now),
            )
        } else {
            resources.getString(R.string.ask_heat_none, temp(answer.peakFeelsLikeC))
        }

        is AskAnswer.Laundry -> answer.best?.let {
            resources.getString(R.string.ask_laundry_headline, window(it, now))
        } ?: resources.getString(R.string.ask_laundry_none)

        is AskAnswer.Rain -> if (answer.days.any { (it.chance ?: 0) >= 30 || it.mm >= 0.2 }) {
            resources.getString(R.string.ask_rain_headline_yes)
        } else {
            resources.getString(R.string.ask_rain_headline_no)
        }

        is AskAnswer.Air -> if (answer.unavailable) {
            resources.getString(R.string.ask_air_unavailable)
        } else {
            resources.getString(
                R.string.ask_air_headline,
                answer.level?.let {
                    resources.getString(it.labelRes())
                }.orEmpty(),
            )
        }
    }

    private fun chips(answer: AskAnswer, now: Instant): List<String> = when (answer) {
        is AskAnswer.RoutineNeeded -> listOf(resources.getString(R.string.ask_chip_setup_routine))

        is AskAnswer.Wear -> {
            val briefText = BriefText(resources, formatter, zone)
            listOf(briefText.routineSummary(answer.brief), briefText.dayOutlook(answer.brief))
        }

        is AskAnswer.ExerciseWindow -> buildList {
            answer.feelsLikeRangeC?.let {
                add(resources.getString(R.string.ask_chip_feels, tempRange(it.start, it.endInclusive)))
            }
            answer.best?.let { add(window(it, now)) }
            add(
                resources.getQuantityString(
                    R.plurals.ask_chip_alternatives,
                    answer.alternatives.size,
                    formatter.number(answer.alternatives.size),
                ),
            )
        }

        is AskAnswer.AvoidHeat -> buildList {
            add(resources.getString(R.string.ask_chip_peak, temp(answer.peakFeelsLikeC)))
            add(window(answer.hottestWindow, now))
            add(
                resources.getQuantityString(
                    R.plurals.ask_chip_cooler,
                    answer.coolerWindows.size,
                    formatter.number(answer.coolerWindows.size),
                ),
            )
        }

        is AskAnswer.Laundry -> buildList {
            answer.best?.let { add(window(it, now)) }
            add(resources.getString(R.string.ask_chip_daytime))
        }

        is AskAnswer.Rain -> answer.days.map { day ->
            resources.getString(
                R.string.ask_chip_rain_day,
                formatter.dayOfWeekShort(day.date),
                day.chance?.let(formatter::percent) ?: resources.getString(R.string.core_ui_unavailable),
                formatter.precipitation(day.mm),
            )
        }

        is AskAnswer.Air -> if (answer.unavailable) {
            emptyList()
        } else {
            buildList {
                answer.usAqi?.let { add(resources.getString(R.string.ask_chip_aqi, formatter.number(it))) }
                answer.pm10?.let { add(resources.getString(R.string.ask_chip_pm10, micrograms(it))) }
                answer.dust?.let { add(resources.getString(R.string.ask_chip_dust, micrograms(it))) }
            }
        }
    }

    private fun reasons(answer: AskAnswer, now: Instant): List<String> = when (answer) {
        is AskAnswer.RoutineNeeded -> listOf(resources.getString(R.string.ask_reason_setup_routine))

        is AskAnswer.Wear -> answer.brief.carry.map { BriefText(resources, formatter, zone).carry(it) }

        is AskAnswer.ExerciseWindow -> answer.reasons.map(::reason)

        is AskAnswer.AvoidHeat -> answer.coolerWindows.map {
            resources.getString(R.string.ask_reason_cooler_window, window(it, now))
        }

        is AskAnswer.Laundry -> answer.reasons.map(::reason)

        is AskAnswer.Rain -> answer.days.mapNotNull { day ->
            day.firstRainyHour?.let {
                resources.getString(R.string.ask_reason_first_rain, formatter.dayOfWeekFull(day.date), time(it))
            }
        }

        is AskAnswer.Air -> if (answer.unavailable) {
            listOf(reason(AskReason.AIR_DATA_UNAVAILABLE))
        } else {
            answer.dustPeak?.let { listOf(resources.getString(R.string.ask_reason_dust_peak, time(it))) }.orEmpty()
        }
    }

    private fun freshness(answer: AskAnswer): String {
        val fetched = answer.forecastFetchedAt?.let { time(it) } ?: resources.getString(R.string.core_ui_unavailable)
        val coverage = resources.getString(
            if (answer.coverage == ForecastCoverage.FULL) R.string.ask_coverage_full else R.string.ask_coverage_partial,
        )
        return resources.getString(R.string.ask_freshness, fetched, coverage)
    }

    private fun reason(reason: AskReason): String = resources.getString(reason.labelRes())

    /** "Tomorrow 6:00–8:00 AM": the day matters because answers span three days. */
    private fun window(window: TimeWindow, now: Instant): String {
        val date = window.start.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        val day = when (date) {
            today -> resources.getString(R.string.brief_today)
            today.plusDays(1) -> resources.getString(R.string.brief_tomorrow)
            else -> formatter.dayOfWeekFull(date)
        }
        return resources.getString(R.string.ask_day_window, day, time(window.start), time(window.end))
    }

    private fun time(instant: Instant): String = formatter.time(instant, zone)

    private fun temp(celsius: Double): String = LTR_START + formatter.temperature(celsius) + LTR_END

    private fun micrograms(value: Double): String = ltr(formatter.number(value.toInt()) + " µg/m³")

    private fun tempRange(minC: Double, maxC: Double): String =
        if (temp(minC) == temp(maxC)) temp(minC) else resources.getString(R.string.brief_range, temp(minC), temp(maxC))

    private fun ltr(text: String): String = LTR_START + text + LTR_END

    private companion object {
        const val LTR_START = "\u2066"
        const val LTR_END = "\u2069"
    }
}

data class AskDisplay(val headline: String, val chips: List<String>, val reasons: List<String>, val freshness: String)

@StringRes
private fun AskQuestion.labelRes(): Int = when (this) {
    AskQuestion.WHAT_TO_WEAR -> R.string.ask_question_wear
    AskQuestion.BEST_EXERCISE_TIME -> R.string.ask_question_exercise
    AskQuestion.AVOID_HEAT -> R.string.ask_question_heat
    AskQuestion.LAUNDRY -> R.string.ask_question_laundry
    AskQuestion.RAIN_NEXT_DAYS -> R.string.ask_question_rain
    AskQuestion.AIR_QUALITY -> R.string.ask_question_air
}

@StringRes
private fun ExerciseKind.labelRes(): Int = when (this) {
    ExerciseKind.WALK -> R.string.ask_exercise_walk
    ExerciseKind.RUN -> R.string.ask_exercise_run
    ExerciseKind.CYCLE -> R.string.ask_exercise_cycle
}

@StringRes
private fun AskReason.labelRes(): Int = when (this) {
    AskReason.COMFORTABLE_TEMP -> R.string.ask_reason_comfortable_temp
    AskReason.NO_RAIN -> R.string.ask_reason_no_rain
    AskReason.LOW_UV -> R.string.ask_reason_low_uv
    AskReason.CALM_WIND -> R.string.ask_reason_calm_wind
    AskReason.CLEAN_AIR -> R.string.ask_reason_clean_air
    AskReason.LOW_HUMIDITY -> R.string.ask_reason_low_humidity
    AskReason.GOOD_DRYING_WIND -> R.string.ask_reason_good_wind
    AskReason.AVOIDS_PEAK_HEAT -> R.string.ask_reason_avoids_peak_heat
    AskReason.COOLER_TIME -> R.string.ask_reason_cooler_time
    AskReason.RAIN_EXPECTED -> R.string.ask_reason_rain_expected
    AskReason.AIR_DATA_UNAVAILABLE -> R.string.ask_reason_air_unavailable
    AskReason.LIMITED_FORECAST -> R.string.ask_reason_limited_forecast
}

@StringRes
private fun AirQualityLevel.labelRes(): Int = when (this) {
    AirQualityLevel.GOOD -> R.string.core_ui_aqi_good
    AirQualityLevel.MODERATE -> R.string.core_ui_aqi_moderate
    AirQualityLevel.UNHEALTHY_FOR_SENSITIVE -> R.string.core_ui_aqi_sensitive
    AirQualityLevel.UNHEALTHY -> R.string.core_ui_aqi_unhealthy
    AirQualityLevel.VERY_UNHEALTHY -> R.string.core_ui_aqi_very_unhealthy
    AirQualityLevel.HAZARDOUS -> R.string.core_ui_aqi_hazardous
}
