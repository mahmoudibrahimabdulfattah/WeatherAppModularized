package com.mk.skycast.feature.places

import com.mk.skycast.core.model.LocationWeather
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState
import com.mk.skycast.core.ui.text.UiText
import java.time.Instant

data class PlacesState(
    val query: String = "",
    val isSearching: Boolean = false,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val searchError: UiText? = null,
    val savedLocations: List<LocationWeather> = emptyList(),
    val selectedLocationId: Long? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLocating: Boolean = false,
    /** Ids currently being added, to show inline progress on a suggestion. */
    val addingIds: Set<Long> = emptySet(),
    /** Ticks every minute; drives each saved city's local clock. */
    val now: Instant = Instant.EPOCH,
) : UiState {
    val isSearchActive: Boolean get() = query.isNotBlank()
    val showNoResults: Boolean
        get() = isSearchActive && !isSearching && searchError == null &&
            suggestions.isEmpty() && query.trim().length >= MIN_QUERY_LENGTH
    val hasDeviceLocation: Boolean get() = savedLocations.any { it.location.isDeviceLocation }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

sealed interface PlacesIntent : UiIntent {
    data class QueryChanged(val query: String) : PlacesIntent
    data object ClearQuery : PlacesIntent
    data class SuggestionClicked(val place: PlaceSuggestion) : PlacesIntent
    data class LocationClicked(val locationId: Long) : PlacesIntent
    data class RemoveLocation(val locationId: Long) : PlacesIntent

    /** Full new order of the non-device locations after a drag/move. */
    data class ReorderLocations(val orderedIds: List<Long>) : PlacesIntent
    data object UseDeviceLocationClicked : PlacesIntent
    data class LocationPermissionResult(val granted: Boolean) : PlacesIntent
    data object BackClicked : PlacesIntent
}

sealed interface PlacesEffect : UiEffect {
    data object NavigateBack : PlacesEffect
    data object RequestLocationPermission : PlacesEffect
    data class ShowMessage(val message: UiText) : PlacesEffect
}
