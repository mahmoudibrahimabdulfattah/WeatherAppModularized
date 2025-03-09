package com.example.forecast

import com.example.data.models.ForecastDay

data class ForecastState(
    val isLoading: Boolean = false,
    val forecast: List<ForecastDay> = emptyList(),
    val error: String? = null,
    val cityName: String = ""
)