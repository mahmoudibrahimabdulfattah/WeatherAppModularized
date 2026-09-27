package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.model.Weather
import kotlinx.coroutines.flow.Flow

/** Offline-first weather source. The local cache is the single source of truth. */
interface WeatherRepository {
    fun observeWeather(locationId: Long): Flow<Weather?>

    /** Fetches fresh data unless the cache is newer than the freshness window (or [force] is set). */
    suspend fun refresh(locationId: Long, force: Boolean = false): Outcome<Unit, DataError>
}
