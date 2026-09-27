package com.mk.skycast.sync.work.di

import com.mk.skycast.core.domain.repository.BriefScheduler
import com.mk.skycast.core.domain.repository.WeatherSyncScheduler
import com.mk.skycast.sync.work.WorkManagerBriefScheduler
import com.mk.skycast.sync.work.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SyncModule {
    @Binds
    abstract fun bindScheduler(impl: WorkManagerSyncScheduler): WeatherSyncScheduler

    @Binds
    abstract fun bindBriefScheduler(impl: WorkManagerBriefScheduler): BriefScheduler
}
