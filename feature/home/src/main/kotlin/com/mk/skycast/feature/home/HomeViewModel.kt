package com.mk.skycast.feature.home

import androidx.lifecycle.viewModelScope
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.domain.usecase.LocalizeLocationNamesUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.ObserveNetworkStatusUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.RefreshWeatherUseCase
import com.mk.skycast.core.domain.usecase.SelectLocationUseCase
import com.mk.skycast.core.domain.usecase.SyncDeviceLocationUseCase
import com.mk.skycast.core.model.LocationWeather
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.mvi.MviViewModel
import com.mk.skycast.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeLocationWeather: ObserveLocationWeatherUseCase,
    observePreferences: ObserveUserPreferencesUseCase,
    private val observeNetworkStatus: ObserveNetworkStatusUseCase,
    private val refreshWeather: RefreshWeatherUseCase,
    private val selectLocation: SelectLocationUseCase,
    private val syncDeviceLocation: SyncDeviceLocationUseCase,
    private val localizeLocationNames: LocalizeLocationNamesUseCase,
    private val clock: Clock,
    ticker: TimeTicker,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    private var autoRefreshJob: Job? = null
    private var hasAttemptedAutoLocate = false

    init {
        combine(
            observeLocationWeather(),
            observePreferences(),
            ticker.ticks,
        ) { locations, preferences, now ->
            val pages = locations.map { it.toPage(now) }
            val selectedIndex = pages.indexOfFirst { it.location.id == preferences.selectedLocationId }
                .takeIf { it >= 0 } ?: 0
            reduce {
                copy(
                    isLoading = false,
                    pages = pages,
                    selectedIndex = selectedIndex,
                    preferences = preferences,
                    now = now,
                )
            }
            if (pages.isEmpty()) maybeAutoLocate()
        }.launchIn(viewModelScope)

        observeNetworkStatus()
            .onEach { online ->
                val cameBackOnline = online && currentState.isOffline
                reduce { copy(isOffline = !online) }
                if (cameBackOnline) refreshSelected(force = false, userInitiated = false)
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ScreenResumed -> startAutoRefresh()
            HomeIntent.ScreenPaused -> stopAutoRefresh()
            is HomeIntent.DisplayLanguageChanged -> viewModelScope.launch { localizeLocationNames(intent.languageCode) }
            HomeIntent.Refresh -> refreshSelected(force = true, userInitiated = true)
            is HomeIntent.PageChanged -> onPageChanged(intent.index)
            HomeIntent.UseDeviceLocationClicked -> onUseDeviceLocation()
            is HomeIntent.LocationPermissionResult -> onPermissionResult(intent.granted)
            HomeIntent.OpenPlacesClicked -> emitEffect(HomeEffect.NavigateToPlaces)
            HomeIntent.OpenSettingsClicked -> emitEffect(HomeEffect.NavigateToSettings)
        }
    }

    /** While visible: refresh now if stale, then keep polling in step with the API update cadence. */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            refreshDeviceLocationIfPresent()
            while (isActive) {
                refreshSelected(force = false, userInitiated = false)
                delay(AUTO_REFRESH_INTERVAL.toMillis())
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    private fun onPageChanged(index: Int) {
        val page = currentState.pages.getOrNull(index) ?: return
        if (index == currentState.selectedIndex) return
        reduce { copy(selectedIndex = index) }
        viewModelScope.launch {
            selectLocation(page.location.id)
            refresh(page.location.id, force = false, userInitiated = false)
        }
    }

    private fun refreshSelected(force: Boolean, userInitiated: Boolean) {
        val id = currentState.selectedPage?.location?.id ?: return
        viewModelScope.launch { refresh(id, force, userInitiated) }
    }

    private suspend fun refresh(locationId: Long, force: Boolean, userInitiated: Boolean) {
        if (userInitiated) reduce { copy(isRefreshing = true) }
        when (val result = refreshWeather(locationId, force)) {
            is Outcome.Success -> reduce { copy(pageErrors = pageErrors - locationId) }

            is Outcome.Failure -> {
                val message = result.error.toUiText()
                reduce { copy(pageErrors = pageErrors + (locationId to message)) }
                if (userInitiated) emitEffect(HomeEffect.ShowMessage(message))
            }
        }
        if (userInitiated) reduce { copy(isRefreshing = false) }
    }

    private fun onUseDeviceLocation() {
        if (syncDeviceLocation.hasPermission()) locate() else emitEffect(HomeEffect.RequestLocationPermission)
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            locate()
        } else {
            emitEffect(HomeEffect.ShowMessage(com.mk.skycast.core.common.LocationError.PermissionDenied.toUiText()))
        }
    }

    private fun locate() {
        if (currentState.isLocating) return
        viewModelScope.launch {
            reduce { copy(isLocating = true) }
            val result = syncDeviceLocation(select = true)
            reduce { copy(isLocating = false) }
            if (result is Outcome.Failure) emitEffect(HomeEffect.ShowMessage(result.error.toUiText()))
        }
    }

    /** First launch with permission already granted: show local weather without asking. */
    private fun maybeAutoLocate() {
        if (hasAttemptedAutoLocate || !syncDeviceLocation.hasPermission()) return
        hasAttemptedAutoLocate = true
        locate()
    }

    /** Keeps "my location" accurate when the user travels. */
    private suspend fun refreshDeviceLocationIfPresent() {
        val device = currentState.pages.firstOrNull { it.location.isDeviceLocation } ?: return
        val fetchedAt = device.weather?.fetchedAt
        val stale = fetchedAt == null || Duration.between(fetchedAt, clock.instant()) > STALE_AFTER
        if (stale && syncDeviceLocation.hasPermission()) syncDeviceLocation(select = false)
    }

    private fun LocationWeather.toPage(now: Instant): WeatherPage = WeatherPage(
        location = location,
        weather = weather,
        upcomingHours = weather?.upcomingHours(now).orEmpty(),
        days = weather?.upcomingDays(now).orEmpty(),
        isStale = weather?.let { Duration.between(it.fetchedAt, now) > STALE_AFTER } ?: true,
    )

    private fun Weather.upcomingHours(now: Instant) =
        hourly.filter { !it.time.isBefore(now.truncatedTo(ChronoUnit.HOURS)) }.take(HOURS_SHOWN)

    private fun Weather.upcomingDays(now: Instant) =
        daily.filter { !it.date.isBefore(now.atZone(zoneId).toLocalDate()) }

    companion object {
        val AUTO_REFRESH_INTERVAL: Duration = Duration.ofMinutes(15)
        val STALE_AFTER: Duration = Duration.ofMinutes(30)
        const val HOURS_SHOWN = 24
    }
}
