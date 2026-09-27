package com.mk.skycast.core.data.repository

import com.mk.skycast.core.datastore.RoutineDataSource
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreRoutineRepository @Inject constructor(private val dataSource: RoutineDataSource) :
    RoutineRepository {
    override val routine: Flow<Routine> = dataSource.routine
    override val overrides: Flow<List<DayPlanOverride>> = dataSource.overrides

    override suspend fun save(routine: Routine) = dataSource.save(routine)
    override suspend fun setOverride(override: DayPlanOverride) = dataSource.setOverride(override)
    override suspend fun clearOverride(date: LocalDate) = dataSource.clearOverride(date)
}
