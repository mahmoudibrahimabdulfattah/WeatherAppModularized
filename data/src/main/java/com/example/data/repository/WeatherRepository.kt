package com.example.data.repository

import com.example.core.Result
import com.example.data.models.CurrentWeather
import com.example.data.models.ForecastDay
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getCurrentWeather(cityName: String): Result<CurrentWeather>
    suspend fun getForecast(cityName: String): Result<List<ForecastDay>>
    fun getLastSearchedCity(): Flow<String>
    suspend fun saveLastSearchedCity(cityName: String)
}