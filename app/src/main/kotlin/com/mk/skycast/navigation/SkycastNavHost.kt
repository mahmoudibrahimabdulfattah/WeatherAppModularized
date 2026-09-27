package com.mk.skycast.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mk.skycast.feature.home.navigation.HomeDestination
import com.mk.skycast.feature.home.navigation.homeScreen
import com.mk.skycast.feature.places.navigation.PlacesDestination
import com.mk.skycast.feature.places.navigation.placesScreen
import com.mk.skycast.feature.routine.navigation.RoutineDestination
import com.mk.skycast.feature.routine.navigation.routineScreen
import com.mk.skycast.feature.settings.navigation.SettingsDestination
import com.mk.skycast.feature.settings.navigation.settingsScreen

/** The app module is the only place that knows about every feature and wires them together. */
@Composable
fun SkycastNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeDestination) {
        homeScreen(
            onOpenPlaces = { navController.navigate(PlacesDestination) { launchSingleTop = true } },
            onOpenSettings = { navController.navigate(SettingsDestination) { launchSingleTop = true } },
        )
        placesScreen(onBack = { navController.popBackStack() })
        settingsScreen(
            onBack = { navController.popBackStack() },
            onOpenRoutine = { navController.navigate(RoutineDestination) { launchSingleTop = true } },
        )
        routineScreen(onClose = { navController.popBackStack() })
    }
}
