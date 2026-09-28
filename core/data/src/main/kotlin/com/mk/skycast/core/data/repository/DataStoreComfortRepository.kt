package com.mk.skycast.core.data.repository

import com.mk.skycast.core.datastore.ComfortDataSource
import com.mk.skycast.core.domain.repository.ComfortRepository
import com.mk.skycast.core.model.ComfortFeedback
import com.mk.skycast.core.model.ComfortVote
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreComfortRepository @Inject constructor(private val dataSource: ComfortDataSource) :
    ComfortRepository {
    override val feedback: Flow<List<ComfortFeedback>> = dataSource.feedback
    override suspend fun record(date: LocalDate, vote: ComfortVote) = dataSource.record(date, vote)
    override suspend fun reset() = dataSource.reset()
}
