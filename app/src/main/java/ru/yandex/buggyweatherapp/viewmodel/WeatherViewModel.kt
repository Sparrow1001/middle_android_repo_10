package ru.yandex.buggyweatherapp.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.WeatherApplication
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import ru.yandex.buggyweatherapp.repository.LocationRepository
import ru.yandex.buggyweatherapp.repository.WeatherRepository
import ru.yandex.buggyweatherapp.utils.ImageLoader
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: IWeatherRepository,
    private val locationRepository: ILocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _locationState = MutableStateFlow<Location?>(null)
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()

    private val _cityNameState = MutableStateFlow<String?>(null)
    val cityNameState: StateFlow<String?> = _cityNameState.asStateFlow()
    
    
    val weatherData = MutableLiveData<WeatherData>()
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()

    private var refreshTimer: Timer? = null
    
    init {
        fetchCurrentLocationWeather()
        startAutoRefresh()
    }
    
    
    fun fetchCurrentLocationWeather() {
        isLoading.value = true
        error.value = null

        viewModelScope.launch {
            locationRepository.getCurrentLocation().fold(
                onSuccess = { location ->
                    _locationState.value = location

                    locationRepository.getCityNameFromLocation(location).fold(
                        onSuccess = { cityName ->
                            _cityNameState.value = cityName
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error getting city name", error)
                        }
                    )

                    getWeatherForLocation(location)
                },
                onFailure = {
                    isLoading.value = false
                    error.value = "Unable to get current location"
                }
            )
        }


    }
    
    fun getWeatherForLocation(location: Location) {
        isLoading.value = true
        error.value = null

        viewModelScope.launch {
            weatherRepository.getWeatherData(location).fold(
                onSuccess = { data ->
                    weatherData.value = data
                    isLoading.value = false
                },
                onFailure = { er ->
                    error.value = er.message
                    isLoading.value = false
                }
            )
        }
    }
    
    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = "City name cannot be empty"
            return
        }
        
        isLoading.value = true
        error.value = null

        viewModelScope.launch {
            weatherRepository.getWeatherByCity(city).fold(
                onSuccess = { data ->
                    isLoading.value = false

                    weatherData.value = data
                    _cityNameState.value = data.cityName
                    _locationState.value = Location(0.0, 0.0, data.cityName)
                },
                onFailure = { er ->
                    isLoading.value = false

                    error.value = er.message
                }
            )
        }
    }
    
    
    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }
    
    
    fun loadWeatherIcon(iconCode: String) {
        viewModelScope.launch {
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            ImageLoader.loadImage(iconUrl)
        }
    }
    
    
    private fun startAutoRefresh() {
        refreshTimer = Timer()
        refreshTimer?.schedule(object : TimerTask() {
            override fun run() {
                _locationState.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }, 60000, 60000)
    }
    
    
    fun toggleFavorite() {
        weatherData.value?.let {
            it.isFavorite = !it.isFavorite
            
            weatherData.value = it
        }
    }
    
    
    override fun onCleared() {
        super.onCleared()
        
    }
}