package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.repository.DeviceLocationProvider
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import javax.inject.Inject

/** Resolves the device position, stores it as "my location" and refreshes its weather. */
class SyncDeviceLocationUseCase @Inject constructor(
    private val deviceLocationProvider: DeviceLocationProvider,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: UserPreferencesRepository,
) {
    fun hasPermission(): Boolean = deviceLocationProvider.hasPermission()

    suspend operator fun invoke(select: Boolean = true): Outcome<Long, LocationError> =
        when (val result = deviceLocationProvider.currentLocation()) {
            is Outcome.Failure -> result

            is Outcome.Success -> {
                val id = locationRepository.saveDeviceLocation(result.data)
                if (select) preferencesRepository.setSelectedLocationId(id)
                when (val refresh = weatherRepository.refresh(id, force = true)) {
                    is Outcome.Failure -> Outcome.Failure(LocationError.Data(refresh.error))
                    is Outcome.Success -> Outcome.Success(id)
                }
            }
        }
}
