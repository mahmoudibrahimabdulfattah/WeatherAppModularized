package com.mk.skycast.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "current_weather",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CurrentWeatherEntity(
    @PrimaryKey val locationId: Long,
    val fetchedAtEpochMillis: Long,
    val timezone: String,
    val utcOffsetSeconds: Int,
    val timeEpochSeconds: Long,
    val weatherCode: Int,
    val isDay: Boolean,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val relativeHumidity: Int,
    val dewPointC: Double?,
    val precipitationMm: Double,
    val cloudCover: Int,
    val pressureHpa: Double,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val windGustsKmh: Double?,
    val uvIndex: Double?,
    val visibilityMeters: Double?,
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
)

@Entity(
    tableName = "hourly_forecast",
    primaryKeys = ["locationId", "timeEpochSeconds"],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HourlyForecastEntity(
    val locationId: Long,
    val timeEpochSeconds: Long,
    val weatherCode: Int,
    val isDay: Boolean,
    val temperatureC: Double,
    @ColumnInfo(defaultValue = "NULL") val apparentTemperatureC: Double?,
    @ColumnInfo(defaultValue = "NULL") val relativeHumidity: Int?,
    val precipitationProbability: Int?,
    val precipitationMm: Double,
    val windSpeedKmh: Double,
    @ColumnInfo(defaultValue = "NULL") val windGustsKmh: Double?,
    val uvIndex: Double?,
    @ColumnInfo(defaultValue = "NULL") val visibilityMeters: Double?,
)

@Entity(
    tableName = "hourly_air_quality",
    primaryKeys = ["locationId", "timeEpochSeconds"],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HourlyAirQualityEntity(
    val locationId: Long,
    val timeEpochSeconds: Long,
    @ColumnInfo(defaultValue = "NULL") val usAqi: Int?,
    @ColumnInfo(defaultValue = "NULL") val pm10: Double?,
    @ColumnInfo(defaultValue = "NULL") val dust: Double?,
)

@Entity(
    tableName = "daily_forecast",
    primaryKeys = ["locationId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DailyForecastEntity(
    val locationId: Long,
    val epochDay: Long,
    val weatherCode: Int,
    val temperatureMaxC: Double,
    val temperatureMinC: Double,
    val precipitationProbabilityMax: Int?,
    val precipitationSumMm: Double,
    val sunriseEpochSeconds: Long?,
    val sunsetEpochSeconds: Long?,
    val uvIndexMax: Double?,
    val windSpeedMaxKmh: Double,
)

data class PopulatedWeather(
    @Embedded val current: CurrentWeatherEntity,
    @Relation(parentColumn = "locationId", entityColumn = "locationId")
    val hourly: List<HourlyForecastEntity>,
    @Relation(parentColumn = "locationId", entityColumn = "locationId")
    val daily: List<DailyForecastEntity>,
    @Relation(parentColumn = "locationId", entityColumn = "locationId")
    val hourlyAirQuality: List<HourlyAirQualityEntity>,
)
