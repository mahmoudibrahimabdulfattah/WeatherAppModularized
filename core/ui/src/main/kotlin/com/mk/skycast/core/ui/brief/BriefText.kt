package com.mk.skycast.core.ui.brief

import android.content.res.Resources
import androidx.annotation.StringRes
import com.mk.skycast.core.model.CarryItem
import com.mk.skycast.core.model.CarrySuggestion
import com.mk.skycast.core.model.ClothingLevel
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.DayChangeKind
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.ExposureKind
import com.mk.skycast.core.model.ExposureWindow
import com.mk.skycast.core.model.Hazard
import com.mk.skycast.core.model.HazardAlert
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.WindowOutlook
import com.mk.skycast.core.ui.R
import com.mk.skycast.core.ui.format.WeatherFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Deterministic wording for a [DailyBrief], shared by the Home card and the
 * notification. Built on [Resources] so it also works outside composition.
 */
class BriefText(private val resources: Resources, private val formatter: WeatherFormatter, private val zone: ZoneId) {
    /** "Tomorrow" / "Today" relative to [now]. */
    fun dayWord(brief: DailyBrief, now: Instant): String {
        val today = now.atZone(zone).toLocalDate()
        return resources.getString(if (brief.date == today) R.string.brief_today else R.string.brief_tomorrow)
    }

    /** "Tomorrow · Monday" */
    fun title(brief: DailyBrief, now: Instant): String =
        resources.getString(R.string.brief_title, dayWord(brief, now), formatter.dayOfWeekFull(brief.date))

    /** "Work/study day · 1 outing" */
    fun routineSummary(brief: DailyBrief): String {
        val day = resources.getString(
            when (brief.dayType) {
                DayType.AWAY -> R.string.brief_day_away
                DayType.HOME -> R.string.brief_day_home
                DayType.OFF -> R.string.brief_day_off
            },
        )
        val outings = brief.windows.count { it.window.kind == ExposureKind.OUTING }
        if (outings == 0) return day
        val count = resources.getQuantityString(R.plurals.brief_outings, outings, formatter.number(outings))
        return resources.getString(R.string.brief_joined, day, count)
    }

    /** The one decision that matters most, e.g. "Tomorrow: take an umbrella". */
    fun headline(brief: DailyBrief, now: Instant): String =
        resources.getString(R.string.brief_headline, dayWord(brief, now), action(brief))

    /** True when nothing outranks clothing, so the headline already says what to wear. */
    private fun headlineIsClothing(brief: DailyBrief) = brief.hazards.none { it.hazard in HEADLINE_HAZARDS } &&
        brief.carry.none { it.item in HEADLINE_ITEMS } &&
        brief.change == null

    private fun action(brief: DailyBrief): String {
        brief.hazards.firstOrNull { it.hazard in HEADLINE_HAZARDS }
            ?.let { return resources.getString(it.hazard.actionRes()) }
        brief.carry.firstOrNull { it.item in HEADLINE_ITEMS }?.let { return resources.getString(it.item.actionRes()) }
        brief.change?.let { change ->
            val delta = change.deltaC?.let(formatter::temperatureDelta).orEmpty()
            return when (change.kind) {
                DayChangeKind.WARMER -> resources.getString(R.string.brief_action_warmer, delta)
                DayChangeKind.COLDER -> resources.getString(R.string.brief_action_colder, delta)
                DayChangeKind.WINDIER -> resources.getString(R.string.brief_action_windier)
                DayChangeKind.RAINIER -> resources.getString(R.string.brief_action_rainier)
            }
        }
        val clothing = brief.clothing ?: return resources.getString(R.string.brief_action_no_outing)
        return resources.getString(R.string.brief_action_wear, resources.getString(clothing.labelRes()))
    }

    /** "Wear a light layer — it feels like 16° when you leave at 8:00 AM." plus the swing hint. */
    fun wear(brief: DailyBrief): String? {
        val clothing = brief.clothing ?: return null
        val first = brief.windows.first()
        val feels = temp(first.feelsLikeMinC)
        val leave = time(first.window.start)
        val line = if (headlineIsClothing(brief)) {
            resources.getString(R.string.brief_wear_reason, feels, leave)
        } else {
            resources.getString(R.string.brief_wear_line, resources.getString(clothing.labelRes()), feels, leave)
        }
        if (!brief.layerForSwing) return line
        val warmest = brief.windows.maxOf { it.feelsLikeMaxC }
        return line + " " + resources.getString(R.string.brief_wear_swing, temp(warmest))
    }

    /** Explains that clothing advice was adjusted from the user's feedback, or null if it wasn't. */
    fun comfortNote(brief: DailyBrief): String? = when {
        brief.clothing == null || kotlin.math.abs(brief.comfortOffsetC) < 1.0 -> null
        brief.comfortOffsetC < 0 -> resources.getString(R.string.brief_tuned_cold)
        else -> resources.getString(R.string.brief_tuned_warm)
    }

    fun carry(suggestion: CarrySuggestion): String =
        resources.getString(suggestion.item.lineRes(), where(suggestion.because), time(suggestion.at))

    fun hazard(alert: HazardAlert): String =
        resources.getString(alert.hazard.lineRes(), alert.window?.let(::where).orEmpty(), time(alert.at)).trim()

    /** "High 30° · low 20° · sunset 6:04 PM" */
    fun dayOutlook(brief: DailyBrief): String {
        val temps = resources.getString(
            R.string.brief_day_temps,
            temp(brief.day.maxC),
            temp(brief.day.minC),
        )
        val sunset = brief.day.sunset ?: return temps
        return resources.getString(
            R.string.brief_joined,
            temps,
            resources.getString(R.string.brief_sunset, time(sunset)),
        )
    }

    fun windowLabel(window: ExposureWindow): String = when (window.kind) {
        ExposureKind.COMMUTE_OUT -> resources.getString(R.string.brief_window_out)
        ExposureKind.COMMUTE_BACK -> resources.getString(R.string.brief_window_back)
        ExposureKind.OUTING -> window.outing?.let(::outingLabel) ?: resources.getString(R.string.brief_kind_outing)
    }

    fun outingLabel(outing: Outing): String =
        outing.customLabel?.takeIf { outing.kind == OutingKind.CUSTOM && it.isNotBlank() }
            ?: resources.getString(outing.kind.labelRes())

    /** "8:00 AM · feels 14–16°" */
    fun windowDetail(outlook: WindowOutlook): String {
        val min = temp(outlook.feelsLikeMinC)
        val max = temp(outlook.feelsLikeMaxC)
        val feels = if (min == max) min else resources.getString(R.string.brief_range, min, max)
        return resources.getString(R.string.brief_window_detail, time(outlook.window.start), feels)
    }

    fun time(instant: Instant): String = formatter.time(instant, zone)

    // Isolated so "26°" keeps its reading order inside Arabic sentences.
    private fun temp(celsius: Double): String = LTR_START + formatter.temperature(celsius) + LTR_END

    private fun deltaText(celsius: Double): String = LTR_START + formatter.temperatureDelta(celsius) + LTR_END

    fun isToday(date: LocalDate, now: Instant) = date == now.atZone(zone).toLocalDate()

    private fun where(kind: ExposureKind): String = resources.getString(
        when (kind) {
            ExposureKind.COMMUTE_OUT -> R.string.brief_when_out
            ExposureKind.COMMUTE_BACK -> R.string.brief_when_back
            ExposureKind.OUTING -> R.string.brief_when_outing
        },
    )

    private companion object {
        const val LTR_START = "\u2066"
        const val LTR_END = "\u2069"
        val HEADLINE_HAZARDS = setOf(
            Hazard.THUNDERSTORM,
            Hazard.HEAVY_RAIN,
            Hazard.FOG,
            Hazard.DUST,
            Hazard.EXTREME_HEAT,
            Hazard.STRONG_WIND,
        )
        val HEADLINE_ITEMS = setOf(CarryItem.UMBRELLA, CarryItem.RAINCOAT, CarryItem.EXTRA_LAYER, CarryItem.MASK)
    }
}

// Exhaustive `when`s: a new enum value fails compilation until it gets wording.

@StringRes
fun ClothingLevel.labelRes(): Int = when (this) {
    ClothingLevel.VERY_LIGHT -> R.string.brief_wear_very_light
    ClothingLevel.LIGHT -> R.string.brief_wear_light
    ClothingLevel.LIGHT_LAYER -> R.string.brief_wear_light_layer
    ClothingLevel.JACKET -> R.string.brief_wear_jacket
    ClothingLevel.WARM_COAT -> R.string.brief_wear_warm_coat
    ClothingLevel.HEAVY -> R.string.brief_wear_heavy
}

@StringRes
fun CarryItem.labelRes(): Int = when (this) {
    CarryItem.UMBRELLA -> R.string.brief_item_umbrella
    CarryItem.RAINCOAT -> R.string.brief_item_raincoat
    CarryItem.EXTRA_LAYER -> R.string.brief_item_extra_layer
    CarryItem.SUNSCREEN -> R.string.brief_item_sunscreen
    CarryItem.WATER -> R.string.brief_item_water
    CarryItem.MASK -> R.string.brief_item_mask
}

@StringRes
private fun CarryItem.actionRes(): Int = when (this) {
    CarryItem.UMBRELLA -> R.string.brief_action_umbrella
    CarryItem.RAINCOAT -> R.string.brief_action_raincoat
    CarryItem.EXTRA_LAYER -> R.string.brief_action_extra_layer
    CarryItem.SUNSCREEN -> R.string.brief_action_sunscreen
    CarryItem.WATER -> R.string.brief_action_water
    CarryItem.MASK -> R.string.brief_action_mask
}

@StringRes
private fun CarryItem.lineRes(): Int = when (this) {
    CarryItem.UMBRELLA -> R.string.brief_carry_umbrella
    CarryItem.RAINCOAT -> R.string.brief_carry_raincoat
    CarryItem.EXTRA_LAYER -> R.string.brief_carry_extra_layer
    CarryItem.SUNSCREEN -> R.string.brief_carry_sunscreen
    CarryItem.WATER -> R.string.brief_carry_water
    CarryItem.MASK -> R.string.brief_carry_mask
}

@StringRes
private fun Hazard.actionRes(): Int = when (this) {
    Hazard.THUNDERSTORM -> R.string.brief_action_thunderstorm
    Hazard.HEAVY_RAIN -> R.string.brief_action_heavy_rain
    Hazard.RAIN -> R.string.brief_action_umbrella
    Hazard.FOG -> R.string.brief_action_fog
    Hazard.STRONG_WIND -> R.string.brief_action_strong_wind
    Hazard.DUST -> R.string.brief_action_dust
    Hazard.POOR_AIR -> R.string.brief_action_mask
    Hazard.EXTREME_HEAT -> R.string.brief_action_extreme_heat
    Hazard.HIGH_UV -> R.string.brief_action_sunscreen
    Hazard.COLD -> R.string.brief_action_cold
}

@StringRes
private fun Hazard.lineRes(): Int = when (this) {
    Hazard.THUNDERSTORM -> R.string.brief_hazard_thunderstorm
    Hazard.HEAVY_RAIN -> R.string.brief_hazard_heavy_rain
    Hazard.RAIN -> R.string.brief_hazard_rain
    Hazard.FOG -> R.string.brief_hazard_fog
    Hazard.STRONG_WIND -> R.string.brief_hazard_strong_wind
    Hazard.DUST -> R.string.brief_hazard_dust
    Hazard.POOR_AIR -> R.string.brief_hazard_poor_air
    Hazard.EXTREME_HEAT -> R.string.brief_hazard_extreme_heat
    Hazard.HIGH_UV -> R.string.brief_hazard_high_uv
    Hazard.COLD -> R.string.brief_hazard_cold
}

@StringRes
private fun OutingKind.labelRes(): Int = when (this) {
    OutingKind.GYM -> R.string.brief_kind_gym
    OutingKind.OUTING -> R.string.brief_kind_outing
    OutingKind.FAMILY -> R.string.brief_kind_family
    OutingKind.PRAYER -> R.string.brief_kind_prayer
    OutingKind.CUSTOM -> R.string.brief_kind_outing
}
