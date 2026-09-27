package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.model.DeviceLocation

interface DeviceLocationProvider {
    fun hasPermission(): Boolean

    /** Current position, reverse-geocoded in [languageCode] (device default when null). */
    suspend fun currentLocation(languageCode: String? = null): Outcome<DeviceLocation, LocationError>
}
