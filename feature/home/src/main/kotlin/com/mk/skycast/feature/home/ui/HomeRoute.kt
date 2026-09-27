package com.mk.skycast.feature.home.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.mvi.CollectEffects
import com.mk.skycast.feature.home.HomeEffect
import com.mk.skycast.feature.home.HomeIntent
import com.mk.skycast.feature.home.HomeViewModel

/** Stateful entry point: wires the ViewModel, lifecycle and effects to the stateless [HomeScreen]. */
@Composable
fun HomeRoute(onOpenPlaces: () -> Unit, onOpenSettings: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onIntent(HomeIntent.LocationPermissionResult(granted)) }

    val languageCode = LocalConfiguration.current.locales[0].language
    LaunchedEffect(languageCode) { viewModel.onIntent(HomeIntent.DisplayLanguageChanged(languageCode)) }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(HomeIntent.ScreenResumed)
        onPauseOrDispose { viewModel.onIntent(HomeIntent.ScreenPaused) }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            HomeEffect.NavigateToPlaces -> onOpenPlaces()

            HomeEffect.NavigateToSettings -> onOpenSettings()

            HomeEffect.RequestLocationPermission ->
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

            is HomeEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
        }
    }

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}
