package com.mk.skycast.feature.routine.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mk.skycast.core.designsystem.theme.SkycastTheme
import com.mk.skycast.core.model.Commute
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.feature.routine.RoutineState
import com.mk.skycast.feature.routine.RoutineStep
import java.time.DayOfWeek
import java.time.LocalTime

private val previewRoutine = Routine(
    commute = Commute(mode = TravelMode.CAR),
    outings = listOf(
        Outing(
            id = "gym",
            kind = OutingKind.GYM,
            customLabel = null,
            days = setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            departAt = LocalTime.of(19, 0),
            returnAt = LocalTime.of(21, 0),
            mode = TravelMode.CAR,
            setting = OutingSetting.INDOORS,
        ),
    ),
)

private val weekState = RoutineState(isLoading = false, draft = previewRoutine)
private val tripsState = weekState.copy(step = RoutineStep.TRIPS)
private val briefState = weekState.copy(step = RoutineStep.BRIEF)

@Preview(name = "Routine · week", widthDp = 393, heightDp = 852)
@Composable
private fun RoutineWeekPreview() = SkycastTheme { RoutineScreen(weekState, onIntent = {}) }

@Preview(name = "Routine · week dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 393, heightDp = 852)
@Composable
private fun RoutineWeekDarkPreview() = SkycastTheme(darkTheme = true) { RoutineScreen(weekState, onIntent = {}) }

@Preview(name = "Routine · trips Arabic", locale = "ar", widthDp = 393, heightDp = 852)
@Composable
private fun RoutineTripsArabicPreview() = SkycastTheme { RoutineScreen(tripsState, onIntent = {}) }

@Preview(name = "Routine · brief", widthDp = 393, heightDp = 852)
@Composable
private fun RoutineBriefPreview() = SkycastTheme { RoutineScreen(briefState, onIntent = {}) }
