package com.mk.skycast.core.network

import com.mk.skycast.core.network.model.NetworkAirQuality
import com.mk.skycast.core.network.model.NetworkForecast
import com.mk.skycast.core.network.model.NetworkGeocodingResponse
import com.mk.skycast.core.network.model.NetworkPlace
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo: free, no API key. https://open-meteo.com */
internal interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("past_hours") pastHours: Int = 1,
    ): NetworkForecast

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,pm2_5,pm10",
        @Query("timeformat") timeFormat: String = "unixtime",
    ): NetworkAirQuality

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchPlaces(
        @Query("name") name: String,
        @Query("language") language: String,
        @Query("count") count: Int = 12,
        @Query("format") format: String = "json",
    ): NetworkGeocodingResponse

    @GET("https://geocoding-api.open-meteo.com/v1/get")
    suspend fun place(@Query("id") id: Long, @Query("language") language: String): NetworkPlace

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m," +
            "is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m," +
            "wind_direction_10m,wind_gusts_10m,uv_index,visibility"
        const val HOURLY_FIELDS = "temperature_2m,weather_code,is_day,precipitation_probability," +
            "precipitation,wind_speed_10m,uv_index"
        const val DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min," +
            "precipitation_probability_max,precipitation_sum,sunrise,sunset,uv_index_max,wind_speed_10m_max"
    }
}
