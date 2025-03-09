package com.example.data.domain

import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLastSearchedCityUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(): Flow<String> {
        return weatherRepository.getLastSearchedCity()
    }
}