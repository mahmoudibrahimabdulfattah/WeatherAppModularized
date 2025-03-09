package com.example.forecast

import com.example.core.Result
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.domain.GetForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val getForecastUseCase: GetForecastUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    fun processIntent(intent: ForecastIntent) {
        when (intent) {
            is ForecastIntent.LoadForecast -> loadForecast(intent.cityName)
            is ForecastIntent.RefreshForecast -> refreshForecast()
        }
    }

    private fun loadForecast(cityName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                cityName = cityName,
                error = null
            )

            when (val result = getForecastUseCase(cityName)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        forecast = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load forecast: ${result.exception.message}"
                    )
                }
                Result.Loading -> {
                    _state.value = _state.value.copy(
                        isLoading = true,
                        error = null
                    )
                }
            }
        }
    }

    private fun refreshForecast() {
        if (_state.value.cityName.isNotEmpty()) {
            loadForecast(_state.value.cityName)
        }
    }
}