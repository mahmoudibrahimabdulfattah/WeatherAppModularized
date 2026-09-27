package com.mk.skycast.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.mk.skycast.core.database.dao.LocationDao
import com.mk.skycast.core.database.dao.WeatherDao
import com.mk.skycast.core.database.entity.CurrentWeatherEntity
import com.mk.skycast.core.database.entity.DailyForecastEntity
import com.mk.skycast.core.database.entity.HourlyAirQualityEntity
import com.mk.skycast.core.database.entity.HourlyForecastEntity
import com.mk.skycast.core.database.entity.LocationEntity

@Database(
    entities = [
        LocationEntity::class,
        CurrentWeatherEntity::class,
        HourlyForecastEntity::class,
        HourlyAirQualityEntity::class,
        DailyForecastEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class SkycastDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun weatherDao(): WeatherDao
}
