package com.mk.skycast

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mk.skycast.core.common.ApplicationScope
import com.mk.skycast.core.domain.repository.BriefScheduler
import com.mk.skycast.core.domain.repository.WeatherSyncScheduler
import com.mk.skycast.core.domain.usecase.ObserveDayOverridesUseCase
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import dagger.hilt.android.HiltAndroidApp
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

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

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        installAppCheck()
        syncScheduler.schedulePeriodicSync()
        // Re-plan the brief whenever the routine or a day's plan changes (and on every start).
        combine(observeRoutine(), observeDayOverrides()) { routine, overrides ->
            briefScheduler.schedule(routine, overrides, clock.instant())
        }.launchIn(appScope)
    }
}
