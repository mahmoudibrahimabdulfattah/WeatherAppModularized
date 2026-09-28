package com.mk.skycast.core.domain.ask

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AskIntentMatcherTest {

    @Test
    fun `english questions map to guided answers`() {
        assertThat(AskIntentMatcher.match("Do I need a jacket tomorrow?")?.question).isEqualTo(AskQuestion.WHAT_TO_WEAR)
        assertThat(AskIntentMatcher.match("Will it rain on Thursday?")?.question).isEqualTo(AskQuestion.RAIN_NEXT_DAYS)
        assertThat(AskIntentMatcher.match("When can I go for a run?"))
            .isEqualTo(AskIntent(AskQuestion.BEST_EXERCISE_TIME, ExerciseKind.RUN))
        assertThat(AskIntentMatcher.match("Is the air ok for my kids?")?.question).isEqualTo(AskQuestion.AIR_QUALITY)
    }

    @Test
    fun `egyptian and formal arabic questions map too`() {
        assertThat(AskIntentMatcher.match("ألبس إيه بكرة؟")?.question).isEqualTo(AskQuestion.WHAT_TO_WEAR)
        assertThat(AskIntentMatcher.match("هتمطر الخميس؟")?.question).isEqualTo(AskQuestion.RAIN_NEXT_DAYS)
        assertThat(AskIntentMatcher.match("ينفع أجري الصبح؟"))
            .isEqualTo(AskIntent(AskQuestion.BEST_EXERCISE_TIME, ExerciseKind.RUN))
        assertThat(AskIntentMatcher.match("أنشر الغسيل امتى؟")?.question).isEqualTo(AskQuestion.LAUNDRY)
        assertThat(AskIntentMatcher.match("فيه تراب النهارده؟")?.question).isEqualTo(AskQuestion.AIR_QUALITY)
        assertThat(AskIntentMatcher.match("إمتى الحر يخف؟")?.question).isEqualTo(AskQuestion.AVOID_HEAT)
    }

    @Test
    fun `unrelated questions are not forced into a guided answer`() {
        assertThat(AskIntentMatcher.match("How windy will it be on Friday evening?")).isNull()
        assertThat(AskIntentMatcher.match("   ")).isNull()
        assertThat(
            AskIntentMatcher.match("What should I wear on Sunday?")?.question,
        ).isEqualTo(AskQuestion.WHAT_TO_WEAR)
    }

    @Test
    fun `days named in the question narrow the answer`() {
        assertThat(AskIntentMatcher.match("run tomorrow morning")?.day).isEqualTo(DayHint.Tomorrow)
        assertThat(AskIntentMatcher.match("هتمطر الخميس؟")?.day).isEqualTo(DayHint.On(java.time.DayOfWeek.THURSDAY))
        assertThat(AskIntentMatcher.match("ألبس إيه النهارده؟")?.day).isEqualTo(DayHint.Today)
        val monday = java.time.LocalDate.of(2026, 9, 28)
        assertThat(DayHint.On(java.time.DayOfWeek.WEDNESDAY).resolve(monday)).isEqualTo(monday.plusDays(2))
        assertThat(DayHint.On(java.time.DayOfWeek.FRIDAY).resolve(monday)).isNull()
    }
}
