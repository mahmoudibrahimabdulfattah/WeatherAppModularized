package com.example.weathertaskmodularized


sealed class Screen(val route: String) {
    object CityInput : Screen("city_input")
    object WeatherDetails : Screen("weather_details/{cityName}") {
        fun createRoute(cityName: String): String {
            return "weather_details/$cityName"
        }
    }
}