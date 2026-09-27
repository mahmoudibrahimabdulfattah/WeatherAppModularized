package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.WindSpeedUnit
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setWindSpeedUnit(unit: WindSpeedUnit)
    suspend fun setPrecipitationUnit(unit: PrecipitationUnit)
    suspend fun setPressureUnit(unit: PressureUnit)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(enabled: Boolean)
    suspend fun setSelectedLocationId(id: Long?)
}
