package com.mk.skycast.feature.places.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mk.skycast.feature.places.ui.PlacesRoute
import kotlinx.serialization.Serializable

@Serializable
data object PlacesDestination

fun NavGraphBuilder.placesScreen(onBack: () -> Unit) {
    composable<PlacesDestination> {
        PlacesRoute(onBack = onBack)
    }
}
