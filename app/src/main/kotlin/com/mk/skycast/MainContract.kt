package com.mk.skycast

import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState

data class MainState(val isLoading: Boolean = true, val preferences: UserPreferences = UserPreferences()) : UiState

sealed interface MainIntent : UiIntent

sealed interface MainEffect : UiEffect
