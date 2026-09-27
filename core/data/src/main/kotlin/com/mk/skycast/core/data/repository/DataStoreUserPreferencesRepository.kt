package com.mk.skycast.core.data.repository

import com.mk.skycast.core.datastore.UserPreferencesDataSource
import com.mk.skycast.core.datastore.UserPreferencesDataSource.EnumKey
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.WindSpeedUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = dataSource.preferences

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) = dataSource.setEnum(EnumKey.TEMPERATURE, unit)
    override suspend fun setWindSpeedUnit(unit: WindSpeedUnit) = dataSource.setEnum(EnumKey.WIND, unit)
    override suspend fun setPrecipitationUnit(unit: PrecipitationUnit) = dataSource.setEnum(EnumKey.PRECIPITATION, unit)
    override suspend fun setPressureUnit(unit: PressureUnit) = dataSource.setEnum(EnumKey.PRESSURE, unit)
    override suspend fun setTimeFormat(format: TimeFormat) = dataSource.setEnum(EnumKey.TIME_FORMAT, format)
    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setEnum(EnumKey.THEME, mode)
    override suspend fun setUseDynamicColor(enabled: Boolean) = dataSource.setUseDynamicColor(enabled)
    override suspend fun setSelectedLocationId(id: Long?) = dataSource.setSelectedLocationId(id)
}
