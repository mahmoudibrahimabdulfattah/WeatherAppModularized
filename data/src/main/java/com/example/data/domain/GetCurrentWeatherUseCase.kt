package com.example.data.domain

import com.example.core.Result
import com.example.data.models.CurrentWeather
import com.example.data.repository.WeatherRepository
import javax.inject.Inject

class GetCurrentWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(cityName: String): Result<CurrentWeather> {
        return weatherRepository.getCurrentWeather(cityName)
    }
}