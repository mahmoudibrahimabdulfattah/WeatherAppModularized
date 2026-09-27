package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.model.PlaceSuggestion

interface PlaceSearchRepository {
    suspend fun search(query: String, languageCode: String): Outcome<List<PlaceSuggestion>, DataError>
}
