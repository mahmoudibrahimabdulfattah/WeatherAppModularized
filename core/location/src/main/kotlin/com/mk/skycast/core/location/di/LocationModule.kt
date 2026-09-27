package com.mk.skycast.core.location.di

import com.mk.skycast.core.domain.repository.DeviceLocationProvider
import com.mk.skycast.core.location.AndroidDeviceLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindDeviceLocationProvider(impl: AndroidDeviceLocationProvider): DeviceLocationProvider
}
