package com.mk.skycast.feature.settings

import com.mk.skycast.core.domain.usecase.PreferenceUpdate
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState

data class SettingsState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** AI wording is only offered where the free Gemini tier may be used. */
    val aiSupported: Boolean = false,
    val aiEnabled: Boolean = false,
) : UiState

sealed interface SettingsIntent : UiIntent {
    /** Every setting change goes through one intent; applied instantly app-wide. */
    data class Update(val update: PreferenceUpdate) : SettingsIntent
    data object OpenDataSourceClicked : SettingsIntent
    data object OpenRoutineClicked : SettingsIntent
    data class AiWordingToggled(val enabled: Boolean) : SettingsIntent
    data object BackClicked : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
    data object OpenRoutine : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect
}
