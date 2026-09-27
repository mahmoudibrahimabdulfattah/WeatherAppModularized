package com.mk.skycast.feature.home

import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState
import com.mk.skycast.core.ui.text.UiText
import java.time.Instant

data class HomeState(
    val isLoading: Boolean = true,
    val pages: List<WeatherPage> = emptyList(),
    val selectedIndex: Int = 0,
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val isOffline: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    /** Ticks every minute so relative labels ("updated 3 min ago", local clock) stay live. */
    val now: Instant = Instant.EPOCH,
    /** Last refresh error per location id, shown when that page has no cached data. */
    val pageErrors: Map<Long, UiText> = emptyMap(),
) : UiState {
    val isEmpty: Boolean get() = !isLoading && pages.isEmpty()
    val selectedPage: WeatherPage? get() = pages.getOrNull(selectedIndex)
}

/** One swipeable page per saved location. */
data class WeatherPage(
    val location: SavedLocation,
    val weather: Weather?,
    /** Next 24 hours starting from the current hour. */
    val upcomingHours: List<HourlyForecast>,
    /** Today first, up to 10 days. */
    val days: List<DailyForecast>,
    /** True when the cache is older than [HomeViewModel.STALE_AFTER]. */
    val isStale: Boolean,
) {
    val today: DailyForecast? get() = days.firstOrNull()
}

sealed interface HomeIntent : UiIntent {
    data object ScreenResumed : HomeIntent
    data object ScreenPaused : HomeIntent

    /** The language the UI is currently rendered in; saved names are re-localized to it. */
    data class DisplayLanguageChanged(val languageCode: String) : HomeIntent
    data object Refresh : HomeIntent
    data class PageChanged(val index: Int) : HomeIntent
    data object UseDeviceLocationClicked : HomeIntent
    data class LocationPermissionResult(val granted: Boolean) : HomeIntent
    data object OpenPlacesClicked : HomeIntent
    data object OpenSettingsClicked : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object NavigateToPlaces : HomeEffect
    data object NavigateToSettings : HomeEffect
    data object RequestLocationPermission : HomeEffect
    data class ShowMessage(val message: UiText) : HomeEffect
}
