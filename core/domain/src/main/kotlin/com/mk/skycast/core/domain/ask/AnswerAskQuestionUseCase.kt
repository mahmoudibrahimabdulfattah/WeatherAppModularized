package com.mk.skycast.core.domain.ask

import com.mk.skycast.core.domain.brief.ComfortCalibration
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.domain.repository.ComfortRepository
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.SavedLocation
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class AnswerAskQuestionUseCase @Inject constructor(
    private val routines: RoutineRepository,
    private val locations: LocationRepository,
    private val weather: WeatherRepository,
    private val preferences: UserPreferencesRepository,
    private val comfort: ComfortRepository,
) {
    suspend operator fun invoke(question: AskQuestion, exercise: ExerciseKind? = null, now: Instant): AskAnswer? {
        val routine = routines.routine.first()
        val savedLocations = locations.observeSavedLocations().first()
        val location = askLocation(routine, savedLocations) ?: return null
        val forecast = weather.observeWeather(location.id).first() ?: return null
        return when (question) {
            AskQuestion.WHAT_TO_WEAR -> {
                val overrideDate = ObserveDailyBriefUseCase.briefDate(now, forecast.zoneId)
                AskEngine.wear(
                    routine = routine,
                    override = routines.overrides.first().firstOrNull { it.date == overrideDate },
                    weather = forecast,
                    now = now,
                    comfortOffsetC = ComfortCalibration.offsetC(comfort.feedback.first()),
                )
            }

            AskQuestion.BEST_EXERCISE_TIME -> AskEngine.exercise(exercise ?: ExerciseKind.WALK, forecast, now)

            AskQuestion.AVOID_HEAT -> AskEngine.avoidHeat(forecast, now)

            AskQuestion.LAUNDRY -> AskEngine.laundry(forecast, now)

            AskQuestion.RAIN_NEXT_DAYS -> AskEngine.rain(forecast, now)

            AskQuestion.AIR_QUALITY -> AskEngine.air(forecast, now)
        }
    }

    private suspend fun askLocation(routine: Routine, locations: List<SavedLocation>): SavedLocation? =
        if (routine.isConfigured) {
            ObserveDailyBriefUseCase.routineLocation(routine, locations)
        } else {
            val selected = preferences.preferences.first().selectedLocationId
            locations.firstOrNull { it.id == selected }
                ?: locations.firstOrNull { it.isDeviceLocation }
                ?: locations.minByOrNull { it.sortOrder }
        }
}
