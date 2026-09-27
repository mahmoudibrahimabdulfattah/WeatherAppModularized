package com.mk.skycast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mk.skycast.core.database.entity.CurrentWeatherEntity
import com.mk.skycast.core.database.entity.DailyForecastEntity
import com.mk.skycast.core.database.entity.HourlyForecastEntity
import com.mk.skycast.core.database.entity.PopulatedWeather
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Transaction
    @Query("SELECT * FROM current_weather WHERE locationId = :locationId")
    fun observe(locationId: Long): Flow<PopulatedWeather?>

    @Query("SELECT fetchedAtEpochMillis FROM current_weather WHERE locationId = :locationId")
    suspend fun lastFetchedAt(locationId: Long): Long?

    @Upsert
    suspend fun upsertCurrent(entity: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourly(entities: List<HourlyForecastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaily(entities: List<DailyForecastEntity>)

    @Query("DELETE FROM hourly_forecast WHERE locationId = :locationId")
    suspend fun deleteHourly(locationId: Long)

    @Query("DELETE FROM daily_forecast WHERE locationId = :locationId")
    suspend fun deleteDaily(locationId: Long)

    /** Atomically replaces the whole snapshot so observers never see a half-written state. */
    @Transaction
    suspend fun replace(
        current: CurrentWeatherEntity,
        hourly: List<HourlyForecastEntity>,
        daily: List<DailyForecastEntity>,
    ) {
        upsertCurrent(current)
        deleteHourly(current.locationId)
        deleteDaily(current.locationId)
        insertHourly(hourly)
        insertDaily(daily)
    }
}
