package com.mk.skycast.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `maps WMO codes to conditions`() {
        assertThat(WeatherCondition.fromWmoCode(0)).isEqualTo(WeatherCondition.CLEAR)
        assertThat(WeatherCondition.fromWmoCode(3)).isEqualTo(WeatherCondition.OVERCAST)
        assertThat(WeatherCondition.fromWmoCode(48)).isEqualTo(WeatherCondition.FOG)
        assertThat(WeatherCondition.fromWmoCode(63)).isEqualTo(WeatherCondition.RAIN)
        assertThat(WeatherCondition.fromWmoCode(75)).isEqualTo(WeatherCondition.SNOW)
        assertThat(WeatherCondition.fromWmoCode(82)).isEqualTo(WeatherCondition.RAIN_SHOWERS)
        assertThat(WeatherCondition.fromWmoCode(99)).isEqualTo(WeatherCondition.THUNDERSTORM_HAIL)
    }

    @Test
    fun `unknown or null codes map to UNKNOWN`() {
        assertThat(WeatherCondition.fromWmoCode(null)).isEqualTo(WeatherCondition.UNKNOWN)
        assertThat(WeatherCondition.fromWmoCode(42)).isEqualTo(WeatherCondition.UNKNOWN)
    }

    @Test
    fun `aqi levels follow US EPA breakpoints`() {
        assertThat(AirQualityLevel.fromUsAqi(50)).isEqualTo(AirQualityLevel.GOOD)
        assertThat(AirQualityLevel.fromUsAqi(51)).isEqualTo(AirQualityLevel.MODERATE)
        assertThat(AirQualityLevel.fromUsAqi(151)).isEqualTo(AirQualityLevel.UNHEALTHY)
        assertThat(AirQualityLevel.fromUsAqi(400)).isEqualTo(AirQualityLevel.HAZARDOUS)
    }
}
