package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.repository.WeatherRepository
import javax.inject.Inject

class RefreshWeatherUseCase @Inject constructor(private val weatherRepository: WeatherRepository) {
    suspend operator fun invoke(locationId: Long, force: Boolean = false): Outcome<Unit, DataError> =
        weatherRepository.refresh(locationId, force)
}
