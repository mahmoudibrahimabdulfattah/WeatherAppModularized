package com.mk.skycast.feature.places.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.mvi.CollectEffects
import com.mk.skycast.feature.places.PlacesEffect
import com.mk.skycast.feature.places.PlacesIntent
import com.mk.skycast.feature.places.PlacesViewModel

@Composable
fun PlacesRoute(onBack: () -> Unit, viewModel: PlacesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onIntent(PlacesIntent.LocationPermissionResult(granted)) }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PlacesEffect.NavigateBack -> onBack()

            PlacesEffect.RequestLocationPermission ->
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

            is PlacesEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
        }
    }

    PlacesScreen(state = state, snackbarHostState = snackbarHostState, onIntent = viewModel::onIntent)
}
