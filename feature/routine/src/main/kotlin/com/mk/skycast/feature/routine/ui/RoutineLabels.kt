package com.mk.skycast.feature.routine.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.feature.routine.R
import java.time.DayOfWeek
import java.time.format.TextStyle

// Exhaustive `when`s: a new enum value fails compilation until it gets a label.

/** Week rows start on Sunday (the regional default); RTL layouts mirror them automatically. */
internal val weekOrder: List<DayOfWeek> = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)

internal val travelMinuteOptions = listOf(15, 30, 45, 60, 90)

@StringRes
internal fun DayType.labelRes(): Int = when (this) {
    DayType.AWAY -> R.string.routine_day_away
    DayType.HOME -> R.string.routine_day_home
    DayType.OFF -> R.string.routine_day_off
}

@StringRes
internal fun TravelMode.labelRes(): Int = when (this) {
    TravelMode.CAR -> R.string.routine_mode_car
    TravelMode.PUBLIC_TRANSPORT -> R.string.routine_mode_public
    TravelMode.WALK -> R.string.routine_mode_walk
    TravelMode.BICYCLE -> R.string.routine_mode_bicycle
    TravelMode.MOTORBIKE -> R.string.routine_mode_motorbike
}

@StringRes
internal fun OutingSetting.labelRes(): Int = when (this) {
    OutingSetting.INDOORS -> R.string.routine_setting_indoors
    OutingSetting.OUTDOORS -> R.string.routine_setting_outdoors
}

@StringRes
internal fun OutingKind.labelRes(): Int = when (this) {
    OutingKind.GYM -> R.string.routine_kind_gym
    OutingKind.OUTING -> R.string.routine_kind_outing
    OutingKind.FAMILY -> R.string.routine_kind_family
    OutingKind.PRAYER -> R.string.routine_kind_prayer
    OutingKind.CUSTOM -> R.string.routine_kind_custom
}

@Composable
internal fun Outing.title(): String =
    customLabel?.takeIf { kind == OutingKind.CUSTOM && it.isNotBlank() } ?: stringResource(kind.labelRes())

@Composable
internal fun DayOfWeek.displayName(style: TextStyle = TextStyle.FULL): String =
    getDisplayName(style, LocalConfiguration.current.locales[0])

/** "Sun, Tue, Thu" in week order. */
@Composable
internal fun Set<DayOfWeek>.shortList(): String = weekOrder.filter {
    it in this
}.map { it.displayName(TextStyle.SHORT) }.joinToString(stringResource(R.string.routine_list_separator))
