package com.mk.skycast

import androidx.lifecycle.viewModelScope
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** App-wide state: theme preferences drive the whole UI and apply instantly. */
@HiltViewModel
class MainViewModel @Inject constructor(observePreferences: ObserveUserPreferencesUseCase) :
    MviViewModel<MainState, MainIntent, MainEffect>(MainState()) {

    init {
        observePreferences()
            .onEach { reduce { copy(isLoading = false, preferences = it) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: MainIntent) = Unit
}
