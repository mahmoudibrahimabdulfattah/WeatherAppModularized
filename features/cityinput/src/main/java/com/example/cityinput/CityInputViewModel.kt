package com.example.cityinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.domain.GetLastSearchedCityUseCase
import com.example.data.domain.SaveLastSearchedCityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityInputViewModel @Inject constructor(
    private val getLastSearchedCityUseCase: GetLastSearchedCityUseCase,
    private val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase
) : ViewModel() {

    private val _cityState = MutableStateFlow("")
    val cityState: StateFlow<String> = _cityState.asStateFlow()

    private val _eventFlow = MutableStateFlow<CityInputEvent?>(null)
    val eventFlow: StateFlow<CityInputEvent?> = _eventFlow.asStateFlow()

    init {
        loadLastSearchedCity()
    }

    fun updateCity(city: String) {
        _cityState.value = city
    }

    fun searchCity() {
        val cityName = _cityState.value.trim()
        if (cityName.isNotEmpty()) {
            viewModelScope.launch {
                saveLastSearchedCityUseCase(cityName)
                _eventFlow.value = CityInputEvent.NavigateToWeather(cityName)
            }
        } else {
            _eventFlow.value = CityInputEvent.ShowError("City name cannot be empty")
        }
    }

    fun clearEvent() {
        _eventFlow.value = null
    }

    private fun loadLastSearchedCity() {
        viewModelScope.launch {
            getLastSearchedCityUseCase().collect { city ->
                _cityState.value = city
                if (city.isNotEmpty()) {
                    _eventFlow.value = CityInputEvent.NavigateToWeather(city)
                }
            }
        }
    }

    sealed class CityInputEvent {
        data class NavigateToWeather(val cityName: String) : CityInputEvent()
        data class ShowError(val message: String) : CityInputEvent()
    }
}