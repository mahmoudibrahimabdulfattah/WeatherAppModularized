package com.mk.skycast.core.data.repository

import com.mk.skycast.core.datastore.AiSettingsDataSource
import com.mk.skycast.core.domain.ai.AiConsent
import com.mk.skycast.core.domain.ai.AiSettingsRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreAiSettingsRepository @Inject constructor(private val dataSource: AiSettingsDataSource) :
    AiSettingsRepository {
    override val consent: Flow<AiConsent> = dataSource.consentName.map { name ->
        AiConsent.entries.firstOrNull { it.name == name } ?: AiConsent.UNKNOWN
    }

    override suspend fun setConsent(consent: AiConsent) = dataSource.setConsentName(consent.name)
    override suspend fun generationsOn(date: LocalDate): Int = dataSource.generationsOn(date)
    override suspend fun recordGeneration(date: LocalDate) = dataSource.recordGeneration(date)
}
