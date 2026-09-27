package com.mk.skycast.core.network

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.network.model.NetworkAirQuality
import com.mk.skycast.core.network.model.NetworkForecast
import com.mk.skycast.core.network.model.NetworkPlace

/** Remote API boundary. Implementations never throw; failures are mapped to [DataError]. */
interface WeatherNetworkDataSource {
    suspend fun forecast(latitude: Double, longitude: Double): Outcome<NetworkForecast, DataError>
    suspend fun airQuality(latitude: Double, longitude: Double): Outcome<NetworkAirQuality, DataError>
    suspend fun searchPlaces(query: String, languageCode: String): Outcome<List<NetworkPlace>, DataError>
    suspend fun place(id: Long, languageCode: String): Outcome<NetworkPlace, DataError>
}
