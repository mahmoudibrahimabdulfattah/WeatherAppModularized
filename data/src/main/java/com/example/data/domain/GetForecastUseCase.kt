package com.example.data.domain

import com.example.core.Result
import com.example.data.models.ForecastDay
import com.example.data.repository.WeatherRepository
import javax.inject.Inject

class GetForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(cityName: String): Result<List<ForecastDay>> {
        return weatherRepository.getForecast(cityName)
    }
}