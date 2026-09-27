package com.mk.skycast.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.ask.AnswerAskQuestionUseCase
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.domain.usecase.ClearDayOverrideUseCase
import com.mk.skycast.core.domain.usecase.LocalizeLocationNamesUseCase
import com.mk.skycast.core.domain.usecase.ObserveDayOverridesUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.ObserveNetworkStatusUseCase
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.RefreshWeatherUseCase
import com.mk.skycast.core.domain.usecase.SelectLocationUseCase
import com.mk.skycast.core.domain.usecase.SetDayOverrideUseCase
import com.mk.skycast.core.domain.usecase.SyncDeviceLocationUseCase
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.testing.FakeDeviceLocationProvider
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakeNetworkMonitor
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeTimeTicker
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.MainDispatcherRule
import com.mk.skycast.core.testing.TestData
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(TestData.NOW, ZoneOffset.UTC)
    private val weather = FakeWeatherRepository()
    private val network = FakeNetworkMonitor()
    private val deviceLocation = FakeDeviceLocationProvider()
    private val routines = FakeRoutineRepository()
    private val ticker = FakeTimeTicker()
    private lateinit var locations: FakeLocationRepository
    private lateinit var preferences: FakeUserPreferencesRepository

    private fun createViewModel(
        saved: List<SavedLocation> = emptyList(),
        prefs: UserPreferences = UserPreferences(),
    ): HomeViewModel {
        locations = FakeLocationRepository(saved)
        preferences = FakeUserPreferencesRepository(prefs)
        return HomeViewModel(
            observeLocationWeather = ObserveLocationWeatherUseCase(locations, weather),
            observePreferences = ObserveUserPreferencesUseCase(preferences),
            observeNetworkStatus = ObserveNetworkStatusUseCase(network),
            refreshWeather = RefreshWeatherUseCase(weather),
            selectLocation = SelectLocationUseCase(preferences),
            syncDeviceLocation = SyncDeviceLocationUseCase(deviceLocation, locations, weather, preferences),
            localizeLocationNames = LocalizeLocationNamesUseCase(locations, deviceLocation),
            observeDailyBrief = ObserveDailyBriefUseCase(routines, locations, weather, ticker),
            observeRoutine = ObserveRoutineUseCase(routines),
            observeDayOverrides = ObserveDayOverridesUseCase(routines),
            answerAskQuestion = AnswerAskQuestionUseCase(routines, locations, weather, preferences),
            setDayOverride = SetDayOverrideUseCase(routines),
            clearDayOverride = ClearDayOverrideUseCase(routines),
            clock = clock,
            ticker = ticker,
        )
    }

    @Test
    fun `builds one page per saved location and selects the preferred one`() = runTest {
        weather.emit(TestData.weather(locationId = 1))
        weather.emit(TestData.weather(locationId = 2))
        val vm = createViewModel(
            saved = listOf(TestData.location(1, "Cairo"), TestData.location(2, "Alexandria", sortOrder = 1)),
            prefs = UserPreferences(selectedLocationId = 2),
        )

        val state = vm.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.pages.map { it.location.name }).containsExactly("Cairo", "Alexandria").inOrder()
        assertThat(state.selectedIndex).isEqualTo(1)
        assertThat(state.now).isEqualTo(TestData.NOW)
    }

    @Test
    fun `upcoming hours start at the current hour and are capped`() = runTest {
        weather.emit(TestData.weather(locationId = 1))
        val vm = createViewModel(saved = listOf(TestData.location(1)))

        val hours = vm.state.value.pages.single().upcomingHours
        assertThat(hours).hasSize(HomeViewModel.HOURS_SHOWN)
        assertThat(hours.first().time).isEqualTo(TestData.NOW.minusSeconds(15 * 60))
    }

    @Test
    fun `old cache is flagged as stale`() = runTest {
        weather.emit(TestData.weather(locationId = 1, fetchedAt = TestData.NOW.minus(Duration.ofHours(2))))
        val vm = createViewModel(saved = listOf(TestData.location(1)))

        assertThat(vm.state.value.pages.single().isStale).isTrue()
    }

    @Test
    fun `empty without permission shows onboarding and does not locate`() = runTest {
        deviceLocation.permissionGranted = false
        val vm = createViewModel()

        assertThat(vm.state.value.isEmpty).isTrue()
        assertThat(locations.locations.value).isEmpty()
    }

    @Test
    fun `empty with permission auto locates the device`() = runTest {
        val vm = createViewModel()

        val page = vm.state.value.pages.single()
        assertThat(page.location.isDeviceLocation).isTrue()
        assertThat(page.weather).isNotNull()
        assertThat(preferences.current.selectedLocationId).isEqualTo(page.location.id)
    }

    @Test
    fun `manual refresh failure surfaces a message and a page error`() = runTest {
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        weather.nextRefreshResult = Outcome.Failure(DataError.NoInternet)

        vm.effects.test {
            vm.onIntent(HomeIntent.Refresh)
            assertThat(awaitItem()).isInstanceOf(HomeEffect.ShowMessage::class.java)
        }
        assertThat(vm.state.value.isRefreshing).isFalse()
        assertThat(vm.state.value.pageErrors).containsKey(1L)
        assertThat(weather.refreshCalls.last()).isEqualTo(1L to true)
    }

    @Test
    fun `changing page persists the selection`() = runTest {
        val vm = createViewModel(
            saved = listOf(TestData.location(1), TestData.location(2, sortOrder = 1)),
        )

        vm.onIntent(HomeIntent.PageChanged(1))

        assertThat(preferences.current.selectedLocationId).isEqualTo(2)
        assertThat(vm.state.value.selectedIndex).isEqualTo(1)
    }

    @Test
    fun `use my location without permission requests it`() = runTest {
        deviceLocation.permissionGranted = false
        val vm = createViewModel(saved = listOf(TestData.location(1)))

        vm.effects.test {
            vm.onIntent(HomeIntent.UseDeviceLocationClicked)
            assertThat(awaitItem()).isEqualTo(HomeEffect.RequestLocationPermission)
        }
    }

    @Test
    fun `coming back online refreshes the selected location`() = runTest {
        network.online.value = false
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        assertThat(vm.state.value.isOffline).isTrue()

        network.online.value = true

        assertThat(vm.state.value.isOffline).isFalse()
        assertThat(weather.refreshCalls).contains(1L to false)
    }

    @Test
    fun `display language change re-localizes saved names`() = runTest {
        val vm = createViewModel(saved = listOf(TestData.location(1, "القاهرة")))

        vm.onIntent(HomeIntent.DisplayLanguageChanged("en"))

        assertThat(vm.state.value.pages.single().location.name).isEqualTo("القاهرة [en]")
    }

    @Test
    fun `navigation intents emit effects`() = runTest {
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        vm.effects.test {
            vm.onIntent(HomeIntent.OpenPlacesClicked)
            assertThat(awaitItem()).isEqualTo(HomeEffect.NavigateToPlaces)
            vm.onIntent(HomeIntent.OpenSettingsClicked)
            assertThat(awaitItem()).isEqualTo(HomeEffect.NavigateToSettings)
        }
    }

    @Test
    fun `routine prompt shows until the routine is set up, then the brief appears`() = runTest {
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        weather.emit(TestData.weather(1))

        assertThat(vm.state.value.showRoutinePrompt).isTrue()
        assertThat(vm.state.value.briefLocationId).isEqualTo(1)

        routines.save(Routine(isConfigured = true))

        assertThat(vm.state.value.showRoutinePrompt).isFalse()
        assertThat(vm.state.value.brief?.date).isEqualTo(LocalDate.of(2026, 9, 28))
    }

    @Test
    fun `plans changed saves a one-day override and reset clears it`() = runTest {
        routines.save(Routine(isConfigured = true))
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        weather.emit(TestData.weather(1))

        vm.onIntent(HomeIntent.PlansChangedClicked)
        val editor = vm.state.value.planEditor!!
        assertThat(editor.usualDayType).isEqualTo(DayType.AWAY) // Monday
        vm.onIntent(HomeIntent.PlanDayTypeChanged(DayType.HOME))
        vm.onIntent(HomeIntent.PlanAddOutingClicked)
        vm.onIntent(HomeIntent.PlanSaveClicked)

        val saved = routines.currentOverrides.single()
        assertThat(saved.date).isEqualTo(editor.date)
        assertThat(saved.dayType).isEqualTo(DayType.HOME)
        assertThat(saved.addedOutings).hasSize(1)
        assertThat(vm.state.value.planEditor).isNull()

        vm.onIntent(HomeIntent.PlansChangedClicked)
        assertThat(vm.state.value.planEditor?.hasSavedOverride).isTrue()
        vm.onIntent(HomeIntent.PlanResetClicked)
        assertThat(routines.currentOverrides).isEmpty()
    }

    @Test
    fun `choosing the usual day again is not saved as a change`() = runTest {
        routines.save(Routine(isConfigured = true))
        val vm = createViewModel(saved = listOf(TestData.location(1)))
        weather.emit(TestData.weather(1))

        vm.onIntent(HomeIntent.PlansChangedClicked)
        vm.onIntent(HomeIntent.PlanDayTypeChanged(DayType.AWAY))
        vm.onIntent(HomeIntent.PlanSaveClicked)

        assertThat(routines.currentOverrides).isEmpty()
    }

    @Test
    fun `opening ask and selecting a question stores an answer`() = runTest {
        weather.emit(TestData.weather(1))
        val vm = createViewModel(saved = listOf(TestData.location(1)))

        vm.onIntent(HomeIntent.AskOpened(1))
        vm.onIntent(HomeIntent.AskQuestionSelected(AskQuestion.RAIN_NEXT_DAYS))
        runCurrent()

        assertThat(vm.state.value.askSheet?.question).isEqualTo(AskQuestion.RAIN_NEXT_DAYS)
        assertThat(vm.state.value.askSheet?.answer).isNotNull()
    }
}
