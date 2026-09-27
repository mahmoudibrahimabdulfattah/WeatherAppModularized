package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.ExposureKind
import com.mk.skycast.core.model.ExposureWindow
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Turns a routine (plus a one-day override) into the concrete times the user is out. */
object ExposurePlanner {

    fun dayType(routine: Routine, override: DayPlanOverride?, date: LocalDate): DayType =
        override?.dayType ?: routine.week[date.dayOfWeek] ?: DayType.HOME

    fun plan(routine: Routine, override: DayPlanOverride?, date: LocalDate, zone: ZoneId): List<ExposureWindow> {
        val windows = mutableListOf<ExposureWindow>()
        if (dayType(routine, override, date) == DayType.AWAY) {
            val commute = routine.commute
            val mode = commute.mode ?: TravelMode.PUBLIC_TRANSPORT
            val travel = Duration.ofMinutes(commute.travelMinutes.toLong())
            val out = date.atTime(commute.leaveHome).atZone(zone).toInstant()
            windows += ExposureWindow(ExposureKind.COMMUTE_OUT, out, out + travel, mode, outdoors = true)
            val backDate = if (commute.returnsNextDay) date.plusDays(1) else date
            val back = backDate.atTime(commute.leaveWork).atZone(zone).toInstant()
            windows += ExposureWindow(ExposureKind.COMMUTE_BACK, back, back + travel, mode, outdoors = true)
        }
        val cancelled = override?.cancelledOutingIds.orEmpty()
        val outings = routine.outings.filter { date.dayOfWeek in it.days && it.id !in cancelled } +
            override?.addedOutings.orEmpty()
        outings.forEach { outing ->
            val start = date.atTime(outing.departAt).atZone(zone).toInstant()
            // A return time at or before departure means coming back after midnight.
            val endDate = if (outing.returnAt > outing.departAt) date else date.plusDays(1)
            val end = endDate.atTime(outing.returnAt).atZone(zone).toInstant()
            windows += ExposureWindow(
                kind = ExposureKind.OUTING,
                start = start,
                end = end,
                mode = outing.mode,
                outdoors = outing.setting == OutingSetting.OUTDOORS,
                outing = outing,
            )
        }
        return windows.sortedBy { it.start }
    }
}
