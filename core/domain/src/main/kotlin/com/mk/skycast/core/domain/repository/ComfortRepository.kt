package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.model.ComfortFeedback
import com.mk.skycast.core.model.ComfortVote
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ComfortRepository {
    /** Most recent first. */
    val feedback: Flow<List<ComfortFeedback>>
    suspend fun record(date: LocalDate, vote: ComfortVote)
    suspend fun reset()
}
