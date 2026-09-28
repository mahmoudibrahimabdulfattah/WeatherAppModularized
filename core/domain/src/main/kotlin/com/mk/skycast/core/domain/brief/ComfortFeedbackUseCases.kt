package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.domain.repository.ComfortRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.model.ComfortVote
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The day to ask "how did it feel?" about: today, once the user's last time out
 * is over, if they went out and haven't answered yet. Null otherwise.
 */
class ObserveComfortPromptUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val comfortRepository: ComfortRepository,
    private val ticker: TimeTicker,
) {
    operator fun invoke(zone: ZoneId = ZoneId.systemDefault()): Flow<LocalDate?> = combine(
        routineRepository.routine,
        routineRepository.overrides,
        comfortRepository.feedback.map { list -> list.map { it.date }.toSet() },
        ticker.ticks,
    ) { routine, overrides, answered, now ->
        if (!routine.isConfigured) return@combine null
        val today = now.atZone(zone).toLocalDate()
        if (today in answered) return@combine null
        val lastOut = ExposurePlanner.plan(routine, overrides.firstOrNull { it.date == today }, today, zone)
            .maxOfOrNull { it.end } ?: return@combine null
        today.takeIf { now >= lastOut && lastOut.atZone(zone).toLocalDate() == today }
    }.distinctUntilChanged()
}

class RecordComfortVoteUseCase @Inject constructor(private val repository: ComfortRepository) {
    suspend operator fun invoke(date: LocalDate, vote: ComfortVote) = repository.record(date, vote)
}

class ResetComfortCalibrationUseCase @Inject constructor(private val repository: ComfortRepository) {
    suspend operator fun invoke() = repository.reset()
}
