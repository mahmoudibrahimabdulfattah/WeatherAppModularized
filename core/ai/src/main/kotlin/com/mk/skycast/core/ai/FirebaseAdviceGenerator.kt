package com.mk.skycast.core.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.ai.AdviceGenerator
import com.mk.skycast.core.domain.ai.AiError
import com.mk.skycast.core.domain.ai.RephraseRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Gemini through Firebase AI Logic on the Gemini Developer API (free Spark plan).
 * No API key ships in the app; Firebase authorizes the request.
 */
@Singleton
internal class FirebaseAdviceGenerator @Inject constructor() : AdviceGenerator {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig { maxOutputTokens = MAX_OUTPUT_TOKENS },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
        )
    }

    override suspend fun rephrase(request: RephraseRequest): Outcome<String, AiError> = try {
        val text = model.generateContent(prompt(request)).text
        if (text.isNullOrBlank()) Outcome.Failure(AiError.BLOCKED) else Outcome.Success(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: QuotaExceededException) {
        Log.w(TAG, "Gemini quota exceeded", e)
        Outcome.Failure(AiError.QUOTA_EXCEEDED)
    } catch (e: ResponseStoppedException) {
        Log.w(TAG, "Gemini response stopped", e)
        Outcome.Failure(AiError.BLOCKED)
    } catch (e: Exception) {
        Log.w(TAG, "Gemini request failed", e)
        Outcome.Failure(AiError.UNAVAILABLE)
    }

    private fun prompt(request: RephraseRequest): String = buildString {
        appendLine("Language: ${request.languageTag}")
        appendLine("Question: ${request.question.name.lowercase().replace('_', ' ')}")
        appendLine("Answer: ${request.headline}")
        request.facts.forEach { appendLine("Fact: $it") }
        request.reasons.forEach { appendLine("Reason: $it") }
    }

    private companion object {
        /** Cheapest stable model on the free tier (firebase.google.com/docs/ai-logic/models). */
        const val TAG = "AdviceGenerator"
        const val MODEL = "gemini-3.5-flash-lite"
        const val MAX_OUTPUT_TOKENS = 200
        val SYSTEM_INSTRUCTION = """
            You are the friendly voice of a weather app. Rewrite the given answer as 2 or 3 short, warm,
            practical sentences for the user, in the language given (formal Modern Standard Arabic for "ar").
            Use only the facts provided. Never add numbers, times, temperatures, advice or warnings that are
            not in the facts, never contradict them, and never promise safety. Do not greet the user.
            Write every number exactly as it appears in the facts, as digits; never spell numbers out in words.
            No emojis, no markdown, no lists.
        """.trimIndent()
    }
}
