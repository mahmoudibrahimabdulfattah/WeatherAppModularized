package com.mk.skycast.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mk.skycast.core.model.BriefFingerprint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Stores the last notified brief as "yyyy-mm-dd|key,key". */
class BriefHistoryDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    suspend fun lastNotified(): BriefFingerprint? {
        val raw = runCatching { dataStore.data.first()[KEY] }.getOrNull() ?: return null
        val date = runCatching { LocalDate.parse(raw.substringBefore(SEPARATOR)) }.getOrNull() ?: return null
        val keys = raw.substringAfter(SEPARATOR, "").split(KEY_SEPARATOR).filter { it.isNotBlank() }.toSet()
        return BriefFingerprint(date, keys)
    }

    suspend fun save(fingerprint: BriefFingerprint) {
        dataStore.edit {
            it[KEY] =
                "${fingerprint.date}$SEPARATOR${fingerprint.keys.sorted().joinToString(KEY_SEPARATOR)}"
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("brief_last_notified")
        const val SEPARATOR = "|"
        const val KEY_SEPARATOR = ","
    }
}
