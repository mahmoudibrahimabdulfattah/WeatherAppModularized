package com.example.data.data_source

import com.example.data.BuildConfig
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.data.api.ForecastResponse
import com.example.data.api.WeatherApiService
import com.example.data.api.WeatherResponse
import com.example.data.di.Constants
import com.example.data.models.CurrentWeather
import com.example.data.models.ForecastDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class WeatherRemoteDataSource @Inject constructor(
    private val weatherApiService: WeatherApiService
) {

    suspend fun getCurrentWeather(cityName: String): CurrentWeather {
        val response = weatherApiService.getCurrentWeather(cityName, Constants.WEATHER_API_KEY)
        return mapToCurrentWeather(response)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getForecast(cityName: String): List<ForecastDay> {
        val response = weatherApiService.getForecast(cityName, Constants.WEATHER_API_KEY)
        return mapToForecastDays(response)
    }

    private fun mapToCurrentWeather(response: WeatherResponse): CurrentWeather {
        return CurrentWeather(
            cityName = response.cityName,
            temperature = response.main.temperature,
            weatherConditionCode = response.weather.firstOrNull()?.id ?: 0,
            weatherDescription = response.weather.firstOrNull()?.description ?: "",
            humidity = response.main.humidity,
            windSpeed = response.wind.speed,
            timestamp = response.timestamp
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapToForecastDays(response: ForecastResponse): List<ForecastDay> {
        // Group by day and take the noon forecast for each day
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dailyForecasts = response.list
            .groupBy { LocalDate.parse(it.dateText, dateFormatter).toString() }
            .map { (date, forecasts) ->
                // Find forecast closest to noon
                val noonForecast = forecasts.minByOrNull {
                    val forecastHour = LocalDate.parse(it.dateText, dateFormatter).atStartOfDay().hour
                    Math.abs(forecastHour - 12)
                } ?: forecasts.first()

                ForecastDay(
                    date = date,
                    temperature = noonForecast.main.temperature,
                    weatherConditionCode = noonForecast.weather.firstOrNull()?.id ?: 0,
                    weatherDescription = noonForecast.weather.firstOrNull()?.description ?: "",
                    humidity = noonForecast.main.humidity,
                    windSpeed = noonForecast.wind.speed
                )
            }
            .take(7)

        return dailyForecasts
    }
}