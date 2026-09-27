package com.mk.skycast.feature.routine.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.mvi.CollectEffects
import com.mk.skycast.feature.routine.RoutineEffect
import com.mk.skycast.feature.routine.RoutineIntent
import com.mk.skycast.feature.routine.RoutineViewModel

@Composable
fun RoutineRoute(onClose: () -> Unit, viewModel: RoutineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            RoutineEffect.Close -> onClose()
        }
    }
    // System back walks the steps backwards, like the top-bar arrow.
    BackHandler { viewModel.onIntent(RoutineIntent.BackClicked) }

    RoutineScreen(state = state, onIntent = viewModel::onIntent)
}
