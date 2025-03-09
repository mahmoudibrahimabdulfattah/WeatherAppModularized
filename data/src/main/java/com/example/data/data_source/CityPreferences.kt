package com.example.data.data_source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "city_preferences")

@Singleton
class CityPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val LAST_CITY_KEY = stringPreferencesKey("last_city")
        private const val DEFAULT_CITY = "Cairo"
    }

    val lastCity: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_CITY_KEY] ?: DEFAULT_CITY
        }

    suspend fun saveCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CITY_KEY] = city
        }
    }
}