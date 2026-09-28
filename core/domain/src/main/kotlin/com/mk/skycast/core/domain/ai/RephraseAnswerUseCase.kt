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
    private val cache = mutableMapOf<Any, String>()
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

    suspend operator fun invoke(request: RephraseRequest): AiWording =
        generate(request, { generator.rephrase(request) }, { AiPolicy.validate(it, request) })

    /** Answers a typed question from forecast facts only; same gates, cap and cache. */
    suspend fun answer(request: FreeQuestionRequest): AiWording =
        generate(request, { generator.answer(request) }, { AiPolicy.validate(it, request) })

    private suspend fun generate(
        key: Any,
        call: suspend () -> Outcome<String, AiError>,
        validate: (String) -> String?,
    ): AiWording {
        gate()?.let { return it }
        return mutex.withLock {
            cache[key]?.let { return@withLock AiWording.Ready(it) }
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            if (settings.generationsOn(today) >= AiPolicy.DAILY_CAP) return@withLock AiWording.DailyLimitReached
            when (val result = call()) {
                is Outcome.Success -> {
                    settings.recordGeneration(today)
                    val text = validate(result.data) ?: return@withLock AiWording.Unavailable
                    cache[key] = text
                    AiWording.Ready(text)
                }

                is Outcome.Failure -> AiWording.Unavailable
            }
        }
    }
}
