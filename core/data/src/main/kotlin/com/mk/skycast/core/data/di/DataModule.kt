package com.mk.skycast.core.data.di

import com.mk.skycast.core.common.ApplicationScope
import com.mk.skycast.core.common.Dispatcher
import com.mk.skycast.core.common.MinuteTicker
import com.mk.skycast.core.common.SkycastDispatchers
import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.data.repository.AppCompatLanguageRepository
import com.mk.skycast.core.data.repository.DataStoreBriefHistoryRepository
import com.mk.skycast.core.data.repository.DataStoreRoutineRepository
import com.mk.skycast.core.data.repository.DataStoreUserPreferencesRepository
import com.mk.skycast.core.data.repository.OfflineFirstWeatherRepository
import com.mk.skycast.core.data.repository.OpenMeteoPlaceSearchRepository
import com.mk.skycast.core.data.repository.RoomLocationRepository
import com.mk.skycast.core.data.util.ConnectivityNetworkMonitor
import com.mk.skycast.core.domain.repository.AppLanguageRepository
import com.mk.skycast.core.domain.repository.BriefHistoryRepository
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.NetworkMonitor
import com.mk.skycast.core.domain.repository.PlaceSearchRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindWeatherRepository(impl: OfflineFirstWeatherRepository): WeatherRepository

    @Binds
    abstract fun bindLocationRepository(impl: RoomLocationRepository): LocationRepository

    @Binds
    abstract fun bindPlaceSearchRepository(impl: OpenMeteoPlaceSearchRepository): PlaceSearchRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindRoutineRepository(impl: DataStoreRoutineRepository): RoutineRepository

    @Binds
    abstract fun bindBriefHistoryRepository(impl: DataStoreBriefHistoryRepository): BriefHistoryRepository

    @Binds
    abstract fun bindAppLanguageRepository(impl: AppCompatLanguageRepository): AppLanguageRepository

    @Binds
    abstract fun bindTimeTicker(impl: MinuteTicker): TimeTicker

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Dispatcher(SkycastDispatchers.IO)
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Dispatcher(SkycastDispatchers.Default)
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(
            @Dispatcher(SkycastDispatchers.Default) dispatcher: CoroutineDispatcher,
        ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
