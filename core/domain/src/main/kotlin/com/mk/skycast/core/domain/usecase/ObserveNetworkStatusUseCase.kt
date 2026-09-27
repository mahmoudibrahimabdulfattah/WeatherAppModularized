package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.NetworkMonitor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveNetworkStatusUseCase @Inject constructor(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline.distinctUntilChanged()
}
