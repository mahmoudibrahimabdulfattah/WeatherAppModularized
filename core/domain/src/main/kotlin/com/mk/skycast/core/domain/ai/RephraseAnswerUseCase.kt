package com.mk.skycast.core.domain.ai

import com.mk.skycast.core.common.Outcome
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Optional friendly wording on top of a deterministic answer. Applies the region
 * and consent gates, the daily cap, a cache (unchanged answers cost nothing) and
 * output validation. Any failure simply means "no AI wording".
 */
@Singleton
class RephraseAnswerUseCase @Inject constructor(
    private val generator: AdviceGenerator,
    private val settings: AiSettingsRepository,
    private val region: AiRegionPolicy,
    private val clock: Clock,
) {
    private val cache = mutableMapOf<RephraseRequest, String>()
    private val mutex = Mutex()

    /** What the sheet should show before (or instead of) asking the model. */
    suspend fun gate(): AiWording? = when {
        !region.isSupported() -> AiWording.Hidden

        else -> when (settings.consent.first()) {
            AiConsent.UNKNOWN -> AiWording.NeedsConsent
            AiConsent.DECLINED -> AiWording.Declined
            AiConsent.GRANTED -> null
        }
    }

    suspend operator fun invoke(request: RephraseRequest): AiWording {
        gate()?.let { return it }
        return mutex.withLock {
            cache[request]?.let { return@withLock AiWording.Ready(it) }
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            if (settings.generationsOn(today) >= AiPolicy.DAILY_CAP) return@withLock AiWording.DailyLimitReached
            when (val result = generator.rephrase(request)) {
                is Outcome.Success -> {
                    settings.recordGeneration(today)
                    val text = AiPolicy.validate(result.data, request) ?: return@withLock AiWording.Unavailable
                    cache[request] = text
                    AiWording.Ready(text)
                }

                is Outcome.Failure -> AiWording.Unavailable
            }
        }
    }
}
