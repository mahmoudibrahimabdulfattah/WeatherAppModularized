package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.domain.repository.BriefHistoryRepository
import com.mk.skycast.core.domain.repository.ComfortRepository
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.BriefFingerprint
import com.mk.skycast.core.model.CarryItem
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.DrivingRisk
import com.mk.skycast.core.model.Hazard
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class BriefRun {
    /** The evening brief about tomorrow; always delivered. */
    NIGHTLY,

    /** Morning re-check of today; delivered only if what to carry changed. */
    MORNING,
}

data class BriefNotice(val brief: DailyBrief, val isUpdate: Boolean)

/** Refreshes the forecast and decides what, if anything, to notify. */
class PrepareBriefNotificationUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val history: BriefHistoryRepository,
    private val comfortRepository: ComfortRepository,
) {
    suspend operator fun invoke(run: BriefRun, now: Instant): BriefNotice? {
        val routine = routineRepository.routine.first()
        if (!routine.isConfigured) return null
        val location = ObserveDailyBriefUseCase.routineLocation(routine, locationRepository.getSavedLocations())
            ?: return null
        weatherRepository.refresh(location.id) // Best effort: a cached forecast is still useful offline.
        val weather = weatherRepository.observeWeather(location.id).first() ?: return null
        val today = now.atZone(weather.zoneId).toLocalDate()
        val date = if (run == BriefRun.NIGHTLY) ObserveDailyBriefUseCase.briefDate(now, weather.zoneId) else today
        val override = routineRepository.overrides.first().firstOrNull { it.date == date }
        val comfortOffset = ComfortCalibration.offsetC(comfortRepository.feedback.first())
        val brief = BriefEngine.build(date, location.id, routine, override, weather, comfortOffset) ?: return null
        val fingerprint = fingerprint(brief)

        return when (run) {
            BriefRun.NIGHTLY -> {
                history.saveNotified(fingerprint)
                BriefNotice(brief, isUpdate = false)
            }

            BriefRun.MORNING -> {
                val previous = history.lastNotified()
                val stillAhead = brief.windows.any { it.window.end > now }
                if (previous?.date != date || previous == fingerprint || !stillAhead) return null
                history.saveNotified(fingerprint)
                BriefNotice(brief, isUpdate = true)
            }
        }
    }

    companion object {
        private val ACTION_ITEMS = setOf(CarryItem.UMBRELLA, CarryItem.RAINCOAT, CarryItem.EXTRA_LAYER, CarryItem.MASK)
        private val ACTION_HAZARDS = setOf(
            Hazard.THUNDERSTORM,
            Hazard.HEAVY_RAIN,
            Hazard.FOG,
            Hazard.DUST,
            Hazard.EXTREME_HEAT,
            Hazard.STRONG_WIND,
        )
        private val ACTION_DRIVING = setOf(
            DrivingRisk.FLOODED_STREETS,
            DrivingRisk.LOW_VISIBILITY,
            DrivingRisk.DUST_VISIBILITY,
            DrivingRisk.SLIPPERY_ROAD,
        )

        fun fingerprint(brief: DailyBrief) = BriefFingerprint(
            date = brief.date,
            keys = brief.carry.map { it.item }.filter { it in ACTION_ITEMS }.map { "carry:${it.name}" }.toSet() +
                brief.hazards.map { it.hazard }.filter { it in ACTION_HAZARDS }.map { "hazard:${it.name}" } +
                brief.driving.map { it.risk }.filter { it in ACTION_DRIVING }.map { "drive:${it.name}" },
        )
    }
}
