package com.mk.skycast.core.domain.ai

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.domain.ask.AskQuestion
import com.mk.skycast.core.testing.FakeAdviceGenerator
import com.mk.skycast.core.testing.FakeAiSettingsRepository
import com.mk.skycast.core.testing.TestData
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RephraseAnswerTest {

    private val request = RephraseRequest(
        question = AskQuestion.BEST_EXERCISE_TIME,
        languageTag = "en",
        headline = "Best time for a walk: Tomorrow 6:00–8:00 AM",
        facts = listOf("Feels like 22°"),
        reasons = listOf("No rain expected."),
    )

    // --- policy ------------------------------------------------------------------------

    @Test
    fun `output with a number the facts don't contain is rejected`() {
        assertThat(AiPolicy.validate("Go at 7 before it hits 30 degrees.", request)).isNull()
        assertThat(AiPolicy.validate("Head out between 6:00 and 8:00, it feels like 22°.", request)).isNotNull()
    }

    @Test
    fun `spelled-out numbers are rejected`() {
        assertThat(AiPolicy.validate("It feels like twenty-two degrees.", request)).isNull()
        assertThat(AiPolicy.validate("تبلغ نسبة الغبار خمسة عشر ميكروغرامًا", request)).isNull()
        assertThat(AiPolicy.validate("ستكون الأجواء جافة يوم الثلاثاء والأربعاء، ويمكنك الاستمتاع بها.", request))
            .isNotNull()
    }

    @Test
    fun `arabic-indic digits are checked like latin ones`() {
        assertThat(AiPolicy.validate("اخرج الساعة ٦:٠٠", request)).isNotNull()
        assertThat(AiPolicy.validate("اخرج الساعة ٩", request)).isNull()
    }

    @Test
    fun `markdown is stripped and empty or overlong output rejected`() {
        assertThat(AiPolicy.validate("**Great** time for a _walk_.", request)).isEqualTo("Great time for a walk.")
        assertThat(AiPolicy.validate("   ", request)).isNull()
        assertThat(AiPolicy.validate("a".repeat(500), request)).isNull()
    }

    @Test
    fun `only launch countries outside the EEA, UK and CH are allowed`() {
        assertThat(AiPolicy.isCountryAllowed("eg")).isTrue()
        assertThat(AiPolicy.isCountryAllowed("DE")).isFalse()
        assertThat(AiPolicy.isCountryAllowed("GB")).isFalse()
        assertThat(AiPolicy.isCountryAllowed(null)).isFalse()
        assertThat(AiPolicy.isCountryAllowed("")).isFalse()
    }

    // --- use case ----------------------------------------------------------------------

    private val clock = Clock.fixed(TestData.NOW, ZoneOffset.UTC)
    private val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
    private val generator = FakeAdviceGenerator()
    private val settings = FakeAiSettingsRepository(AiConsent.GRANTED)
    private fun useCase(supported: Boolean = true) = RephraseAnswerUseCase(generator, settings, { supported }, clock)

    @Test
    fun `unsupported region hides AI without asking`() = runTest {
        assertThat(useCase(supported = false)(request)).isEqualTo(AiWording.Hidden)
        assertThat(generator.requests).isEmpty()
    }

    @Test
    fun `consent is required before the first request`() = runTest {
        settings.setConsent(AiConsent.UNKNOWN)
        assertThat(useCase()(request)).isEqualTo(AiWording.NeedsConsent)
        settings.setConsent(AiConsent.DECLINED)
        assertThat(useCase()(request)).isEqualTo(AiWording.Declined)
        assertThat(generator.requests).isEmpty()
    }

    @Test
    fun `unchanged answers come from the cache and don't spend quota`() = runTest {
        val useCase = useCase()

        assertThat(useCase(request)).isEqualTo(AiWording.Ready("Friendly wording."))
        assertThat(useCase(request)).isEqualTo(AiWording.Ready("Friendly wording."))

        assertThat(generator.requests).hasSize(1)
        assertThat(settings.generations[today]).isEqualTo(1)
    }

    @Test
    fun `daily cap stops generation`() = runTest {
        settings.generations[today] = AiPolicy.DAILY_CAP

        assertThat(useCase()(request)).isEqualTo(AiWording.DailyLimitReached)
        assertThat(generator.requests).isEmpty()
    }

    @Test
    fun `errors and invalid output fall back to no wording`() = runTest {
        generator.result = Outcome.Failure(AiError.QUOTA_EXCEEDED)
        assertThat(useCase()(request)).isEqualTo(AiWording.Unavailable)

        generator.result = Outcome.Success("It will be 35 degrees.")
        assertThat(useCase()(request)).isEqualTo(AiWording.Unavailable)
    }
}
