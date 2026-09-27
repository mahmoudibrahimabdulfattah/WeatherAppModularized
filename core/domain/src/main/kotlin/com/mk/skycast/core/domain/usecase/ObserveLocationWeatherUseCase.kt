package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.LocationWeather
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Streams every saved location together with its cached weather; updates on any change. */
class ObserveLocationWeatherUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
) {
    operator fun invoke(): Flow<List<LocationWeather>> = locationRepository.observeSavedLocations()
        .flatMapLatest { locations ->
            if (locations.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    locations.map { location ->
                        weatherRepository.observeWeather(location.id)
                            .map { LocationWeather(location, it) }
                    },
                ) { it.toList() }
            }
        }
        .distinctUntilChanged()
}
