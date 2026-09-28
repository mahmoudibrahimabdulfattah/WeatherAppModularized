package com.mk.skycast.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mk.skycast.core.domain.brief.BriefRun
import com.mk.skycast.core.domain.brief.PrepareBriefNotificationUseCase
import com.mk.skycast.core.domain.repository.WidgetRefresher
import com.mk.skycast.core.domain.usecase.ObserveDayOverridesUseCase
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import kotlinx.coroutines.flow.first

/** Builds the brief, notifies if warranted, then plans its next run. */
@HiltWorker
class BriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prepareBriefNotification: PrepareBriefNotificationUseCase,
    private val observeRoutine: ObserveRoutineUseCase,
    private val observeDayOverrides: ObserveDayOverridesUseCase,
    private val notifier: BriefNotifier,
    private val scheduler: WorkManagerBriefScheduler,
    private val widgetRefresher: WidgetRefresher,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val run = inputData.getString(KEY_RUN)?.let { name -> BriefRun.entries.firstOrNull { it.name == name } }
            ?: BriefRun.NIGHTLY
        try {
            prepareBriefNotification(run, clock.instant())?.let { notifier.show(it) }
        } finally {
            widgetRefresher.refresh()
            scheduler.scheduleNext(observeRoutine().first(), observeDayOverrides().first(), clock.instant())
        }
        return Result.success()
    }

    companion object {
        const val KEY_RUN = "run"
    }
}
