package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Refreshes all saved locations in parallel; returns the first error, if any. */
class RefreshAllWeatherUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
) {
    suspend operator fun invoke(force: Boolean = false): Outcome<Unit, DataError> = coroutineScope {
        val results = locationRepository.getSavedLocations()
            .map { async { weatherRepository.refresh(it.id, force) } }
            .awaitAll()
        results.firstOrNull { it is Outcome.Failure } ?: Outcome.Success(Unit)
    }
}
