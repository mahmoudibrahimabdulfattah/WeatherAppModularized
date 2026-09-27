package com.mk.skycast.feature.routine.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mk.skycast.feature.routine.ui.RoutineRoute
import kotlinx.serialization.Serializable

@Serializable
data object RoutineDestination

fun NavGraphBuilder.routineScreen(onClose: () -> Unit) {
    composable<RoutineDestination> {
        RoutineRoute(onClose = onClose)
    }
}
