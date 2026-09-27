package com.mk.skycast.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class DayType {
    AWAY,
    HOME,
    OFF,
}

enum class TravelMode {
    CAR,
    PUBLIC_TRANSPORT,
    WALK,
    BICYCLE,
    MOTORBIKE,
}

enum class OutingSetting {
    INDOORS,
    OUTDOORS,
}

enum class OutingKind {
    GYM,
    OUTING,
    FAMILY,
    PRAYER,
    CUSTOM,
}

data class Commute(
    val leaveHome: LocalTime = LocalTime.of(8, 0),
    val leaveWork: LocalTime = LocalTime.of(17, 0),
    val returnsNextDay: Boolean = false,
    val travelMinutes: Int = 30,
    val mode: TravelMode? = null,
)

data class Outing(
    val id: String,
    val kind: OutingKind,
    val customLabel: String?,
    val days: Set<DayOfWeek>,
    val departAt: LocalTime,
    val returnAt: LocalTime,
    val mode: TravelMode,
    val setting: OutingSetting,
)

data class Routine(
    val isConfigured: Boolean = false,
    val week: Map<DayOfWeek, DayType> = defaultWeek(),
    val commute: Commute = Commute(),
    val outings: List<Outing> = emptyList(),
    val briefTime: LocalTime = LocalTime.of(21, 0),
    val morningRefresh: Boolean = false,
    val locationId: Long? = null,
)

data class DayPlanOverride(
    val date: LocalDate,
    val dayType: DayType? = null,
    val addedOutings: List<Outing> = emptyList(),
    val cancelledOutingIds: Set<String> = emptySet(),
)

fun defaultWeek(): Map<DayOfWeek, DayType> = DayOfWeek.entries.associateWith { day ->
    when (day) {
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        -> DayType.OFF

        else -> DayType.AWAY
    }
}
