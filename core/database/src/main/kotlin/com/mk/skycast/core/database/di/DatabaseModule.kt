package com.mk.skycast.core.database.di

import android.content.Context
import androidx.room.Room
import com.mk.skycast.core.database.SkycastDatabase
import com.mk.skycast.core.database.dao.LocationDao
import com.mk.skycast.core.database.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkycastDatabase =
        Room.databaseBuilder(context, SkycastDatabase::class.java, "skycast.db").build()

    @Provides
    fun provideLocationDao(db: SkycastDatabase): LocationDao = db.locationDao()

    @Provides
    fun provideWeatherDao(db: SkycastDatabase): WeatherDao = db.weatherDao()
}
