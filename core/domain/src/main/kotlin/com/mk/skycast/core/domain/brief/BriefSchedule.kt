package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** When the nightly brief and the optional morning re-check should run. */
object BriefSchedule {
    private val MORNING_LEAD: Duration = Duration.ofMinutes(90)
    private val EARLIEST_MORNING: LocalTime = LocalTime.of(5, 0)

    fun nextNightly(routine: Routine, now: Instant, zone: ZoneId): Instant {
        val local = now.atZone(zone)
        val today = local.toLocalDate().atTime(routine.briefTime).atZone(zone).toInstant()
        return if (today >
            now
        ) {
            today
        } else {
            local.toLocalDate().plusDays(1).atTime(routine.briefTime).atZone(zone).toInstant()
        }
    }

    /**
     * 90 minutes before the first time out (not before 05:00), on the next day that
     * has one. Null when disabled, or when the brief itself is sent in the morning.
     */
    fun nextMorningCheck(routine: Routine, overrides: List<DayPlanOverride>, now: Instant, zone: ZoneId): Instant? {
        if (!routine.morningRefresh || routine.briefTime < LocalTime.NOON) return null
        val today = now.atZone(zone).toLocalDate()
        return listOf(today, today.plusDays(1))
            .mapNotNull { morningCheck(routine, overrides, it, zone) }
            .firstOrNull { it > now }
    }

    private fun morningCheck(
        routine: Routine,
        overrides: List<DayPlanOverride>,
        date: LocalDate,
        zone: ZoneId,
    ): Instant? {
        val first = ExposurePlanner.plan(routine, overrides.firstOrNull { it.date == date }, date, zone).firstOrNull()
            ?: return null
        val earliest = date.atTime(EARLIEST_MORNING).atZone(zone).toInstant()
        return maxOf(first.start - MORNING_LEAD, earliest)
    }
}
