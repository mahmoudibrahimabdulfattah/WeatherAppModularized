package com.mk.skycast.feature.places

import androidx.lifecycle.viewModelScope
import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.domain.usecase.AddPlaceUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.RemoveLocationUseCase
import com.mk.skycast.core.domain.usecase.ReorderLocationsUseCase
import com.mk.skycast.core.domain.usecase.SearchPlacesUseCase
import com.mk.skycast.core.domain.usecase.SelectLocationUseCase
import com.mk.skycast.core.domain.usecase.SyncDeviceLocationUseCase
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.mvi.MviViewModel
import com.mk.skycast.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class PlacesViewModel @Inject constructor(
    observeLocationWeather: ObserveLocationWeatherUseCase,
    observePreferences: ObserveUserPreferencesUseCase,
    private val searchPlaces: SearchPlacesUseCase,
    private val addPlace: AddPlaceUseCase,
    private val removeLocation: RemoveLocationUseCase,
    private val reorderLocations: ReorderLocationsUseCase,
    private val selectLocation: SelectLocationUseCase,
    private val syncDeviceLocation: SyncDeviceLocationUseCase,
    ticker: TimeTicker,
) : MviViewModel<PlacesState, PlacesIntent, PlacesEffect>(PlacesState()) {

    private val queryFlow = MutableStateFlow("")

    init {
        combine(observeLocationWeather(), observePreferences(), ticker.ticks) { locations, preferences, now ->
            reduce {
                copy(
                    savedLocations = locations,
                    preferences = preferences,
                    selectedLocationId = preferences.selectedLocationId,
                    now = now,
                )
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            queryFlow
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest(::search)
        }
    }

    override fun onIntent(intent: PlacesIntent) {
        when (intent) {
            is PlacesIntent.QueryChanged -> {
                reduce { copy(query = intent.query) }
                queryFlow.value = intent.query
            }

            PlacesIntent.ClearQuery -> {
                reduce { copy(query = "", suggestions = emptyList(), searchError = null, isSearching = false) }
                queryFlow.value = ""
            }

            is PlacesIntent.SuggestionClicked -> add(intent.place)

            is PlacesIntent.LocationClicked -> viewModelScope.launch {
                selectLocation(intent.locationId)
                emitEffect(PlacesEffect.NavigateBack)
            }

            is PlacesIntent.RemoveLocation -> viewModelScope.launch { removeLocation(intent.locationId) }

            is PlacesIntent.ReorderLocations -> viewModelScope.launch { reorderLocations(intent.orderedIds) }

            PlacesIntent.UseDeviceLocationClicked ->
                if (syncDeviceLocation.hasPermission()) locate() else emitEffect(PlacesEffect.RequestLocationPermission)

            is PlacesIntent.LocationPermissionResult ->
                if (intent.granted) {
                    locate()
                } else {
                    emitEffect(PlacesEffect.ShowMessage(LocationError.PermissionDenied.toUiText()))
                }

            PlacesIntent.BackClicked -> emitEffect(PlacesEffect.NavigateBack)
        }
    }

    private suspend fun search(query: String) {
        if (query.length < PlacesState.MIN_QUERY_LENGTH) {
            reduce { copy(isSearching = false, suggestions = emptyList(), searchError = null) }
            return
        }
        reduce { copy(isSearching = true, searchError = null) }
        when (val result = searchPlaces(query, Locale.getDefault().language)) {
            is Outcome.Success -> reduce { copy(isSearching = false, suggestions = result.data) }

            is Outcome.Failure -> reduce {
                copy(isSearching = false, suggestions = emptyList(), searchError = result.error.toUiText())
            }
        }
    }

    private fun add(place: PlaceSuggestion) {
        if (place.externalId in currentState.addingIds) return
        viewModelScope.launch {
            reduce { copy(addingIds = addingIds + place.externalId) }
            addPlace(place)
            reduce { copy(addingIds = addingIds - place.externalId) }
            emitEffect(PlacesEffect.NavigateBack)
        }
    }

    private fun locate() {
        if (currentState.isLocating) return
        viewModelScope.launch {
            reduce { copy(isLocating = true) }
            val result = syncDeviceLocation(select = true)
            reduce { copy(isLocating = false) }
            when (result) {
                is Outcome.Success -> emitEffect(PlacesEffect.NavigateBack)
                is Outcome.Failure -> emitEffect(PlacesEffect.ShowMessage(result.error.toUiText()))
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
