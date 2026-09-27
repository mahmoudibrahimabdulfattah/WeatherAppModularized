package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    val routine: Flow<Routine>
    val overrides: Flow<List<DayPlanOverride>>

    suspend fun save(routine: Routine)
    suspend fun setOverride(override: DayPlanOverride)
    suspend fun clearOverride(date: LocalDate)
}
