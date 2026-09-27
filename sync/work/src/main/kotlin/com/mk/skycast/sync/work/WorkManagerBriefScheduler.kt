package com.mk.skycast.sync.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mk.skycast.core.domain.brief.BriefRun
import com.mk.skycast.core.domain.brief.BriefSchedule
import com.mk.skycast.core.domain.repository.BriefScheduler
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.Routine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * One-off work per run, re-planned after every run and whenever the routine changes.
 * WorkManager timing is approximate (Doze), which is fine for a brief.
 */
class WorkManagerBriefScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    BriefScheduler {

    override fun schedule(routine: Routine, overrides: List<DayPlanOverride>, now: Instant) =
        plan(routine, overrides, now, ExistingWorkPolicy.REPLACE)

    /** From inside a running worker: append so the running work is not cancelled. */
    fun scheduleNext(routine: Routine, overrides: List<DayPlanOverride>, now: Instant) =
        plan(routine, overrides, now, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun plan(routine: Routine, overrides: List<DayPlanOverride>, now: Instant, policy: ExistingWorkPolicy) {
        val workManager = WorkManager.getInstance(context)
        if (!routine.isConfigured) {
            workManager.cancelUniqueWork(NIGHTLY_WORK)
            workManager.cancelUniqueWork(MORNING_WORK)
            return
        }
        val zone = ZoneId.systemDefault()
        enqueue(NIGHTLY_WORK, BriefRun.NIGHTLY, BriefSchedule.nextNightly(routine, now, zone), now, policy)
        val morning = BriefSchedule.nextMorningCheck(routine, overrides, now, zone)
        if (morning == null) {
            workManager.cancelUniqueWork(MORNING_WORK)
        } else {
            enqueue(MORNING_WORK, BriefRun.MORNING, morning, now, policy)
        }
    }

    private fun enqueue(name: String, run: BriefRun, at: Instant, now: Instant, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<BriefWorker>()
            .setInitialDelay(Duration.between(now, at).coerceAtLeast(Duration.ZERO))
            .setInputData(workDataOf(BriefWorker.KEY_RUN to run.name))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, policy, request)
    }

    private companion object {
        const val NIGHTLY_WORK = "daily_brief_nightly"
        const val MORNING_WORK = "daily_brief_morning"
    }
}
