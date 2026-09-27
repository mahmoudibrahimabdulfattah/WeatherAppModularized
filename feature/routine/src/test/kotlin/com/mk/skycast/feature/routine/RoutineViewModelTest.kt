package com.mk.skycast.feature.routine

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.SaveRoutineUseCase
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.testing.FakeRoutineRepository
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RoutineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routines = FakeRoutineRepository()
    private val vm by lazy {
        RoutineViewModel(
            ObserveRoutineUseCase(routines),
            ObserveUserPreferencesUseCase(FakeUserPreferencesRepository()),
            SaveRoutineUseCase(routines),
        )
    }

    @Test
    fun `steps advance and go back, and back on the first step closes`() = runTest {
        vm.onIntent(RoutineIntent.DayTypeChanged(DayOfWeek.SUNDAY, DayType.HOME))
        assertThat(vm.state.value.draft.week[DayOfWeek.SUNDAY]).isEqualTo(DayType.HOME)

        vm.onIntent(RoutineIntent.NextClicked)
        assertThat(vm.state.value.step).isEqualTo(RoutineStep.TRIPS)

        vm.onIntent(RoutineIntent.BackClicked)
        assertThat(vm.state.value.step).isEqualTo(RoutineStep.WEEK)

        vm.effects.test {
            vm.onIntent(RoutineIntent.BackClicked)
            assertThat(awaitItem()).isEqualTo(RoutineEffect.Close)
        }
    }

    @Test
    fun `transport is required only when some day is away`() = runTest {
        vm.onIntent(RoutineIntent.NextClicked)
        vm.onIntent(RoutineIntent.NextClicked)
        assertThat(vm.state.value.step).isEqualTo(RoutineStep.TRIPS)
        assertThat(vm.state.value.showCommuteModeError).isTrue()

        vm.onIntent(RoutineIntent.TravelModeChanged(TravelMode.PUBLIC_TRANSPORT))
        vm.onIntent(RoutineIntent.NextClicked)
        assertThat(vm.state.value.step).isEqualTo(RoutineStep.BRIEF)
    }

    @Test
    fun `a week without away days hides the commute and skips its validation`() = runTest {
        DayOfWeek.entries.forEach { vm.onIntent(RoutineIntent.DayTypeChanged(it, DayType.HOME)) }
        assertThat(vm.state.value.showsCommute).isFalse()

        vm.onIntent(RoutineIntent.NextClicked)
        vm.onIntent(RoutineIntent.NextClicked)

        assertThat(vm.state.value.step).isEqualTo(RoutineStep.BRIEF)
    }

    @Test
    fun `outings can be added, edited and deleted`() = runTest {
        vm.onIntent(RoutineIntent.AddOutingClicked)
        val draft = vm.state.value.outingEditor!!
        assertThat(vm.state.value.canSaveOuting).isFalse() // no days yet

        vm.onIntent(RoutineIntent.OutingDraftChanged(draft.copy(days = setOf(DayOfWeek.FRIDAY))))
        vm.onIntent(RoutineIntent.SaveOutingClicked)
        assertThat(vm.state.value.draft.outings).hasSize(1)
        assertThat(vm.state.value.outingEditor).isNull()

        vm.onIntent(RoutineIntent.EditOutingClicked(draft.id))
        vm.onIntent(RoutineIntent.OutingDraftChanged(vm.state.value.outingEditor!!.copy(kind = OutingKind.CUSTOM)))
        assertThat(vm.state.value.canSaveOuting).isFalse() // custom needs a name
        vm.onIntent(
            RoutineIntent.OutingDraftChanged(vm.state.value.outingEditor!!.copy(customLabel = "Football")),
        )
        vm.onIntent(RoutineIntent.SaveOutingClicked)
        assertThat(vm.state.value.draft.outings.single().customLabel).isEqualTo("Football")

        vm.onIntent(RoutineIntent.DeleteOutingClicked(draft.id))
        assertThat(vm.state.value.draft.outings).isEmpty()
    }

    @Test
    fun `saving on the last step persists a configured routine and closes`() = runTest {
        vm.onIntent(RoutineIntent.TravelModeChanged(TravelMode.CAR))
        vm.onIntent(RoutineIntent.BriefTimeChanged(LocalTime.of(20, 0)))
        vm.onIntent(RoutineIntent.NextClicked)
        vm.onIntent(RoutineIntent.NextClicked)

        vm.effects.test {
            vm.onIntent(RoutineIntent.NextClicked)
            assertThat(awaitItem()).isEqualTo(RoutineEffect.Saved)
        }
        assertThat(routines.current.isConfigured).isTrue()
        assertThat(routines.current.briefTime).isEqualTo(LocalTime.of(20, 0))
        assertThat(routines.current.commute.mode).isEqualTo(TravelMode.CAR)
    }

    @Test
    fun `an existing routine is loaded for editing`() = runTest {
        routines.save(Routine(isConfigured = true, morningRefresh = true))
        assertThat(vm.state.value.isEditingExisting).isTrue()
        assertThat(vm.state.value.draft.morningRefresh).isTrue()
    }
}
