package com.mk.skycast.core.model

/** Pure unit conversions. Source values are always metric. */
object UnitConversions {

    fun temperature(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
        TemperatureUnit.CELSIUS -> celsius
        TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    }

    fun windSpeed(kmh: Double, unit: WindSpeedUnit): Double = when (unit) {
        WindSpeedUnit.KMH -> kmh
        WindSpeedUnit.MPH -> kmh * 0.621371
        WindSpeedUnit.METERS_PER_SECOND -> kmh / 3.6
        WindSpeedUnit.KNOTS -> kmh * 0.539957
    }

    fun precipitation(mm: Double, unit: PrecipitationUnit): Double = when (unit) {
        PrecipitationUnit.MILLIMETERS -> mm
        PrecipitationUnit.INCHES -> mm / 25.4
    }

    fun pressure(hpa: Double, unit: PressureUnit): Double = when (unit) {
        PressureUnit.HPA -> hpa
        PressureUnit.INHG -> hpa * 0.0295300
    }
}
