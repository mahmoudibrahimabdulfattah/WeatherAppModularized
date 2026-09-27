package com.mk.skycast.core.data.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.map
import com.mk.skycast.core.data.mapper.toDomain
import com.mk.skycast.core.domain.repository.PlaceSearchRepository
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.network.WeatherNetworkDataSource
import javax.inject.Inject

internal class OpenMeteoPlaceSearchRepository @Inject constructor(private val network: WeatherNetworkDataSource) :
    PlaceSearchRepository {

    override suspend fun search(query: String, languageCode: String): Outcome<List<PlaceSuggestion>, DataError> =
        network.searchPlaces(query, languageCode).map { places -> places.map { it.toDomain() } }
}
