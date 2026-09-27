package com.mk.skycast.feature.settings

import androidx.lifecycle.viewModelScope
import com.mk.skycast.core.domain.ai.AiConsent
import com.mk.skycast.core.domain.ai.ObserveAiAvailabilityUseCase
import com.mk.skycast.core.domain.ai.SetAiConsentUseCase
import com.mk.skycast.core.domain.usecase.GetAppLanguageUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.PreferenceUpdate
import com.mk.skycast.core.domain.usecase.UpdateUserPreferenceUseCase
import com.mk.skycast.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observePreferences: ObserveUserPreferencesUseCase,
    getAppLanguage: GetAppLanguageUseCase,
    private val updatePreference: UpdateUserPreferenceUseCase,
    observeAiAvailability: ObserveAiAvailabilityUseCase,
    private val setAiConsent: SetAiConsentUseCase,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState(language = getAppLanguage())) {

    init {
        observePreferences()
            .onEach { reduce { copy(isLoading = false, preferences = it) } }
            .launchIn(viewModelScope)
        observeAiAvailability()
            .onEach { reduce { copy(aiSupported = it.supported, aiEnabled = it.consent == AiConsent.GRANTED) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.Update -> {
                val update = intent.update
                if (update is PreferenceUpdate.Language) reduce { copy(language = update.language) }
                viewModelScope.launch { updatePreference(update) }
            }

            SettingsIntent.OpenDataSourceClicked -> emitEffect(SettingsEffect.OpenUrl(DATA_SOURCE_URL))

            SettingsIntent.OpenRoutineClicked -> emitEffect(SettingsEffect.OpenRoutine)

            // Turning it back on asks again (with the age check) on the next answer.
            is SettingsIntent.AiWordingToggled -> viewModelScope.launch {
                setAiConsent(if (intent.enabled) AiConsent.UNKNOWN else AiConsent.DECLINED)
            }

            SettingsIntent.BackClicked -> emitEffect(SettingsEffect.NavigateBack)
        }
    }

    companion object {
        const val DATA_SOURCE_URL = "https://open-meteo.com/"
    }
}
