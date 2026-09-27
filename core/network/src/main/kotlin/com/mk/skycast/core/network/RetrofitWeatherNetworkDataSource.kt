package com.mk.skycast.core.network

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.network.model.NetworkAirQuality
import com.mk.skycast.core.network.model.NetworkForecast
import com.mk.skycast.core.network.model.NetworkPlace
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal class RetrofitWeatherNetworkDataSource @Inject constructor(private val api: OpenMeteoApi) :
    WeatherNetworkDataSource {

    override suspend fun forecast(latitude: Double, longitude: Double) = safeCall { api.forecast(latitude, longitude) }

    override suspend fun airQuality(latitude: Double, longitude: Double) =
        safeCall { api.airQuality(latitude, longitude) }

    override suspend fun searchPlaces(query: String, languageCode: String) =
        safeCall { api.searchPlaces(query, languageCode).results }

    override suspend fun place(id: Long, languageCode: String) = safeCall { api.place(id, languageCode) }
}

internal suspend fun <T> safeCall(block: suspend () -> T): Outcome<T, DataError> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    Outcome.Failure(DataError.Timeout)
} catch (e: UnknownHostException) {
    Outcome.Failure(DataError.NoInternet)
} catch (e: IOException) {
    Outcome.Failure(DataError.NoInternet)
} catch (e: HttpException) {
    Outcome.Failure(if (e.code() == 404) DataError.NotFound else DataError.Server(e.code()))
} catch (e: SerializationException) {
    Outcome.Failure(DataError.Serialization)
} catch (e: Exception) {
    Outcome.Failure(DataError.Unknown)
}
