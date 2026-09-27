package com.mk.skycast.core.data.repository

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.Dispatcher
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.SkycastDispatchers
import com.mk.skycast.core.data.mapper.toCurrentEntity
import com.mk.skycast.core.data.mapper.toDailyEntities
import com.mk.skycast.core.data.mapper.toDomain
import com.mk.skycast.core.data.mapper.toHourlyEntities
import com.mk.skycast.core.database.dao.LocationDao
import com.mk.skycast.core.database.dao.WeatherDao
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.network.WeatherNetworkDataSource
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Room is the single source of truth. UI observes the database; the network only
 * writes into it. Concurrent refreshes for the same location are de-duplicated.
 */
@Singleton
internal class OfflineFirstWeatherRepository @Inject constructor(
    private val network: WeatherNetworkDataSource,
    private val weatherDao: WeatherDao,
    private val locationDao: LocationDao,
    private val clock: Clock,
    @Dispatcher(SkycastDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : WeatherRepository {

    private val locks = ConcurrentHashMap<Long, Mutex>()

    override fun observeWeather(locationId: Long): Flow<Weather?> = weatherDao.observe(locationId)
        .map { it?.toDomain() }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override suspend fun refresh(locationId: Long, force: Boolean): Outcome<Unit, DataError> =
        withContext(ioDispatcher) {
            locks.getOrPut(locationId) { Mutex() }.withLock {
                if (!force && isFresh(locationId)) return@withLock Outcome.Success(Unit)
                val location = locationDao.getById(locationId)
                    ?: return@withLock Outcome.Failure(DataError.NotFound)
                fetchAndStore(locationId, location.latitude, location.longitude)
            }
        }

    private suspend fun isFresh(locationId: Long): Boolean {
        val last = weatherDao.lastFetchedAt(locationId) ?: return false
        return Duration.ofMillis(clock.millis() - last) < FRESHNESS_WINDOW
    }

    private suspend fun fetchAndStore(
        locationId: Long,
        latitude: Double,
        longitude: Double,
    ): Outcome<Unit, DataError> = coroutineScope {
        val forecastDeferred = async { network.forecast(latitude, longitude) }
        // Air quality is a nice-to-have: its failure never fails the refresh.
        val airDeferred = async { network.airQuality(latitude, longitude) }
        when (val forecast = forecastDeferred.await()) {
            is Outcome.Failure -> forecast

            is Outcome.Success -> {
                val air = (airDeferred.await() as? Outcome.Success)?.data
                val data = forecast.data
                weatherDao.replace(
                    current = data.toCurrentEntity(locationId, clock.instant(), air),
                    hourly = data.toHourlyEntities(locationId),
                    daily = data.toDailyEntities(locationId),
                )
                if (locationDao.getById(locationId)?.timezone == null) {
                    locationDao.getById(locationId)?.let { locationDao.upsert(it.copy(timezone = data.timezone)) }
                }
                Outcome.Success(Unit)
            }
        }
    }

    companion object {
        /** Open-Meteo updates current conditions every 15 minutes. */
        val FRESHNESS_WINDOW: Duration = Duration.ofMinutes(10)
    }
}
