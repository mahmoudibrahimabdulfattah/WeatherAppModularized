package com.mk.skycast.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.model.Commute
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RoutineDataSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val today = LocalDate.of(2026, 9, 27)
    private val clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC)

    private fun TestScope.dataSource(file: File = tmp.newFile("prefs.preferences_pb")): Pair<RoutineDataSource, File> {
        file.delete()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + backgroundScope.coroutineContext)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        return RoutineDataSource(store, clock) to file
    }

    private val gym = Outing(
        id = "gym",
        kind = OutingKind.GYM,
        customLabel = null,
        days = setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY),
        departAt = LocalTime.of(19, 0),
        returnAt = LocalTime.of(21, 30),
        mode = TravelMode.WALK,
        setting = OutingSetting.INDOORS,
    )

    @Test
    fun `missing routine falls back to defaults`() = runTest {
        val (source) = dataSource()
        assertThat(source.routine.first()).isEqualTo(Routine())
    }

    @Test
    fun `routine round-trips through json`() = runTest {
        val (source) = dataSource()
        val routine = Routine(
            isConfigured = true,
            week = Routine().week + (DayOfWeek.MONDAY to DayType.HOME),
            commute = Commute(LocalTime.of(22, 0), LocalTime.of(6, 30), returnsNextDay = true, 45, TravelMode.CAR),
            outings = listOf(gym),
            briefTime = LocalTime.of(20, 15),
            morningRefresh = true,
            locationId = 7,
        )

        source.save(routine)

        assertThat(source.routine.first()).isEqualTo(routine)
    }

    @Test
    fun `overrides replace same date and prune past dates`() = runTest {
        val (source) = dataSource()
        source.setOverride(DayPlanOverride(today.minusDays(1), dayType = DayType.OFF))
        source.setOverride(DayPlanOverride(today.plusDays(1), dayType = DayType.HOME))
        source.setOverride(
            DayPlanOverride(
                today.plusDays(1),
                dayType = DayType.AWAY,
                addedOutings = listOf(gym),
                cancelledOutingIds = setOf("x"),
            ),
        )

        val overrides = source.overrides.first()

        assertThat(overrides).containsExactly(
            DayPlanOverride(today.plusDays(1), DayType.AWAY, listOf(gym), setOf("x")),
        )

        source.clearOverride(today.plusDays(1))
        assertThat(source.overrides.first()).isEmpty()
    }

    @Test
    fun `corrupt json falls back to defaults instead of crashing`() = runTest {
        val file = tmp.newFile("corrupt.preferences_pb").also { it.delete() }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + backgroundScope.coroutineContext)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        store.edit { it[stringPreferencesKey("routine_json")] = "{not json" }

        assertThat(RoutineDataSource(store, clock).routine.first()).isEqualTo(Routine())
    }
}
