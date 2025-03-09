package com.example.data.models

data class CurrentWeather(
    val cityName: String,
    val temperature: Double,
    val weatherConditionCode: Int,
    val weatherDescription: String,
    val humidity: Int,
    val windSpeed: Double,
    val timestamp: Long
)