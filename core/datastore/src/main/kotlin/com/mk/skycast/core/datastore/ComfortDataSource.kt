package com.mk.skycast.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mk.skycast.core.model.ComfortFeedback
import com.mk.skycast.core.model.ComfortVote
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Recent comfort votes as "yyyy-mm-dd:VOTE" entries, most recent first. */
class ComfortDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val feedback: Flow<List<ComfortFeedback>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it[KEY]) }

    suspend fun record(date: LocalDate, vote: ComfortVote) {
        dataStore.edit { prefs ->
            val updated = listOf(ComfortFeedback(date, vote)) + decode(prefs[KEY]).filterNot { it.date == date }
            prefs[KEY] = updated.take(MAX_ENTRIES).joinToString(SEPARATOR) { "${it.date}:${it.vote.name}" }
        }
    }

    suspend fun reset() {
        dataStore.edit { it.remove(KEY) }
    }

    private fun decode(raw: String?): List<ComfortFeedback> = raw.orEmpty()
        .split(SEPARATOR)
        .mapNotNull { entry ->
            val date = runCatching { LocalDate.parse(entry.substringBefore(':')) }.getOrNull()
            val vote = ComfortVote.entries.firstOrNull { it.name == entry.substringAfter(':') }
            if (date != null && vote != null) ComfortFeedback(date, vote) else null
        }

    private companion object {
        val KEY = stringPreferencesKey("comfort_feedback")
        const val SEPARATOR = ","
        const val MAX_ENTRIES = 30
    }
}
