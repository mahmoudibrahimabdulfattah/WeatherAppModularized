package com.mk.skycast.feature.home

import com.mk.skycast.core.domain.ai.AiWording
import com.mk.skycast.core.domain.ai.RephraseRequest
import com.mk.skycast.core.domain.ask.AskAnswer
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.domain.ask.DayHint
import com.mk.skycast.core.domain.ask.ExerciseKind
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.model.ComfortVote
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.mvi.UiEffect
import com.mk.skycast.core.mvi.UiIntent
import com.mk.skycast.core.mvi.UiState
import com.mk.skycast.core.ui.text.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeState(
    val isLoading: Boolean = true,
    val pages: List<WeatherPage> = emptyList(),
    val selectedIndex: Int = 0,
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val isOffline: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    /** Ticks every minute so relative labels ("updated 3 min ago", local clock) stay live. */
    val now: Instant = Instant.EPOCH,
    /** Last refresh error per location id, shown when that page has no cached data. */
    val pageErrors: Map<Long, UiText> = emptyMap(),
    val routine: Routine? = null,
    val overrides: List<DayPlanOverride> = emptyList(),
    /** Deterministic brief for the routine's location; null until the routine is set up. */
    val brief: DailyBrief? = null,
    val isBriefExpanded: Boolean = false,
    /** Day to ask "how did it feel?" about, once the user is back home. */
    val comfortPromptDate: LocalDate? = null,
    /** Open "plans changed?" sheet, or null. */
    val planEditor: PlanEditor? = null,
    /** Open Ask Skycast sheet, or null. */
    val askSheet: AskSheetState? = null,
) : UiState {
    val showRoutinePrompt: Boolean get() = routine?.isConfigured == false

    /** Page that hosts the brief card (or the "set up your routine" prompt). */
    val briefLocationId: Long?
        get() = brief?.locationId
            ?: routine?.let { ObserveDailyBriefUseCase.routineLocation(it, pages.map(WeatherPage::location)) }?.id
    val isEmpty: Boolean get() = !isLoading && pages.isEmpty()
    val selectedPage: WeatherPage? get() = pages.getOrNull(selectedIndex)
}

data class AskSheetState(
    val location: SavedLocation,
    val zoneId: ZoneId,
    val question: AskQuestion? = null,
    val exercise: ExerciseKind = ExerciseKind.WALK,
    val answer: AskAnswer? = null,
    /** Optional AI wording for [answer]; null until requested. */
    val aiWording: AiWording? = null,
    /** What the user typed, if the current answer came from the question box. */
    val typedQuestion: String? = null,
    /** Day named in the typed question, narrowing the answer to it. */
    val dayHint: DayHint? = null,
    /** AI answer to a typed question no guided answer covers. */
    val freeAnswer: AiWording? = null,
)

/** One-day change of plans, edited as a draft until saved. */
data class PlanEditor(
    val date: LocalDate,
    /** The routine's plan for that day, shown as the default. */
    val usualDayType: DayType,
    val usualOutings: List<Outing>,
    val draft: DayPlanOverride,
    val hasSavedOverride: Boolean,
) {
    val dayType: DayType get() = draft.dayType ?: usualDayType
}

/** One swipeable page per saved location. */
data class WeatherPage(
    val location: SavedLocation,
    val weather: Weather?,
    /** Next 24 hours starting from the current hour. */
    val upcomingHours: List<HourlyForecast>,
    /** Today first, up to 10 days. */
    val days: List<DailyForecast>,
    /** True when the cache is older than [HomeViewModel.STALE_AFTER]. */
    val isStale: Boolean,
) {
    val today: DailyForecast? get() = days.firstOrNull()
}

sealed interface HomeIntent : UiIntent {
    data object ScreenResumed : HomeIntent
    data object ScreenPaused : HomeIntent

    /** The language the UI is currently rendered in; saved names are re-localized to it. */
    data class DisplayLanguageChanged(val languageCode: String) : HomeIntent
    data object Refresh : HomeIntent
    data class PageChanged(val index: Int) : HomeIntent
    data object UseDeviceLocationClicked : HomeIntent
    data class LocationPermissionResult(val granted: Boolean) : HomeIntent
    data object OpenPlacesClicked : HomeIntent
    data object OpenSettingsClicked : HomeIntent

    data object OpenRoutineClicked : HomeIntent
    data object BriefExpandToggled : HomeIntent
    data class ComfortVoted(val vote: ComfortVote) : HomeIntent
    data object PlansChangedClicked : HomeIntent
    data class AskOpened(val locationId: Long) : HomeIntent
    data class AskQuestionSelected(val question: AskQuestion) : HomeIntent
    data class AskExerciseSelected(val exercise: ExerciseKind) : HomeIntent
    data object AskDismissed : HomeIntent

    /** The sheet rendered an answer; ask for friendlier wording of exactly these facts. */
    data class AskWordingRequested(val request: RephraseRequest) : HomeIntent
    data class AskAiConsentGiven(val granted: Boolean) : HomeIntent
    data class AskTyped(val text: String, val languageTag: String) : HomeIntent

    /** From the notification: open the sheet as soon as the routine is loaded. */
    data object OpenPlansRequested : HomeIntent
    data class PlanDayTypeChanged(val dayType: DayType) : HomeIntent
    data class PlanOutingToggled(val outingId: String, val going: Boolean) : HomeIntent
    data object PlanAddOutingClicked : HomeIntent
    data class PlanAddedOutingChanged(val outing: Outing) : HomeIntent
    data class PlanAddedOutingRemoved(val outingId: String) : HomeIntent
    data object PlanSaveClicked : HomeIntent
    data object PlanResetClicked : HomeIntent
    data object PlanDismissed : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object NavigateToPlaces : HomeEffect
    data object NavigateToSettings : HomeEffect
    data object NavigateToRoutine : HomeEffect
    data object RequestLocationPermission : HomeEffect
    data class ShowMessage(val message: UiText) : HomeEffect
}
