package com.mk.skycast.core.data.mapper

import com.mk.skycast.core.database.entity.CurrentWeatherEntity
import com.mk.skycast.core.database.entity.DailyForecastEntity
import com.mk.skycast.core.database.entity.HourlyAirQualityEntity
import com.mk.skycast.core.database.entity.HourlyForecastEntity
import com.mk.skycast.core.database.entity.PopulatedWeather
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.model.HourlyAirQuality
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.network.model.NetworkAirQuality
import com.mk.skycast.core.network.model.NetworkForecast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// ---- Network -> Entity ----

internal fun NetworkForecast.toCurrentEntity(locationId: Long, fetchedAt: Instant, airQuality: NetworkAirQuality?) =
    CurrentWeatherEntity(
        locationId = locationId,
        fetchedAtEpochMillis = fetchedAt.toEpochMilli(),
        timezone = timezone,
        utcOffsetSeconds = utcOffsetSeconds,
        timeEpochSeconds = current.time,
        weatherCode = current.weatherCode,
        isDay = current.isDay == 1,
        temperatureC = current.temperature,
        apparentTemperatureC = current.apparentTemperature,
        relativeHumidity = current.relativeHumidity,
        dewPointC = current.dewPoint,
        precipitationMm = current.precipitation,
        cloudCover = current.cloudCover,
        pressureHpa = current.pressureMsl,
        windSpeedKmh = current.windSpeed,
        windDirectionDegrees = current.windDirection,
        windGustsKmh = current.windGusts,
        uvIndex = current.uvIndex,
        visibilityMeters = current.visibility,
        usAqi = airQuality?.current?.usAqi,
        pm25 = airQuality?.current?.pm25,
        pm10 = airQuality?.current?.pm10,
    )

/** Rows with a missing temperature are dropped (Open-Meteo pads arrays with nulls). */
internal fun NetworkForecast.toHourlyEntities(locationId: Long): List<HourlyForecastEntity> =
    hourly.time.mapIndexedNotNull { i, time ->
        val temperature = hourly.temperature.getOrNull(i) ?: return@mapIndexedNotNull null
        HourlyForecastEntity(
            locationId = locationId,
            timeEpochSeconds = time,
            weatherCode = hourly.weatherCode.getOrNull(i) ?: -1,
            isDay = hourly.isDay.getOrNull(i) == 1,
            temperatureC = temperature,
            apparentTemperatureC = hourly.apparentTemperature.getOrNull(i),
            relativeHumidity = hourly.relativeHumidity.getOrNull(i),
            precipitationProbability = hourly.precipitationProbability.getOrNull(i),
            precipitationMm = hourly.precipitation.getOrNull(i) ?: 0.0,
            windSpeedKmh = hourly.windSpeed.getOrNull(i) ?: 0.0,
            windGustsKmh = hourly.windGusts.getOrNull(i),
            uvIndex = hourly.uvIndex.getOrNull(i),
            visibilityMeters = hourly.visibility.getOrNull(i),
        )
    }

internal fun NetworkAirQuality?.toHourlyEntities(locationId: Long): List<HourlyAirQualityEntity> {
    val hourly = this?.hourly ?: return emptyList()
    return hourly.time.mapIndexed { i, time ->
        HourlyAirQualityEntity(
            locationId = locationId,
            timeEpochSeconds = time,
            usAqi = hourly.usAqi.getOrNull(i),
            pm10 = hourly.pm10.getOrNull(i),
            dust = hourly.dust.getOrNull(i),
        )
    }
}

internal fun NetworkForecast.toDailyEntities(locationId: Long): List<DailyForecastEntity> {
    val offset = ZoneOffset.ofTotalSeconds(utcOffsetSeconds)
    return daily.time.mapIndexedNotNull { i, time ->
        val max = daily.temperatureMax.getOrNull(i) ?: return@mapIndexedNotNull null
        val min = daily.temperatureMin.getOrNull(i) ?: return@mapIndexedNotNull null
        DailyForecastEntity(
            locationId = locationId,
            // Daily timestamps are local midnight expressed in UTC seconds.
            epochDay = Instant.ofEpochSecond(time).atOffset(offset).toLocalDate().toEpochDay(),
            weatherCode = daily.weatherCode.getOrNull(i) ?: -1,
            temperatureMaxC = max,
            temperatureMinC = min,
            precipitationProbabilityMax = daily.precipitationProbabilityMax.getOrNull(i),
            precipitationSumMm = daily.precipitationSum.getOrNull(i) ?: 0.0,
            sunriseEpochSeconds = daily.sunrise.getOrNull(i),
            sunsetEpochSeconds = daily.sunset.getOrNull(i),
            uvIndexMax = daily.uvIndexMax.getOrNull(i),
            windSpeedMaxKmh = daily.windSpeedMax.getOrNull(i) ?: 0.0,
        )
    }
}

// ---- Entity -> Domain ----

internal fun PopulatedWeather.toDomain(): Weather = Weather(
    locationId = current.locationId,
    zoneId = current.zoneId(),
    current = CurrentWeather(
        time = Instant.ofEpochSecond(current.timeEpochSeconds),
        condition = WeatherCondition.fromWmoCode(current.weatherCode),
        isDay = current.isDay,
        temperatureC = current.temperatureC,
        apparentTemperatureC = current.apparentTemperatureC,
        relativeHumidity = current.relativeHumidity,
        dewPointC = current.dewPointC,
        precipitationMm = current.precipitationMm,
        cloudCover = current.cloudCover,
        pressureHpa = current.pressureHpa,
        windSpeedKmh = current.windSpeedKmh,
        windDirectionDegrees = current.windDirectionDegrees,
        windGustsKmh = current.windGustsKmh,
        uvIndex = current.uvIndex,
        visibilityMeters = current.visibilityMeters,
    ),
    hourly = hourly.sortedBy { it.timeEpochSeconds }.map {
        HourlyForecast(
            time = Instant.ofEpochSecond(it.timeEpochSeconds),
            condition = WeatherCondition.fromWmoCode(it.weatherCode),
            isDay = it.isDay,
            temperatureC = it.temperatureC,
            apparentTemperatureC = it.apparentTemperatureC,
            relativeHumidity = it.relativeHumidity,
            precipitationProbability = it.precipitationProbability,
            precipitationMm = it.precipitationMm,
            windSpeedKmh = it.windSpeedKmh,
            windGustsKmh = it.windGustsKmh,
            uvIndex = it.uvIndex,
            visibilityMeters = it.visibilityMeters,
        )
    },
    daily = daily.sortedBy { it.epochDay }.map {
        DailyForecast(
            date = LocalDate.ofEpochDay(it.epochDay),
            condition = WeatherCondition.fromWmoCode(it.weatherCode),
            temperatureMaxC = it.temperatureMaxC,
            temperatureMinC = it.temperatureMinC,
            precipitationProbabilityMax = it.precipitationProbabilityMax,
            precipitationSumMm = it.precipitationSumMm,
            sunrise = it.sunriseEpochSeconds?.let(Instant::ofEpochSecond),
            sunset = it.sunsetEpochSeconds?.let(Instant::ofEpochSecond),
            uvIndexMax = it.uvIndexMax,
            windSpeedMaxKmh = it.windSpeedMaxKmh,
        )
    },
    airQuality = if (current.usAqi == null && current.pm25 == null && current.pm10 == null &&
        hourlyAirQuality.isEmpty()
    ) {
        null
    } else {
        AirQuality(
            usAqi = current.usAqi,
            pm25 = current.pm25,
            pm10 = current.pm10,
            hourly = hourlyAirQuality.sortedBy { it.timeEpochSeconds }.map {
                HourlyAirQuality(
                    time = Instant.ofEpochSecond(it.timeEpochSeconds),
                    usAqi = it.usAqi,
                    pm10 = it.pm10,
                    dust = it.dust,
                )
            },
        )
    },
    fetchedAt = Instant.ofEpochMilli(current.fetchedAtEpochMillis),
)

private fun CurrentWeatherEntity.zoneId(): ZoneId =
    runCatching { ZoneId.of(timezone) }.getOrElse { ZoneOffset.ofTotalSeconds(utcOffsetSeconds) }
