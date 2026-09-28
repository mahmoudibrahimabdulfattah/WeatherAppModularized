package com.mk.skycast.core.domain.brief

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.testing.FakeComfortRepository
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeTimeTicker
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.TestData
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveDailyBriefUseCaseTest {

    private val routines = FakeRoutineRepository()
    private val locations =
        FakeLocationRepository(listOf(TestData.location(1), TestData.location(2, "Giza", sortOrder = 1)))
    private val weather = FakeWeatherRepository()
    private val ticker = FakeTimeTicker()
    private val useCase = ObserveDailyBriefUseCase(routines, locations, weather, FakeComfortRepository(), ticker)

    @Test
    fun `nothing is built until the routine is set up`() = runTest {
        weather.emit(TestData.weather(1))

        useCase().test {
            assertThat(awaitItem()).isNull()

            routines.save(Routine(isConfigured = true))
            val brief = awaitItem()
            assertThat(brief?.locationId).isEqualTo(1)
            // TestData.NOW is 13:15 in Cairo, so the brief is already about tomorrow.
            assertThat(brief?.date).isEqualTo(LocalDate.of(2026, 9, 28))
        }
    }

    @Test
    fun `the routine's own location wins over the first saved place`() = runTest {
        weather.emit(TestData.weather(2))
        routines.save(Routine(isConfigured = true, locationId = 2))

        useCase().test {
            assertThat(awaitItem()?.locationId).isEqualTo(2)
        }
    }
}
