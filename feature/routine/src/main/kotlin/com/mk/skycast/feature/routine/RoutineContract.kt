package com.mk.skycast.feature.routine

import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState
import java.time.DayOfWeek
import java.time.LocalTime

enum class RoutineStep { WEEK, TRIPS, BRIEF }

data class RoutineState(
    val isLoading: Boolean = true,
    val step: RoutineStep = RoutineStep.WEEK,
    /** Edited copy; only persisted on the final "Save". */
    val draft: Routine = Routine(),
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    /** Outing open in the editor sheet, or null when the sheet is closed. */
    val outingEditor: Outing? = null,
    val showCommuteModeError: Boolean = false,
) : UiState {
    /** The usual trip only matters when at least one day is spent at work/study. */
    val showsCommute: Boolean get() = draft.week.values.any { it == DayType.AWAY }
    val isEditingExisting: Boolean get() = draft.isConfigured
    val isLastStep: Boolean get() = step == RoutineStep.entries.last()
    val canSaveOuting: Boolean
        get() = outingEditor?.let {
            it.days.isNotEmpty() && (it.kind != OutingKind.CUSTOM || !it.customLabel.isNullOrBlank())
        } ?: false
}

sealed interface RoutineIntent : UiIntent {
    data class DayTypeChanged(val day: DayOfWeek, val type: DayType) : RoutineIntent
    data class LeaveHomeChanged(val time: LocalTime) : RoutineIntent
    data class LeaveWorkChanged(val time: LocalTime) : RoutineIntent
    data class ReturnsNextDayChanged(val enabled: Boolean) : RoutineIntent
    data class TravelMinutesChanged(val minutes: Int) : RoutineIntent
    data class TravelModeChanged(val mode: TravelMode) : RoutineIntent

    data object AddOutingClicked : RoutineIntent
    data class EditOutingClicked(val id: String) : RoutineIntent
    data class OutingDraftChanged(val outing: Outing) : RoutineIntent
    data object SaveOutingClicked : RoutineIntent
    data class DeleteOutingClicked(val id: String) : RoutineIntent
    data object DismissOutingEditor : RoutineIntent

    data class BriefTimeChanged(val time: LocalTime) : RoutineIntent
    data class MorningRefreshChanged(val enabled: Boolean) : RoutineIntent

    data object NextClicked : RoutineIntent
    data object BackClicked : RoutineIntent
    data object SkipClicked : RoutineIntent
}

sealed interface RoutineEffect : UiEffect {
    data object Close : RoutineEffect
}
