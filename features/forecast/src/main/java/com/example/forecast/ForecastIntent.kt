package com.example.forecast

sealed class ForecastIntent {
    data class LoadForecast(val cityName: String) : ForecastIntent()
    object RefreshForecast : ForecastIntent()
}