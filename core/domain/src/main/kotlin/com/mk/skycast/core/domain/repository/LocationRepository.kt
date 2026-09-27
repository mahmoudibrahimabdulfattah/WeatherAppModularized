package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.model.DeviceLocation
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.SavedLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeSavedLocations(): Flow<List<SavedLocation>>
    suspend fun getSavedLocations(): List<SavedLocation>

    /** Saves [place] (or returns the existing id if already saved). */
    suspend fun save(place: PlaceSuggestion): Long

    /** Creates or updates the single "my location" entry. */
    suspend fun saveDeviceLocation(location: DeviceLocation): Long

    suspend fun remove(locationId: Long)

    /** Persists a new ordering. Ids not listed keep their relative order at the end. */
    suspend fun reorder(orderedIds: List<Long>)

    /** Re-fetches names of searched places whose names are not in [languageCode]. */
    suspend fun localizeNames(languageCode: String): Outcome<Unit, DataError>
}
