package com.mk.skycast.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mk.skycast.feature.home.ui.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoutine: () -> Unit,
    openPlans: Boolean,
    onConsumeOpenPlans: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onOpenPlaces = onOpenPlaces,
            onOpenSettings = onOpenSettings,
            onOpenRoutine = onOpenRoutine,
            openPlans = openPlans,
            onConsumeOpenPlans = onConsumeOpenPlans,
        )
    }
}
