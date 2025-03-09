package com.example.forecast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    cityName: String,
    viewModel: ForecastViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cityName) {
        viewModel.processIntent(ForecastIntent.LoadForecast(cityName))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("7-Day Forecast") },
                actions = {
                    IconButton(onClick = { viewModel.processIntent(ForecastIntent.RefreshForecast) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingIndicator()
                }

                state.error != null -> {
                    ErrorMessage(state.error!!)
                }

                state.forecast.isNotEmpty() -> {
                    LazyRow {
                        items(state.forecast) { forecastDay ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = WeatherFormatter.formatDate(forecastDay.date),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = WeatherIcons.getWeatherIcon(forecastDay.weatherConditionCode),
                                            contentDescription = forecastDay.weatherDescription,
                                            modifier = Modifier.size(48.dp)
                                        )

                                        Spacer(modifier = Modifier.size(16.dp))

                                        Column {
                                            Text(
                                                text = WeatherFormatter.formatTemperature(forecastDay.temperature),
                                                style = MaterialTheme.typography.headlineSmall
                                            )

                                            Text(
                                                text = WeatherFormatter.getWeatherCondition(forecastDay.weatherConditionCode),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}