package com.example.data.domain

import com.example.data.repository.WeatherRepository
import javax.inject.Inject

class SaveLastSearchedCityUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(cityName: String) {
        weatherRepository.saveLastSearchedCity(cityName)
    }
}