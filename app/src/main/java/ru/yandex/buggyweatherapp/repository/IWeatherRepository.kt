package ru.yandex.buggyweatherapp.repository

import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData

// интерфейс для следования принципам solid и удобного тестирования
interface IWeatherRepository {
    suspend fun getWeatherData(location: Location): Result<WeatherData>
    suspend fun getWeatherByCity(cityName: String): Result<WeatherData>
}