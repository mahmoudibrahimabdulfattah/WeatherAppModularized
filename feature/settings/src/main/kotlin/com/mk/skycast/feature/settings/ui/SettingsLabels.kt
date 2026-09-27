package com.mk.skycast.feature.settings.ui

import androidx.annotation.StringRes
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.WindSpeedUnit
import com.mk.skycast.feature.settings.R

// Exhaustive `when`s: a new enum value fails compilation until it gets a label.

@StringRes
internal fun TemperatureUnit.labelRes(): Int = when (this) {
    TemperatureUnit.CELSIUS -> R.string.settings_celsius
    TemperatureUnit.FAHRENHEIT -> R.string.settings_fahrenheit
}

@StringRes
internal fun WindSpeedUnit.labelRes(): Int = when (this) {
    WindSpeedUnit.KMH -> R.string.settings_kmh
    WindSpeedUnit.MPH -> R.string.settings_mph
    WindSpeedUnit.METERS_PER_SECOND -> R.string.settings_ms
    WindSpeedUnit.KNOTS -> R.string.settings_kn
}

@StringRes
internal fun PrecipitationUnit.labelRes(): Int = when (this) {
    PrecipitationUnit.MILLIMETERS -> R.string.settings_mm
    PrecipitationUnit.INCHES -> R.string.settings_in
}

@StringRes
internal fun PressureUnit.labelRes(): Int = when (this) {
    PressureUnit.HPA -> R.string.settings_hpa
    PressureUnit.INHG -> R.string.settings_inhg
}

@StringRes
internal fun TimeFormat.labelRes(): Int = when (this) {
    TimeFormat.SYSTEM -> R.string.settings_system
    TimeFormat.HOUR_12 -> R.string.settings_12h
    TimeFormat.HOUR_24 -> R.string.settings_24h
}

@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_system
    ThemeMode.LIGHT -> R.string.settings_light
    ThemeMode.DARK -> R.string.settings_dark
}

@StringRes
internal fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_system
    AppLanguage.ENGLISH -> R.string.settings_language_english
    AppLanguage.ARABIC -> R.string.settings_language_arabic
}
