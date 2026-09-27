package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.repository.PlaceSearchRepository
import com.mk.skycast.core.model.PlaceSuggestion
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(private val placeSearchRepository: PlaceSearchRepository) {
    suspend operator fun invoke(query: String, languageCode: String): Outcome<List<PlaceSuggestion>, DataError> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return Outcome.Success(emptyList())
        return placeSearchRepository.search(trimmed, languageCode)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
