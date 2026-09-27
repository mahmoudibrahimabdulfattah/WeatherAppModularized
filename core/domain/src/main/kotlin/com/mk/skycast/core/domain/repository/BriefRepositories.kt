package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.model.BriefFingerprint
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import java.time.Instant

/** Remembers the last brief the user was notified about. */
interface BriefHistoryRepository {
    suspend fun lastNotified(): BriefFingerprint?
    suspend fun saveNotified(fingerprint: BriefFingerprint)
}

/** Schedules the nightly brief (and the optional morning check) on the device. */
interface BriefScheduler {
    fun schedule(routine: Routine, overrides: List<DayPlanOverride>, now: Instant)
}
