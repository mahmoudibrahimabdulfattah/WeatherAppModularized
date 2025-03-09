package com.example.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.core.Result
import com.example.data.data_source.CityPreferences
import com.example.data.data_source.WeatherRemoteDataSource
import com.example.data.models.CurrentWeather
import com.example.data.models.ForecastDay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val cityPreferences: CityPreferences
) : WeatherRepository {

    override suspend fun getCurrentWeather(cityName: String): Result<CurrentWeather> {
        return try {
            val weather = remoteDataSource.getCurrentWeather(cityName)
            Result.success(weather)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getForecast(cityName: String): Result<List<ForecastDay>> {
        return try {
            val forecast = remoteDataSource.getForecast(cityName)
            Result.success(forecast)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun getLastSearchedCity(): Flow<String> {
        return cityPreferences.lastCity
    }

    override suspend fun saveLastSearchedCity(cityName: String) {
        cityPreferences.saveCity(cityName)
    }
}