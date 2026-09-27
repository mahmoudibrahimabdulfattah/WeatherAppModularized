package com.mk.skycast.feature.routine.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.mvi.CollectEffects
import com.mk.skycast.feature.routine.RoutineEffect
import com.mk.skycast.feature.routine.RoutineIntent
import com.mk.skycast.feature.routine.RoutineViewModel

@Composable
fun RoutineRoute(onClose: () -> Unit, viewModel: RoutineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The nightly brief is the reason to allow notifications; ask right after the user sets it up.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onClose() }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            RoutineEffect.Close -> onClose()

            RoutineEffect.Saved -> {
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    notificationPermission.launch(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                } else {
                    onClose()
                }
            }
        }
    }
    // System back walks the steps backwards, like the top-bar arrow.
    BackHandler { viewModel.onIntent(RoutineIntent.BackClicked) }

    RoutineScreen(state = state, onIntent = viewModel::onIntent)
}
