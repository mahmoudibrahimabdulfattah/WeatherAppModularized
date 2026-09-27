package com.mk.skycast.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkAirQuality(
    val current: NetworkAirQualityCurrent = NetworkAirQualityCurrent(),
    val hourly: NetworkAirQualityHourly? = null,
)

@Serializable
data class NetworkAirQualityCurrent(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
)

@Serializable
data class NetworkAirQualityHourly(
    val time: List<Long> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?> = emptyList(),
    val pm10: List<Double?> = emptyList(),
    val dust: List<Double?> = emptyList(),
)
