package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.PlaceSuggestion
import javax.inject.Inject

/** Saves a place, makes it the selected location and fetches its weather. */
class AddPlaceUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(place: PlaceSuggestion): Long {
        val id = locationRepository.save(place)
        preferencesRepository.setSelectedLocationId(id)
        weatherRepository.refresh(id)
        return id
    }
}
