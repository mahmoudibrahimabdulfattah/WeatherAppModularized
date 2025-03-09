package com.example.data.models

data class ForecastDay(
    val date: String,
    val temperature: Double,
    val weatherConditionCode: Int,
    val weatherDescription: String,
    val humidity: Int,
    val windSpeed: Double
)
