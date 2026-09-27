package com.mk.skycast.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Open-Meteo /v1/forecast response (timeformat=unixtime). */
@Serializable
data class NetworkForecast(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val current: NetworkCurrent,
    val hourly: NetworkHourly,
    val daily: NetworkDaily,
)

@Serializable
data class NetworkCurrent(
    val time: Long,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("dew_point_2m") val dewPoint: Double? = null,
    @SerialName("is_day") val isDay: Int,
    val precipitation: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("cloud_cover") val cloudCover: Int,
    @SerialName("pressure_msl") val pressureMsl: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Int,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null,
    val visibility: Double? = null,
)

@Serializable
data class NetworkHourly(
    val time: List<Long>,
    @SerialName("temperature_2m") val temperature: List<Double?>,
    @SerialName("weather_code") val weatherCode: List<Int?>,
    @SerialName("is_day") val isDay: List<Int?>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>,
    val precipitation: List<Double?>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>,
    @SerialName("uv_index") val uvIndex: List<Double?>,
)

@Serializable
data class NetworkDaily(
    val time: List<Long>,
    @SerialName("weather_code") val weatherCode: List<Int?>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double?>,
    val sunrise: List<Long?>,
    val sunset: List<Long?>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double?>,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?>,
)
