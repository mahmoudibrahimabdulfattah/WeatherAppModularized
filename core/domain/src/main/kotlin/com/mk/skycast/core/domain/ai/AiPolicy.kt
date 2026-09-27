package com.mk.skycast.core.domain.ai

/** Pure rules around the free Gemini tier, kept here so they are unit-tested. */
object AiPolicy {
    /** Generations per install per local day (soft limit; the free quota is shared by all users). */
    const val DAILY_CAP = 5

    private const val MAX_OUTPUT_CHARS = 420

    /** Launch countries. The free tier may not serve the EEA, UK or Switzerland. */
    private val ALLOWED_COUNTRIES = setOf("EG")
    private val BLOCKED_COUNTRIES = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU",
        "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "IS", "LI", "NO", "GB", "CH",
    )

    fun isCountryAllowed(countryCode: String?): Boolean {
        val code = countryCode?.uppercase()?.takeIf { it.length == 2 } ?: return false
        return code in ALLOWED_COUNTRIES && code !in BLOCKED_COUNTRIES
    }

    /**
     * Cleans model output, or returns null to fall back to the deterministic answer:
     * no markdown, bounded length, and no number that the facts don't contain.
     */
    fun validate(output: String, request: RephraseRequest): String? {
        val text = output
            .replace(Regex("[*_#`>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isEmpty() || text.length > MAX_OUTPUT_CHARS) return null
        val allowed = numbers(request.allowedText)
        return text.takeIf { numbers(it).all { number -> number in allowed } }
    }

    private fun numbers(text: String): Set<String> = Regex("\\d+").findAll(toLatinDigits(text)).map { it.value }.toSet()

    private fun toLatinDigits(text: String): String = buildString(text.length) {
        text.forEach { c ->
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    else -> c
                },
            )
        }
    }
}
