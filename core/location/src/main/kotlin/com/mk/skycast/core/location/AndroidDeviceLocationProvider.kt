package com.mk.skycast.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.mk.skycast.core.common.Dispatcher
import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.SkycastDispatchers
import com.mk.skycast.core.domain.repository.DeviceLocationProvider
import com.mk.skycast.core.model.DeviceLocation
import com.mk.skycast.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Uses the platform [LocationManager] (network provider, coarse accuracy) so the app
 * works without Google Play Services, and [Geocoder] for a human-readable name.
 */
internal class AndroidDeviceLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(SkycastDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : DeviceLocationProvider {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(languageCode: String?): Outcome<DeviceLocation, LocationError> {
        if (!hasPermission()) return Outcome.Failure(LocationError.PermissionDenied)
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            return Outcome.Failure(LocationError.ProviderDisabled)
        }
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { locationManager.isProviderEnabled(it) }
            ?: return Outcome.Failure(LocationError.ProviderDisabled)

        val location = withTimeoutOrNull(FIX_TIMEOUT_MS) { freshLocation(provider) }
            ?: locationManager.getLastKnownLocation(provider)
            ?: return Outcome.Failure(LocationError.Unavailable)

        val point = GeoPoint(location.latitude, location.longitude)
        val locale = languageCode?.let(Locale::forLanguageTag) ?: Locale.getDefault()
        val address = reverseGeocode(point, locale)
        return Outcome.Success(
            DeviceLocation(
                point = point,
                name = address?.locality ?: address?.subAdminArea ?: address?.adminArea,
                region = address?.adminArea,
                country = address?.countryName,
                countryCode = address?.countryCode,
                languageCode = locale.language.takeIf { address != null },
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(provider: String): Location? = suspendCancellableCoroutine { cont ->
        val signal = androidx.core.os.CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            signal,
            Executors.newSingleThreadExecutor(),
        ) { cont.resume(it) }
    }

    private suspend fun reverseGeocode(point: GeoPoint, locale: Locale): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, locale)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(
                            point.latitude,
                            point.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) =
                                    cont.resume(addresses.firstOrNull())
                                override fun onError(errorMessage: String?) = cont.resume(null)
                            },
                        )
                    }
                }
            } else {
                withContext(ioDispatcher) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
                }
            }
        }.getOrNull()
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 10_000L
        const val GEOCODE_TIMEOUT_MS = 5_000L
    }
}
