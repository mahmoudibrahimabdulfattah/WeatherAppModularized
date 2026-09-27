package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.SavedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * The live brief for the routine's location: today's until noon, tomorrow's after.
 * Emits null until the routine is set up or the forecast is available.
 */
class ObserveDailyBriefUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val ticker: TimeTicker,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DailyBrief?> = combine(
        routineRepository.routine,
        routineRepository.overrides,
        locationRepository.observeSavedLocations(),
    ) { routine, overrides, locations -> Triple(routine, overrides, routineLocation(routine, locations)) }
        .flatMapLatest { (routine, overrides, location) ->
            if (!routine.isConfigured || location == null) return@flatMapLatest flowOf(null)
            combine(weatherRepository.observeWeather(location.id), ticker.ticks) { weather, now ->
                weather ?: return@combine null
                val date = briefDate(now, weather.zoneId)
                BriefEngine.build(date, location.id, routine, overrides.firstOrNull { it.date == date }, weather)
            }
        }
        .distinctUntilChanged()

    companion object {
        private val SWITCH_TO_TOMORROW: LocalTime = LocalTime.NOON

        /** Mornings are about the day ahead; from noon on, the user is planning tomorrow. */
        fun briefDate(now: Instant, zone: ZoneId): LocalDate {
            val local = now.atZone(zone)
            return if (local.toLocalTime() <
                SWITCH_TO_TOMORROW
            ) {
                local.toLocalDate()
            } else {
                local.toLocalDate().plusDays(1)
            }
        }

        /** The routine's own location, else the device location, else the first saved place. */
        fun routineLocation(routine: Routine, locations: List<SavedLocation>): SavedLocation? =
            locations.firstOrNull { it.id == routine.locationId }
                ?: locations.firstOrNull { it.isDeviceLocation }
                ?: locations.minByOrNull { it.sortOrder }
    }
}
