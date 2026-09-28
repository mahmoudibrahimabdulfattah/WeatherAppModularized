package com.mk.skycast

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mk.skycast.core.common.ApplicationScope
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.domain.repository.BriefScheduler
import com.mk.skycast.core.domain.repository.WeatherSyncScheduler
import com.mk.skycast.core.domain.repository.WidgetRefresher
import com.mk.skycast.core.domain.usecase.ObserveDayOverridesUseCase
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import dagger.hilt.android.HiltAndroidApp
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltAndroidApp
class SkycastApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var syncScheduler: WeatherSyncScheduler

    @Inject lateinit var briefScheduler: BriefScheduler

    @Inject lateinit var observeRoutine: ObserveRoutineUseCase

    @Inject lateinit var observeDayOverrides: ObserveDayOverridesUseCase

    @Inject lateinit var clock: Clock

    @Inject lateinit var observeDailyBrief: ObserveDailyBriefUseCase

    @Inject lateinit var widgetRefresher: WidgetRefresher

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        installAppCheck()
        syncScheduler.schedulePeriodicSync()
        // Re-plan the brief whenever the routine or a day's plan changes (and on every start).
        combine(observeRoutine(), observeDayOverrides()) { routine, overrides ->
            briefScheduler.schedule(routine, overrides, clock.instant())
        }.launchIn(appScope)
        // Keep the home-screen widget in step with what the app shows.
        observeDailyBrief()
            .debounce(WIDGET_DEBOUNCE_MS)
            .onEach { widgetRefresher.refresh() }
            .launchIn(appScope)
    }

    private companion object {
        const val WIDGET_DEBOUNCE_MS = 2_000L
    }
}
