package com.mk.skycast.core.data.repository

import com.mk.skycast.core.datastore.BriefHistoryDataSource
import com.mk.skycast.core.domain.repository.BriefHistoryRepository
import com.mk.skycast.core.model.BriefFingerprint
import javax.inject.Inject

internal class DataStoreBriefHistoryRepository @Inject constructor(private val dataSource: BriefHistoryDataSource) :
    BriefHistoryRepository {
    override suspend fun lastNotified(): BriefFingerprint? = dataSource.lastNotified()
    override suspend fun saveNotified(fingerprint: BriefFingerprint) = dataSource.save(fingerprint)
}
