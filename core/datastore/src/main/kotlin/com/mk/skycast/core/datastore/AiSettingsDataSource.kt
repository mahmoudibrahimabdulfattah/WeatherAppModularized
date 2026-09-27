package com.mk.skycast.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** AI wording consent (enum name) and today's generation count. */
class AiSettingsDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val consentName: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[CONSENT] }

    suspend fun setConsentName(name: String) {
        dataStore.edit { it[CONSENT] = name }
    }

    suspend fun generationsOn(date: LocalDate): Int {
        val prefs = runCatching { dataStore.data.first() }.getOrNull() ?: return 0
        return if (prefs[COUNT_DATE] == date.toString()) prefs[COUNT] ?: 0 else 0
    }

    suspend fun recordGeneration(date: LocalDate) {
        dataStore.edit { prefs ->
            val sameDay = prefs[COUNT_DATE] == date.toString()
            prefs[COUNT_DATE] = date.toString()
            prefs[COUNT] = if (sameDay) (prefs[COUNT] ?: 0) + 1 else 1
        }
    }

    private companion object {
        val CONSENT = stringPreferencesKey("ai_consent")
        val COUNT_DATE = stringPreferencesKey("ai_generations_date")
        val COUNT = intPreferencesKey("ai_generations_count")
    }
}
