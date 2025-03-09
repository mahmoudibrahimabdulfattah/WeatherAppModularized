package com.example.weathertaskmodularized

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cityinput.CityInputScreen
import com.example.currentweather.CurrentWeatherScreen
import com.example.forecast.ForecastScreen
import com.example.weathertaskmodularized.ui.theme.WeatherTaskModularizedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherTaskModularizedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp()
                }
            }
        }
    }
}

@Composable
fun WeatherApp() {
    val navController = rememberNavController()

    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.CityInput.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.CityInput.route) {
                CityInputScreen(
                    onNavigateToWeather = { cityName ->
                        navController.navigate(Screen.WeatherDetails.createRoute(cityName))
                    }
                )
            }

            composable(
                route = Screen.WeatherDetails.route,
                arguments = listOf(
                    navArgument("cityName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cityName = backStackEntry.arguments?.getString("cityName") ?: ""
                var currentTab by remember { mutableStateOf(0) }

                Column {
                    CurrentWeatherScreen(cityName = cityName)
                    Spacer(Modifier.height(24.dp))
                    ForecastScreen(cityName = cityName)
                }
            }
        }
    }
}