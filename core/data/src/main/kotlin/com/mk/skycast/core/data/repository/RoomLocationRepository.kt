package com.mk.skycast.core.data.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.asEmpty
import com.mk.skycast.core.common.onSuccess
import com.mk.skycast.core.data.mapper.toDomain
import com.mk.skycast.core.database.dao.LocationDao
import com.mk.skycast.core.database.entity.LocationEntity
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.model.DeviceLocation
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.network.WeatherNetworkDataSource
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomLocationRepository @Inject constructor(
    private val dao: LocationDao,
    private val network: WeatherNetworkDataSource,
) : LocationRepository {

    override fun observeSavedLocations(): Flow<List<SavedLocation>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    override suspend fun getSavedLocations(): List<SavedLocation> = dao.getAll().map { it.toDomain() }

    override suspend fun save(place: PlaceSuggestion): Long {
        dao.getByExternalId(place.externalId)?.let { return it.id }
        return dao.insert(
            LocationEntity(
                externalId = place.externalId,
                name = place.name,
                region = place.region,
                country = place.country,
                countryCode = place.countryCode,
                latitude = place.point.latitude,
                longitude = place.point.longitude,
                timezone = place.timezone,
                isDeviceLocation = false,
                sortOrder = dao.maxSortOrder() + 1,
            ),
        )
    }

    override suspend fun saveDeviceLocation(location: DeviceLocation): Long {
        val existing = dao.getDeviceLocation()
        val entity = LocationEntity(
            id = existing?.id ?: 0,
            externalId = null,
            name = location.name ?: existing?.name.orEmpty(),
            region = location.region,
            country = location.country,
            countryCode = location.countryCode,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            timezone = existing?.timezone,
            isDeviceLocation = true,
            sortOrder = -1,
            nameLanguage = location.languageCode,
        )
        return if (existing == null) dao.insert(entity) else dao.upsert(entity).let { existing.id }
    }

    override suspend fun remove(locationId: Long) = dao.delete(locationId)

    override suspend fun reorder(orderedIds: List<Long>) {
        val remaining = dao.getAll().map { it.id }.filterNot { it in orderedIds }
        dao.reorder(orderedIds + remaining)
    }

    override suspend fun localizeNames(languageCode: String): Outcome<Unit, DataError> = coroutineScope {
        val results = dao.getNotLocalizedTo(languageCode)
            .mapNotNull { entity -> entity.externalId?.let { entity.id to it } }
            .map { (id, externalId) ->
                async {
                    network.place(externalId, languageCode).onSuccess { place ->
                        dao.updateNames(id, place.name, place.admin1, place.country, languageCode)
                    }
                }
            }
            .awaitAll()
        results.firstOrNull { it is Outcome.Failure }?.asEmpty() ?: Outcome.Success(Unit)
    }
}
