package com.example.currentweather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ErrorMessage
import com.example.core.components.LoadingIndicator
import com.example.weather_library_formatter.WeatherFormatter
import com.example.weather_library_formatter.WeatherIcons

@Composable
fun CurrentWeatherScreen(
    cityName: String,
    viewModel: CurrentWeatherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cityName) {
        viewModel.loadWeather(cityName)
    }

    when (val currentState = state) {
        is CurrentWeatherViewModel.CurrentWeatherState.Loading -> {
            LoadingIndicator()
        }
        is CurrentWeatherViewModel.CurrentWeatherState.Error -> {
            ErrorMessage(currentState.message)
        }
        is CurrentWeatherViewModel.CurrentWeatherState.Success -> {
            val weather = currentState.weather

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = weather.cityName,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = WeatherIcons.getWeatherIcon(weather.weatherConditionCode),
                            contentDescription = weather.weatherDescription,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.size(16.dp))

                        Text(
                            text = WeatherFormatter.formatTemperature(weather.temperature),
                            style = MaterialTheme.typography.displayMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = WeatherFormatter.getWeatherCondition(weather.weatherConditionCode),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Humidity",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = WeatherFormatter.formatHumidity(weather.humidity),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Wind",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = WeatherFormatter.formatWindSpeed(weather.windSpeed),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}