package com.mk.skycast.core.model

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.KMH,
    val precipitationUnit: PrecipitationUnit = PrecipitationUnit.MILLIMETERS,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val selectedLocationId: Long? = null,
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class WindSpeedUnit { KMH, MPH, METERS_PER_SECOND, KNOTS }

enum class PrecipitationUnit { MILLIMETERS, INCHES }

enum class PressureUnit { HPA, INHG }

enum class TimeFormat { SYSTEM, HOUR_12, HOUR_24 }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
