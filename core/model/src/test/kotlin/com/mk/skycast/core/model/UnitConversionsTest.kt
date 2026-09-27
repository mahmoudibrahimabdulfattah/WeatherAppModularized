package com.mk.skycast.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitConversionsTest {

    @Test
    fun temperature() {
        assertThat(UnitConversions.temperature(0.0, TemperatureUnit.FAHRENHEIT)).isWithin(1e-9).of(32.0)
        assertThat(UnitConversions.temperature(100.0, TemperatureUnit.FAHRENHEIT)).isWithin(1e-9).of(212.0)
        assertThat(UnitConversions.temperature(21.5, TemperatureUnit.CELSIUS)).isEqualTo(21.5)
    }

    @Test
    fun windSpeed() {
        assertThat(UnitConversions.windSpeed(36.0, WindSpeedUnit.METERS_PER_SECOND)).isWithin(1e-9).of(10.0)
        assertThat(UnitConversions.windSpeed(100.0, WindSpeedUnit.MPH)).isWithin(0.01).of(62.14)
        assertThat(UnitConversions.windSpeed(100.0, WindSpeedUnit.KNOTS)).isWithin(0.01).of(54.0)
    }

    @Test
    fun precipitationAndPressure() {
        assertThat(UnitConversions.precipitation(25.4, PrecipitationUnit.INCHES)).isWithin(1e-9).of(1.0)
        assertThat(UnitConversions.pressure(1013.25, PressureUnit.INHG)).isWithin(0.01).of(29.92)
    }
}
