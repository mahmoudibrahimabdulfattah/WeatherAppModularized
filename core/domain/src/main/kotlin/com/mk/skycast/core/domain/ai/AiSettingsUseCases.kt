package com.mk.skycast.core.domain.ai

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Whether AI wording is offered here at all, and the user's current choice. */
data class AiAvailability(val supported: Boolean, val consent: AiConsent)

class ObserveAiAvailabilityUseCase @Inject constructor(
    private val settings: AiSettingsRepository,
    private val region: AiRegionPolicy,
) {
    operator fun invoke(): Flow<AiAvailability> = settings.consent.map { AiAvailability(region.isSupported(), it) }
}

class SetAiConsentUseCase @Inject constructor(private val settings: AiSettingsRepository) {
    suspend operator fun invoke(consent: AiConsent) = settings.setConsent(consent)
}
