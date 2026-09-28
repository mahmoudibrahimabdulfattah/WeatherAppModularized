package com.mk.skycast.feature.settings.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mk.skycast.core.mvi.CollectEffects
import com.mk.skycast.feature.settings.R
import com.mk.skycast.feature.settings.SettingsEffect
import com.mk.skycast.feature.settings.SettingsViewModel

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenRoutine: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.NavigateBack -> onBack()

            SettingsEffect.OpenRoutine -> onOpenRoutine()

            SettingsEffect.ComfortReset ->
                Toast.makeText(context, R.string.settings_comfort_reset_done, Toast.LENGTH_SHORT).show()

            is SettingsEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }

    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}
