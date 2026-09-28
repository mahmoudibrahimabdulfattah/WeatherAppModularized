package com.mk.skycast.core.domain.ask

import java.time.DayOfWeek
import java.time.LocalDate

/** A typed question mapped onto one of the guided questions. */
data class AskIntent(val question: AskQuestion, val exercise: ExerciseKind? = null, val day: DayHint? = null)

/** "today", "tomorrow" or a weekday mentioned in a typed question. */
sealed interface DayHint {
    data object Today : DayHint
    data object Tomorrow : DayHint
    data class On(val day: DayOfWeek) : DayHint

    /** The date meant, within the next [horizonDays] days, or null if out of range. */
    fun resolve(today: LocalDate, horizonDays: Long = 3): LocalDate? = when (this) {
        Today -> today
        Tomorrow -> today.plusDays(1)
        is On -> (0 until horizonDays).map { today.plusDays(it) }.firstOrNull { it.dayOfWeek == day }
    }
}

/**
 * Understands typed questions in English and Arabic (including Egyptian wording)
 * well enough to route them to a guided, rule-based answer. Pure and offline.
 */
object AskIntentMatcher {

    fun match(text: String): AskIntent? {
        val normalized = tokens(normalize(text))
        if (normalized.isEmpty()) return null
        val exercise = EXERCISE.entries.firstOrNull { (_, words) -> normalized.hasAny(words) }?.key
        val question = when {
            normalized.hasAny(AIR) -> AskQuestion.AIR_QUALITY
            normalized.hasAny(LAUNDRY) -> AskQuestion.LAUNDRY
            exercise != null -> AskQuestion.BEST_EXERCISE_TIME
            normalized.hasAny(RAIN) -> AskQuestion.RAIN_NEXT_DAYS
            normalized.hasAny(HEAT) -> AskQuestion.AVOID_HEAT
            normalized.hasAny(WEAR) -> AskQuestion.WHAT_TO_WEAR
            else -> return null
        }
        return AskIntent(question, exercise, dayHint(normalized))
    }

    private fun dayHint(words: List<String>): DayHint? = when {
        words.hasAny(TOMORROW) -> DayHint.Tomorrow
        words.hasAny(TODAY) -> DayHint.Today
        else -> WEEKDAYS.entries.firstOrNull { (_, names) -> words.hasAny(names) }?.let { DayHint.On(it.key) }
    }

    /** Lower-case, no Arabic diacritics/tatweel, unified alef/ya/ta-marbuta forms. */
    internal fun normalize(text: String): String = text.lowercase()
        .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
        .replace(Regex("[أإآ]"), "ا")
        .replace('ى', 'ي')
        .replace('ة', 'ه')

    /** Words, each also without a leading Arabic conjunction/preposition and "ال". */
    private fun tokens(text: String): List<String> = text.split(Regex("[^\\p{L}]+"))
        .filter { it.isNotBlank() }
        .flatMap { word -> listOf(word, word.removePrefix("و").removePrefix("ب").removePrefix("ف").removePrefix("ال")) }

    /** A keyword matches the start of a word ("run" → "running"; "sun" ≠ "sunday" is avoided by listing forms). */
    private fun List<String>.hasAny(words: List<String>) = words.any { key ->
        if (' ' in key) joinToString(" ").contains(key) else any { it.startsWith(key) }
    }

    private val TODAY = listOf("today", "tonight", "النهارده", "اليوم", "الليله")
    private val TOMORROW = listOf("tomorrow", "بكره", "بكرا", "غدا")
    private val WEEKDAYS = mapOf(
        DayOfWeek.SATURDAY to listOf("saturday", "sat", "السبت", "سبت"),
        DayOfWeek.SUNDAY to listOf("sunday", "الاحد", "احد"),
        DayOfWeek.MONDAY to listOf("monday", "الاثنين", "الاتنين", "اثنين", "اتنين"),
        DayOfWeek.TUESDAY to listOf("tuesday", "الثلاثاء", "التلات", "ثلاثاء", "تلات"),
        DayOfWeek.WEDNESDAY to listOf("wednesday", "الاربعاء", "الاربع", "اربعاء"),
        DayOfWeek.THURSDAY to listOf("thursday", "الخميس", "خميس"),
        DayOfWeek.FRIDAY to listOf("friday", "الجمعه", "جمعه"),
    )
    private val WEAR = listOf(
        "wear", "dress", "jacket", "coat", "clothes", "outfit", "sweater",
        "البس", "ارتدي", "لبس", "ملابس", "جاكيت", "جاكت", "بلوفر", "هدوم", "جاكيته",
    )
    private val EXERCISE = linkedMapOf(
        ExerciseKind.RUN to listOf("run", "jog", "جري", "اجري", "جرى"),
        ExerciseKind.CYCLE to listOf("cycl", "bike", "ride", "دراجه", "عجله", "بسكلته"),
        ExerciseKind.WALK to listOf("walk", "stroll", "مشي", "امشي", "اتمشي", "تمشيه"),
    )
    private val HEAT = listOf("heat", "hot", "sunny", "sunshine", "حر", "حراره", "شمس", "سخن")
    private val LAUNDRY = listOf("laundry", "washing", "dry clothes", "غسيل", "انشر", "نشر", "الغسيل")
    private val RAIN = listOf("rain", "umbrella", "storm", "مطر", "تمطر", "هتمطر", "شمسيه", "مظله", "امطار")
    private val AIR = listOf("air", "dust", "pollution", "aqi", "هوا", "تراب", "غبار", "تلوث", "خماسين")
}
