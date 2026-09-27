package com.mk.skycast.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mk.skycast.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserPreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val defaults = UserPreferences()
            UserPreferences(
                temperatureUnit = prefs[Keys.TEMPERATURE].toEnum(defaults.temperatureUnit),
                windSpeedUnit = prefs[Keys.WIND].toEnum(defaults.windSpeedUnit),
                precipitationUnit = prefs[Keys.PRECIPITATION].toEnum(defaults.precipitationUnit),
                pressureUnit = prefs[Keys.PRESSURE].toEnum(defaults.pressureUnit),
                timeFormat = prefs[Keys.TIME_FORMAT].toEnum(defaults.timeFormat),
                themeMode = prefs[Keys.THEME].toEnum(defaults.themeMode),
                useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                selectedLocationId = prefs[Keys.SELECTED_LOCATION],
            )
        }

    suspend fun <E : Enum<E>> setEnum(key: EnumKey, value: E) {
        dataStore.edit { it[key.key] = value.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSelectedLocationId(id: Long?) {
        dataStore.edit { if (id == null) it.remove(Keys.SELECTED_LOCATION) else it[Keys.SELECTED_LOCATION] = id }
    }

    enum class EnumKey(internal val key: Preferences.Key<String>) {
        TEMPERATURE(Keys.TEMPERATURE),
        WIND(Keys.WIND),
        PRECIPITATION(Keys.PRECIPITATION),
        PRESSURE(Keys.PRESSURE),
        TIME_FORMAT(Keys.TIME_FORMAT),
        THEME(Keys.THEME),
    }

    private object Keys {
        val TEMPERATURE = stringPreferencesKey("temperature_unit")
        val WIND = stringPreferencesKey("wind_speed_unit")
        val PRECIPITATION = stringPreferencesKey("precipitation_unit")
        val PRESSURE = stringPreferencesKey("pressure_unit")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SELECTED_LOCATION = longPreferencesKey("selected_location_id")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
