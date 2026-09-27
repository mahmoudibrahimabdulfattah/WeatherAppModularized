package com.mk.skycast.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Marker for an immutable screen state. */
interface UiState

/** Marker for a user/system action sent to a ViewModel. */
interface UiIntent

/** Marker for a one-shot side effect (navigation, snackbar, permission request...). */
interface UiEffect

/**
 * Unidirectional data flow:
 * UI --[Intent]--> ViewModel --reduce--> [State] --> UI
 *                            \--> [Effect] (consumed once)
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Single entry point for everything the UI wants to do. */
    abstract fun onIntent(intent: I)

    protected fun reduce(reducer: S.() -> S) = _state.update(reducer)

    protected fun emitEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
