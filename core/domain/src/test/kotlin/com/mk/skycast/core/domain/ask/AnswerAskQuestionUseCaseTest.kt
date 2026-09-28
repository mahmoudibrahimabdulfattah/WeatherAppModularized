package com.mk.skycast.core.domain.ask

import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.testing.FakeComfortRepository
import com.mk.skycast.core.testing.FakeLocationRepository
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.FakeWeatherRepository
import com.mk.skycast.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnswerAskQuestionUseCaseTest {

    private val routines = FakeRoutineRepository()
    private val locations = FakeLocationRepository(
        listOf(TestData.location(1), TestData.location(2, "Alexandria", sortOrder = 1)),
    )
    private val weather = FakeWeatherRepository()
    private val preferences = FakeUserPreferencesRepository(UserPreferences(selectedLocationId = 2))
    private val useCase = AnswerAskQuestionUseCase(routines, locations, weather, preferences, FakeComfortRepository())

    @Test
    fun `non-routine questions use selected location when routine is not configured`() = runTest {
        weather.emit(TestData.weather(locationId = 2))

        val answer = useCase(AskQuestion.RAIN_NEXT_DAYS, now = TestData.NOW)

        assertThat(answer).isInstanceOf(AskAnswer.Rain::class.java)
    }

    @Test
    fun `what to wear asks for routine setup when routine is not configured`() = runTest {
        weather.emit(TestData.weather(locationId = 2))

        val answer = useCase(AskQuestion.WHAT_TO_WEAR, now = TestData.NOW)

        assertThat(answer).isInstanceOf(AskAnswer.RoutineNeeded::class.java)
    }
}
