package com.mk.skycast.core.model

/** A place returned by a search, not yet saved by the user. */
data class PlaceSuggestion(
    val externalId: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val countryCode: String?,
    val point: GeoPoint,
    val timezone: String?,
)

/** A location the user follows. */
data class SavedLocation(
    val id: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val countryCode: String?,
    val point: GeoPoint,
    val timezone: String?,
    val isDeviceLocation: Boolean,
    val sortOrder: Int,
    /** Language the names are in; null when unknown. Used to re-localize on language change. */
    val nameLanguage: String? = null,
)

/** Result of a device location fix, optionally reverse-geocoded. */
data class DeviceLocation(
    val point: GeoPoint,
    val name: String?,
    val region: String?,
    val country: String?,
    val countryCode: String?,
    /** Language the reverse-geocoded names are in. */
    val languageCode: String? = null,
)
