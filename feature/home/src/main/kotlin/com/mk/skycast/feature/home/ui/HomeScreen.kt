package com.mk.skycast.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mk.skycast.core.designsystem.components.SkySystemBars
import com.mk.skycast.core.designsystem.components.rememberReducedMotion
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.ui.brief.rememberBriefText
import com.mk.skycast.core.ui.format.rememberWeatherFormatter
import com.mk.skycast.core.ui.weather.WeatherBackdrop
import com.mk.skycast.feature.home.HomeIntent
import com.mk.skycast.feature.home.HomeState
import java.time.ZoneId

private const val SKY_CROSSFADE_MS = 900

/** Stateless home screen: an animated sky with one swipeable page per saved location. */
@Composable
fun HomeScreen(
    state: HomeState,
    snackbarHostState: SnackbarHostState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkySystemBars()
    val formatter = rememberWeatherFormatter(state.preferences)
    val reducedMotion = rememberReducedMotion()
    val current = state.selectedPage?.weather?.current
    val sky = current?.let { it.condition to it.isDay } ?: (WeatherCondition.CLEAR to false)

    Box(modifier.fillMaxSize()) {
        Crossfade(
            targetState = sky,
            animationSpec = tween(if (reducedMotion) 0 else SKY_CROSSFADE_MS),
            label = "sky",
        ) { (condition, isDay) ->
            WeatherBackdrop(condition, isDay, Modifier.fillMaxSize())
        }
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = SkyColors.OnSky,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                HomeHeader(
                    location = state.selectedPage?.location,
                    onSearchClick = { onIntent(HomeIntent.OpenPlacesClicked) },
                    onSettingsClick = { onIntent(HomeIntent.OpenSettingsClicked) },
                )
                AnimatedVisibility(visible = state.isOffline) { OfflineBanner() }
                when {
                    state.isLoading -> WeatherSkeleton(Modifier.weight(1f))

                    state.isEmpty -> HomeWelcome(
                        isLocating = state.isLocating,
                        onUseLocation = { onIntent(HomeIntent.UseDeviceLocationClicked) },
                        onSearch = { onIntent(HomeIntent.OpenPlacesClicked) },
                        modifier = Modifier.weight(1f),
                    )

                    else -> LocationPager(
                        state = state,
                        formatter = formatter,
                        onIntent = onIntent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    state.planEditor?.let { editor ->
        val zone = state.pages.firstOrNull { it.location.id == state.briefLocationId }?.weather?.zoneId
            ?: ZoneId.systemDefault()
        PlanSheet(editor, formatter, rememberBriefText(formatter, zone), onIntent)
    }
}
