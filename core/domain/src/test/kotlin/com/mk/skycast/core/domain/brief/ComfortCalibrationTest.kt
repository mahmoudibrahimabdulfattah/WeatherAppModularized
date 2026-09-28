package com.mk.skycast.core.domain.brief

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.ComfortFeedback
import com.mk.skycast.core.model.ComfortVote
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.testing.FakeComfortRepository
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeTimeTicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ComfortCalibrationTest {

    private val day = LocalDate.of(2026, 9, 28)
    private fun votes(vararg votes: ComfortVote) =
        votes.mapIndexed { i, v -> ComfortFeedback(day.minusDays(i.toLong()), v) }

    @Test
    fun `no votes means no adjustment`() {
        assertThat(ComfortCalibration.offsetC(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `feeling cold makes advice warmer, feeling hot lighter`() {
        assertThat(
            ComfortCalibration.offsetC(votes(ComfortVote.COLD, ComfortVote.COLD, ComfortVote.COLD)),
        ).isLessThan(-2.0)
        assertThat(
            ComfortCalibration.offsetC(votes(ComfortVote.HOT, ComfortVote.HOT, ComfortVote.HOT)),
        ).isGreaterThan(2.0)
    }

    @Test
    fun `a single vote only nudges, and the offset stays bounded`() {
        assertThat(ComfortCalibration.offsetC(votes(ComfortVote.COLD))).isWithin(0.01).of(-1.0)
        assertThat(ComfortCalibration.offsetC(votes(*Array(10) { ComfortVote.COLD }))).isAtLeast(-6.0)
    }

    @Test
    fun `recent votes outweigh older ones`() {
        val offset = ComfortCalibration.offsetC(votes(ComfortVote.HOT, ComfortVote.COLD, ComfortVote.COLD))
        assertThat(
            offset,
        ).isGreaterThan(ComfortCalibration.offsetC(votes(ComfortVote.COLD, ComfortVote.COLD, ComfortVote.HOT)))
    }

    @Test
    fun `prompt appears after the last time out and disappears once answered`() = runTest {
        val routines = FakeRoutineRepository(Routine(isConfigured = true)) // Monday: commute back 17:00–17:30
        val comfort = FakeComfortRepository()
        val ticker = FakeTimeTicker(Instant.parse("2026-09-28T16:00:00Z"))
        val useCase = ObserveComfortPromptUseCase(routines, comfort, ticker)

        useCase(ZoneOffset.UTC).test {
            assertThat(awaitItem()).isNull()
            ticker.now.value = Instant.parse("2026-09-28T18:00:00Z")
            assertThat(awaitItem()).isEqualTo(day)
            comfort.record(day, ComfortVote.RIGHT)
            assertThat(awaitItem()).isNull()
        }
    }
}
