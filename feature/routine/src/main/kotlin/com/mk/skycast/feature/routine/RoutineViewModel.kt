package com.mk.skycast.feature.routine

import androidx.lifecycle.viewModelScope
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.SaveRoutineUseCase
import com.mk.skycast.core.model.Commute
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class RoutineViewModel @Inject constructor(
    observeRoutine: ObserveRoutineUseCase,
    observePreferences: ObserveUserPreferencesUseCase,
    private val saveRoutine: SaveRoutineUseCase,
) : MviViewModel<RoutineState, RoutineIntent, RoutineEffect>(RoutineState()) {

    init {
        // The draft is loaded once; later edits stay local until the user saves.
        viewModelScope.launch {
            val routine = observeRoutine().first()
            reduce { copy(isLoading = false, draft = routine) }
        }
        observePreferences()
            .onEach { reduce { copy(timeFormat = it.timeFormat) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: RoutineIntent) {
        when (intent) {
            is RoutineIntent.DayTypeChanged -> editDraft { copy(week = week + (intent.day to intent.type)) }

            is RoutineIntent.LeaveHomeChanged -> editCommute { copy(leaveHome = intent.time) }

            is RoutineIntent.LeaveWorkChanged -> editCommute { copy(leaveWork = intent.time) }

            is RoutineIntent.ReturnsNextDayChanged -> editCommute { copy(returnsNextDay = intent.enabled) }

            is RoutineIntent.TravelMinutesChanged -> editCommute { copy(travelMinutes = intent.minutes) }

            is RoutineIntent.TravelModeChanged -> {
                editCommute { copy(mode = intent.mode) }
                reduce { copy(showCommuteModeError = false) }
            }

            RoutineIntent.AddOutingClicked -> reduce { copy(outingEditor = newOuting(draft)) }

            is RoutineIntent.EditOutingClicked ->
                reduce { copy(outingEditor = draft.outings.firstOrNull { it.id == intent.id }) }

            is RoutineIntent.OutingDraftChanged -> reduce { copy(outingEditor = intent.outing) }

            RoutineIntent.SaveOutingClicked -> saveOuting()

            is RoutineIntent.DeleteOutingClicked -> reduce {
                copy(draft = draft.copy(outings = draft.outings.filterNot { it.id == intent.id }), outingEditor = null)
            }

            RoutineIntent.DismissOutingEditor -> reduce { copy(outingEditor = null) }

            is RoutineIntent.BriefTimeChanged -> editDraft { copy(briefTime = intent.time) }

            is RoutineIntent.MorningRefreshChanged -> editDraft { copy(morningRefresh = intent.enabled) }

            RoutineIntent.NextClicked -> next()

            RoutineIntent.BackClicked -> back()

            RoutineIntent.SkipClicked -> emitEffect(RoutineEffect.Close)
        }
    }

    private fun next() {
        val current = state.value
        if (current.step == RoutineStep.TRIPS && current.showsCommute && current.draft.commute.mode == null) {
            reduce { copy(showCommuteModeError = true) }
            return
        }
        if (current.isLastStep) {
            val routine = current.draft.copy(isConfigured = true)
            viewModelScope.launch {
                saveRoutine(routine)
                emitEffect(RoutineEffect.Close)
            }
        } else {
            reduce { copy(step = RoutineStep.entries[step.ordinal + 1]) }
        }
    }

    private fun back() {
        val step = state.value.step
        if (step.ordinal == 0) {
            emitEffect(RoutineEffect.Close)
        } else {
            reduce { copy(step = RoutineStep.entries[step.ordinal - 1], showCommuteModeError = false) }
        }
    }

    private fun saveOuting() {
        val current = state.value
        val outing = current.outingEditor ?: return
        if (!current.canSaveOuting) return
        val outings = if (current.draft.outings.any { it.id == outing.id }) {
            current.draft.outings.map { if (it.id == outing.id) outing else it }
        } else {
            current.draft.outings + outing
        }
        reduce { copy(draft = draft.copy(outings = outings), outingEditor = null) }
    }

    private fun editDraft(transform: Routine.() -> Routine) = reduce { copy(draft = draft.transform()) }

    private fun editCommute(transform: Commute.() -> Commute) = editDraft { copy(commute = commute.transform()) }

    private fun newOuting(routine: Routine) = Outing(
        id = UUID.randomUUID().toString(),
        kind = OutingKind.OUTING,
        customLabel = null,
        days = emptySet(),
        departAt = LocalTime.of(DEFAULT_OUTING_DEPART_HOUR, 0),
        returnAt = LocalTime.of(DEFAULT_OUTING_RETURN_HOUR, 0),
        mode = routine.commute.mode ?: TravelMode.CAR,
        setting = OutingSetting.OUTDOORS,
    )

    private companion object {
        const val DEFAULT_OUTING_DEPART_HOUR = 19
        const val DEFAULT_OUTING_RETURN_HOUR = 22
    }
}
