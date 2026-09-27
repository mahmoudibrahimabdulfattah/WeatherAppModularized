package com.mk.skycast.core.domain.ai

import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.ask.AskQuestion
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * What is sent to the language model: the already-decided answer, in the
 * user's language. Weather facts only — never location, routine or identity.
 */
data class RephraseRequest(
    val question: AskQuestion,
    val languageTag: String,
    val headline: String,
    val facts: List<String>,
    val reasons: List<String>,
) {
    /** Everything the model may repeat; used to validate its output. */
    val allowedText: String get() = (listOf(headline) + facts + reasons).joinToString("\n")
}

enum class AiError { UNAVAILABLE, QUOTA_EXCEEDED, BLOCKED }

/** Rephrases a deterministic answer. Implementations must not add facts. */
interface AdviceGenerator {
    suspend fun rephrase(request: RephraseRequest): Outcome<String, AiError>
}

enum class AiConsent { UNKNOWN, GRANTED, DECLINED }

interface AiSettingsRepository {
    val consent: Flow<AiConsent>
    suspend fun setConsent(consent: AiConsent)
    suspend fun generationsOn(date: LocalDate): Int
    suspend fun recordGeneration(date: LocalDate)
}

/** Whether the device is somewhere the free Gemini tier may be used. */
fun interface AiRegionPolicy {
    fun isSupported(): Boolean
}

/** Result of asking for AI wording, as the Ask sheet shows it. */
sealed interface AiWording {
    /** Region not supported: no AI, no prompt, nothing shown. */
    data object Hidden : AiWording
    data object NeedsConsent : AiWording
    data object Declined : AiWording
    data object Loading : AiWording
    data class Ready(val text: String) : AiWording
    data object DailyLimitReached : AiWording
    data object Unavailable : AiWording
}
