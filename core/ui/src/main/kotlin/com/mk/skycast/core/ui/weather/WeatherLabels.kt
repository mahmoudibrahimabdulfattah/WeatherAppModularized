package com.mk.skycast.core.ui.weather

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mk.skycast.core.model.AirQualityLevel
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.ui.R

@Composable
fun conditionLabel(condition: WeatherCondition): String = stringResource(condition.labelRes())

@Composable
fun airQualityLabel(level: AirQualityLevel?): String = stringResource(level.labelRes())

@Composable
fun uvLabel(uvIndex: Double): String = stringResource(uvLabelRes(uvIndex))

@StringRes
private fun WeatherCondition.labelRes(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.core_ui_clear
    WeatherCondition.MAINLY_CLEAR -> R.string.core_ui_mainly_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.core_ui_partly_cloudy
    WeatherCondition.OVERCAST -> R.string.core_ui_overcast
    WeatherCondition.FOG -> R.string.core_ui_fog
    WeatherCondition.DRIZZLE -> R.string.core_ui_drizzle
    WeatherCondition.FREEZING_DRIZZLE -> R.string.core_ui_freezing_drizzle
    WeatherCondition.RAIN -> R.string.core_ui_rain
    WeatherCondition.FREEZING_RAIN -> R.string.core_ui_freezing_rain
    WeatherCondition.SNOW -> R.string.core_ui_snow
    WeatherCondition.SNOW_GRAINS -> R.string.core_ui_snow_grains
    WeatherCondition.RAIN_SHOWERS -> R.string.core_ui_rain_showers
    WeatherCondition.SNOW_SHOWERS -> R.string.core_ui_snow_showers
    WeatherCondition.THUNDERSTORM -> R.string.core_ui_thunderstorm
    WeatherCondition.THUNDERSTORM_HAIL -> R.string.core_ui_thunderstorm_hail
    WeatherCondition.UNKNOWN -> R.string.core_ui_unknown
}

@StringRes
private fun AirQualityLevel?.labelRes(): Int = when (this) {
    AirQualityLevel.GOOD -> R.string.core_ui_aqi_good
    AirQualityLevel.MODERATE -> R.string.core_ui_aqi_moderate
    AirQualityLevel.UNHEALTHY_FOR_SENSITIVE -> R.string.core_ui_aqi_sensitive
    AirQualityLevel.UNHEALTHY -> R.string.core_ui_aqi_unhealthy
    AirQualityLevel.VERY_UNHEALTHY -> R.string.core_ui_aqi_very_unhealthy
    AirQualityLevel.HAZARDOUS -> R.string.core_ui_aqi_hazardous
    null -> R.string.core_ui_unavailable
}

/** WHO UV index bands. */
@StringRes
private fun uvLabelRes(uvIndex: Double): Int = when {
    uvIndex < 3 -> R.string.core_ui_uv_low
    uvIndex < 6 -> R.string.core_ui_uv_moderate
    uvIndex < 8 -> R.string.core_ui_uv_high
    uvIndex < 11 -> R.string.core_ui_uv_very_high
    else -> R.string.core_ui_uv_extreme
}
