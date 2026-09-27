package com.mk.skycast.core.domain

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.usecase.AddPlaceUseCase
import com.mk.skycast.core.domain.usecase.LocalizeLocationNamesUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.RefreshAllWeatherUseCase
import com.mk.skycast.core.domain.usecase.RemoveLocationUseCase
import com.mk.skycast.core.domain.usecase.SearchPlacesUseCase
import com.mk.skycast.core.domain.usecase.SyncDeviceLocationUseCase
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.testing.FakeDeviceLocationProvider
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakePlaceSearchRepository
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UseCasesTest {

    private val locations = FakeLocationRepository()
    private val weather = FakeWeatherRepository()
    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `observe location weather pairs every location with its weather and reacts to updates`() = runTest {
        locations.locations.value = listOf(TestData.location(1, "Cairo"), TestData.location(2, "Giza", sortOrder = 1))
        val useCase = ObserveLocationWeatherUseCase(locations, weather)

        useCase().test {
            val initial = awaitItem()
            assertThat(initial.map { it.location.name }).containsExactly("Cairo", "Giza").inOrder()
            assertThat(initial.all { it.weather == null }).isTrue()

            weather.emit(TestData.weather(locationId = 2, temperatureC = 31.0))
            val updated = awaitItem()
            assertThat(updated[1].weather?.current?.temperatureC).isEqualTo(31.0)
        }
    }

    @Test
    fun `observe location weather emits empty list when nothing is saved`() = runTest {
        ObserveLocationWeatherUseCase(locations, weather)().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `refresh all returns the first failure`() = runTest {
        locations.locations.value = listOf(TestData.location(1), TestData.location(2))
        weather.nextRefreshResult = Outcome.Failure(DataError.NoInternet)

        val result = RefreshAllWeatherUseCase(locations, weather)()

        assertThat(result).isEqualTo(Outcome.Failure(DataError.NoInternet))
        assertThat(weather.refreshCalls.map { it.first }).containsExactly(1L, 2L)
    }

    @Test
    fun `search ignores too short queries`() = runTest {
        val repo = FakePlaceSearchRepository()
        val result = SearchPlacesUseCase(repo)(" c ", "en")

        assertThat(result).isEqualTo(Outcome.Success(emptyList<Any>()))
        assertThat(repo.queries).isEmpty()
    }

    @Test
    fun `search trims query`() = runTest {
        val repo = FakePlaceSearchRepository()
        SearchPlacesUseCase(repo)("  Cairo ", "en")
        assertThat(repo.queries).containsExactly("Cairo")
    }

    @Test
    fun `add place saves, selects and refreshes`() = runTest {
        val id = AddPlaceUseCase(locations, weather, preferences)(TestData.place())

        assertThat(locations.locations.value.single().id).isEqualTo(id)
        assertThat(preferences.current.selectedLocationId).isEqualTo(id)
        assertThat(weather.refreshCalls).containsExactly(id to false)
    }

    @Test
    fun `removing the selected location selects the next one`() = runTest {
        locations.locations.value = listOf(TestData.location(1), TestData.location(2, sortOrder = 1))
        val prefs = FakeUserPreferencesRepository(UserPreferences(selectedLocationId = 1))

        RemoveLocationUseCase(locations, prefs)(1)

        assertThat(prefs.current.selectedLocationId).isEqualTo(2)
    }

    @Test
    fun `sync device location fails without permission`() = runTest {
        val provider = FakeDeviceLocationProvider().apply { permissionGranted = false }
        val result = SyncDeviceLocationUseCase(provider, locations, weather, preferences)()

        assertThat(result).isEqualTo(Outcome.Failure(LocationError.PermissionDenied))
        assertThat(locations.locations.value).isEmpty()
    }

    @Test
    fun `sync device location saves, selects and force refreshes`() = runTest {
        val result = SyncDeviceLocationUseCase(FakeDeviceLocationProvider(), locations, weather, preferences)()

        val id = (result as Outcome.Success).data
        assertThat(locations.locations.value.single().isDeviceLocation).isTrue()
        assertThat(preferences.current.selectedLocationId).isEqualTo(id)
        assertThat(weather.refreshCalls).containsExactly(id to true)
    }

    @Test
    fun `localize renames searched places and re-geocodes my location in the new language`() = runTest {
        val device = FakeDeviceLocationProvider()
        locations.locations.value = listOf(
            TestData.location(1, "القاهرة"),
            TestData.location(2, "الجيزة", isDevice = true).copy(nameLanguage = "ar"),
        )

        LocalizeLocationNamesUseCase(locations, device)("en")

        val byId = locations.locations.value.associateBy { it.id }
        assertThat(byId.getValue(1).name).isEqualTo("القاهرة [en]")
        assertThat(byId.getValue(2).nameLanguage).isEqualTo("en")
        assertThat(device.requestedLanguages).containsExactly("en")
    }

    @Test
    fun `localize skips my location when already in that language`() = runTest {
        val device = FakeDeviceLocationProvider()
        locations.locations.value = listOf(TestData.location(2, "Giza", isDevice = true).copy(nameLanguage = "en"))

        LocalizeLocationNamesUseCase(locations, device)("en")

        assertThat(device.requestedLanguages).isEmpty()
    }
}
