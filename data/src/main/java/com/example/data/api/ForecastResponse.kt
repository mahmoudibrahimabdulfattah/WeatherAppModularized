package com.example.data.api

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    @SerializedName("list")
    val list: List<ForecastItem>,
    @SerializedName("city")
    val city: City
) {
    data class ForecastItem(
        @SerializedName("dt_txt")
        val dateText: String,
        @SerializedName("main")
        val main: Main,
        @SerializedName("weather")
        val weather: List<Weather>,
        @SerializedName("wind")
        val wind: Wind
    )

    data class Main(
        @SerializedName("temp")
        val temperature: Double,
        @SerializedName("humidity")
        val humidity: Int
    )

    data class Weather(
        @SerializedName("id")
        val id: Int,
        @SerializedName("description")
        val description: String
    )

    data class Wind(
        @SerializedName("speed")
        val speed: Double
    )

    data class City(
        @SerializedName("name")
        val name: String
    )
}