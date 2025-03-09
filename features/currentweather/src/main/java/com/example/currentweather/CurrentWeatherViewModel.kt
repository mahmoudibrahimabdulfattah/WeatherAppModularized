package com.example.currentweather

import com.example.core.Result
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.domain.GetCurrentWeatherUseCase
import com.example.data.models.CurrentWeather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val getCurrentWeatherUseCase: GetCurrentWeatherUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<CurrentWeatherState>(CurrentWeatherState.Loading)
    val state: StateFlow<CurrentWeatherState> = _state.asStateFlow()

    fun loadWeather(cityName: String) {
        viewModelScope.launch {
            _state.value = CurrentWeatherState.Loading
            when (val result = getCurrentWeatherUseCase(cityName)) {
                is Result.Success -> {
                    _state.value = CurrentWeatherState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = CurrentWeatherState.Error("Failed to load weather data: ${result.exception.message}")
                }
                Result.Loading -> {
                    _state.value = CurrentWeatherState.Loading
                }
            }
        }
    }

    sealed class CurrentWeatherState {
        object Loading : CurrentWeatherState()
        data class Success(val weather: CurrentWeather) : CurrentWeatherState()
        data class Error(val message: String) : CurrentWeatherState()
    }
}