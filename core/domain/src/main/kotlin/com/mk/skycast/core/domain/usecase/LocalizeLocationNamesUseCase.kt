package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.repository.DeviceLocationProvider
import com.mk.skycast.core.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * Keeps saved location names in the language the UI is shown in. Names are stored in
 * the language used when a place was saved, so switching the app language would
 * otherwise leave e.g. "القاهرة" on an English screen.
 */
class LocalizeLocationNamesUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val deviceLocationProvider: DeviceLocationProvider,
) {
    suspend operator fun invoke(languageCode: String) {
        locationRepository.localizeNames(languageCode)

        val device = locationRepository.getSavedLocations().firstOrNull { it.isDeviceLocation } ?: return
        if (device.nameLanguage == languageCode || !deviceLocationProvider.hasPermission()) return
        val fix = deviceLocationProvider.currentLocation(languageCode)
        if (fix is Outcome.Success) locationRepository.saveDeviceLocation(fix.data)
    }
}
