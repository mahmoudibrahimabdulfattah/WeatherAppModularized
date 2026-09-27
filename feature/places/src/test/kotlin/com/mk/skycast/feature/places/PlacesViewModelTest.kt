package com.mk.skycast.feature.places

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.usecase.AddPlaceUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.RemoveLocationUseCase
import com.mk.skycast.core.domain.usecase.ReorderLocationsUseCase
import com.mk.skycast.core.domain.usecase.SearchPlacesUseCase
import com.mk.skycast.core.domain.usecase.SelectLocationUseCase
import com.mk.skycast.core.domain.usecase.SyncDeviceLocationUseCase
import com.mk.skycast.core.testing.FakeDeviceLocationProvider
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakePlaceSearchRepository
import com.mk.skycast.core.testing.FakeTimeTicker
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.MainDispatcherRule
import com.mk.skycast.core.testing.TestData
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PlacesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locations = FakeLocationRepository()
    private val weather = FakeWeatherRepository()
    private val preferences = FakeUserPreferencesRepository()
    private val search = FakePlaceSearchRepository()
    private val deviceLocation = FakeDeviceLocationProvider()

    private fun createViewModel() = PlacesViewModel(
        observeLocationWeather = ObserveLocationWeatherUseCase(locations, weather),
        observePreferences = ObserveUserPreferencesUseCase(preferences),
        searchPlaces = SearchPlacesUseCase(search),
        addPlace = AddPlaceUseCase(locations, weather, preferences),
        removeLocation = RemoveLocationUseCase(locations, preferences),
        reorderLocations = ReorderLocationsUseCase(locations),
        selectLocation = SelectLocationUseCase(preferences),
        syncDeviceLocation = SyncDeviceLocationUseCase(deviceLocation, locations, weather, preferences),
        ticker = FakeTimeTicker(),
    )

    @Test
    fun `typing is debounced into a single search`() = runTest {
        val vm = createViewModel()

        vm.onIntent(PlacesIntent.QueryChanged("Ca"))
        vm.onIntent(PlacesIntent.QueryChanged("Cai"))
        vm.onIntent(PlacesIntent.QueryChanged("Cairo"))
        advanceTimeBy(PlacesViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertThat(search.queries).containsExactly("Cairo")
        assertThat(vm.state.value.suggestions.single().name).isEqualTo("Cairo")
        assertThat(vm.state.value.isSearching).isFalse()
    }

    @Test
    fun `search failure is exposed in state`() = runTest {
        search.result = Outcome.Failure(DataError.NoInternet)
        val vm = createViewModel()

        vm.onIntent(PlacesIntent.QueryChanged("Cairo"))
        advanceTimeBy(PlacesViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertThat(vm.state.value.searchError).isNotNull()
        assertThat(vm.state.value.showNoResults).isFalse()
    }

    @Test
    fun `picking a suggestion saves it, selects it and navigates back`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.onIntent(PlacesIntent.SuggestionClicked(TestData.place()))
            assertThat(awaitItem()).isEqualTo(PlacesEffect.NavigateBack)
        }
        val saved = locations.locations.value.single()
        assertThat(preferences.current.selectedLocationId).isEqualTo(saved.id)
        assertThat(vm.state.value.savedLocations.single().weather).isNotNull()
    }

    @Test
    fun `clear query resets search state`() = runTest {
        val vm = createViewModel()
        vm.onIntent(PlacesIntent.QueryChanged("Cairo"))
        advanceTimeBy(PlacesViewModel.SEARCH_DEBOUNCE_MS + 1)

        vm.onIntent(PlacesIntent.ClearQuery)

        assertThat(vm.state.value.query).isEmpty()
        assertThat(vm.state.value.suggestions).isEmpty()
        assertThat(vm.state.value.isSearchActive).isFalse()
    }

    @Test
    fun `remove deletes the location`() = runTest {
        locations.locations.value = listOf(TestData.location(1))
        val vm = createViewModel()

        vm.onIntent(PlacesIntent.RemoveLocation(1))

        assertThat(vm.state.value.savedLocations).isEmpty()
    }
}
