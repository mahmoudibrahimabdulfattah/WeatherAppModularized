package com.example.cityinput

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityInputScreen(
    viewModel: CityInputViewModel = hiltViewModel(),
    onNavigateToWeather: (String) -> Unit
) {
    val city by viewModel.cityState.collectAsStateWithLifecycle()
    val event by viewModel.eventFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        when (val currentEvent = event) {
            is CityInputViewModel.CityInputEvent.NavigateToWeather -> {
                onNavigateToWeather(currentEvent.cityName)
                viewModel.clearEvent()
            }
            is CityInputViewModel.CityInputEvent.ShowError -> {
                snackbarHostState.showSnackbar(currentEvent.message)
                viewModel.clearEvent()
            }
            null -> { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Now & Later") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter City Name",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { viewModel.updateCity(it) },
                    label = { Text("City") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.searchCity() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Search Weather")
                }
            }
        }
    }
}