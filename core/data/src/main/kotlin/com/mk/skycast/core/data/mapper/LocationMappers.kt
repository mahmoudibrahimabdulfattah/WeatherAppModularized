package com.mk.skycast.core.data.mapper

import com.mk.skycast.core.database.entity.LocationEntity
import com.mk.skycast.core.model.GeoPoint
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.network.model.NetworkPlace

internal fun LocationEntity.toDomain() = SavedLocation(
    id = id,
    name = name,
    region = region,
    country = country,
    countryCode = countryCode,
    point = GeoPoint(latitude, longitude),
    timezone = timezone,
    isDeviceLocation = isDeviceLocation,
    sortOrder = sortOrder,
    nameLanguage = nameLanguage,
)

internal fun NetworkPlace.toDomain() = PlaceSuggestion(
    externalId = id,
    name = name,
    region = admin1,
    country = country,
    countryCode = countryCode,
    point = GeoPoint(latitude, longitude),
    timezone = timezone,
)
