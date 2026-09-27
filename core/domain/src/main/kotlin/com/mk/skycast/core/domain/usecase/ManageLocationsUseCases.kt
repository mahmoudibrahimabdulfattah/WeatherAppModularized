package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class RemoveLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(locationId: Long) {
        locationRepository.remove(locationId)
        if (preferencesRepository.preferences.first().selectedLocationId == locationId) {
            preferencesRepository.setSelectedLocationId(
                locationRepository.getSavedLocations().firstOrNull()?.id,
            )
        }
    }
}

class ReorderLocationsUseCase @Inject constructor(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(orderedIds: List<Long>) = locationRepository.reorder(orderedIds)
}

class SelectLocationUseCase @Inject constructor(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(locationId: Long) = preferencesRepository.setSelectedLocationId(locationId)
}
