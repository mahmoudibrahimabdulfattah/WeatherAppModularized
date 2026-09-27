package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveRoutineUseCase @Inject constructor(private val repository: RoutineRepository) {
    operator fun invoke() = repository.routine.distinctUntilChanged()
}

class ObserveDayOverridesUseCase @Inject constructor(private val repository: RoutineRepository) {
    operator fun invoke() = repository.overrides.distinctUntilChanged()
}

class SaveRoutineUseCase @Inject constructor(private val repository: RoutineRepository) {
    suspend operator fun invoke(routine: Routine) = repository.save(routine)
}

class SetDayOverrideUseCase @Inject constructor(private val repository: RoutineRepository) {
    suspend operator fun invoke(override: DayPlanOverride) = repository.setOverride(override)
}

class ClearDayOverrideUseCase @Inject constructor(private val repository: RoutineRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.clearOverride(date)
}
