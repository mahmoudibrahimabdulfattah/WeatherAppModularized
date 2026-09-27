package com.mk.skycast.core.domain.brief

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.BriefFingerprint
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.testing.FakeBriefHistoryRepository
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.TestData
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BriefNotificationTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private val routine = Routine(isConfigured = true, commute = Routine().commute.copy(mode = TravelMode.CAR))

    // --- scheduling -------------------------------------------------------------------------

    @Test
    fun `nightly runs today if the time is still ahead, otherwise tomorrow`() {
        val afternoon = Instant.parse("2026-09-27T12:00:00Z") // 15:00 Cairo
        val late = Instant.parse("2026-09-27T19:00:00Z") // 22:00 Cairo

        assertThat(
            BriefSchedule.nextNightly(routine, afternoon, cairo),
        ).isEqualTo(Instant.parse("2026-09-27T18:00:00Z"))
        assertThat(BriefSchedule.nextNightly(routine, late, cairo)).isEqualTo(Instant.parse("2026-09-28T18:00:00Z"))
    }

    @Test
    fun `morning check is 90 minutes before leaving and skipped on days without outings`() {
        val withRefresh = routine.copy(morningRefresh = true)
        val saturdayNight = Instant.parse("2026-09-26T19:00:00Z") // Sat 22:00; Sunday is a work day

        val check = BriefSchedule.nextMorningCheck(withRefresh, emptyList(), saturdayNight, cairo)

        assertThat(check).isEqualTo(Instant.parse("2026-09-27T03:30:00Z")) // Sun 06:30 Cairo
        val offSunday = listOf(DayPlanOverride(LocalDate.of(2026, 9, 27), dayType = DayType.OFF))
        // Nothing to check before the next nightly run; that run schedules the following morning.
        assertThat(BriefSchedule.nextMorningCheck(withRefresh, offSunday, saturdayNight, cairo)).isNull()
    }

    @Test
    fun `no morning check when disabled or when the brief is already a morning one`() {
        val now = Instant.parse("2026-09-27T12:00:00Z")
        assertThat(BriefSchedule.nextMorningCheck(routine, emptyList(), now, cairo)).isNull()
        val morningBrief = routine.copy(morningRefresh = true, briefTime = LocalTime.of(6, 0))
        assertThat(BriefSchedule.nextMorningCheck(morningBrief, emptyList(), now, cairo)).isNull()
    }

    // --- deciding what to notify ----------------------------------------------------------

    private val routines = FakeRoutineRepository(routine)
    private val weather = FakeWeatherRepository()
    private val history = FakeBriefHistoryRepository()
    private val useCase = PrepareBriefNotificationUseCase(
        routines,
        FakeLocationRepository(listOf(TestData.location(1))),
        weather,
        history,
    )

    private fun rainyWeather(): Weather = TestData.weather(1).let { w ->
        w.copy(hourly = w.hourly.map { it.copy(precipitationProbability = 90, precipitationMm = 2.0) })
    }

    @Test
    fun `nightly always notifies and remembers what it said`() = runTest {
        val night = Instant.parse("2026-09-27T18:00:00Z")

        val notice = useCase(BriefRun.NIGHTLY, night)

        assertThat(notice?.isUpdate).isFalse()
        assertThat(notice?.brief?.date).isEqualTo(LocalDate.of(2026, 9, 28))
        assertThat(history.last?.date).isEqualTo(LocalDate.of(2026, 9, 28))
    }

    @Test
    fun `morning stays silent when nothing to act on changed`() = runTest {
        useCase(BriefRun.NIGHTLY, Instant.parse("2026-09-27T18:00:00Z"))

        assertThat(useCase(BriefRun.MORNING, Instant.parse("2026-09-28T03:30:00Z"))).isNull()
    }

    @Test
    fun `morning notifies an update when rain appears on the route`() = runTest {
        useCase(BriefRun.NIGHTLY, Instant.parse("2026-09-27T18:00:00Z"))
        weather.weatherFactory = { rainyWeather() }

        val notice = useCase(BriefRun.MORNING, Instant.parse("2026-09-28T03:30:00Z"))

        assertThat(notice?.isUpdate).isTrue()
        assertThat(history.last?.keys).contains("carry:UMBRELLA")
    }

    @Test
    fun `morning without a nightly brief for that day stays silent`() = runTest {
        history.last = BriefFingerprint(LocalDate.of(2026, 9, 20), emptySet())
        weather.weatherFactory = { rainyWeather() }

        assertThat(useCase(BriefRun.MORNING, Instant.parse("2026-09-28T03:30:00Z"))).isNull()
    }
}
