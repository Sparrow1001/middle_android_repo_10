package ru.yandex.buggyweatherapp.repository

import ru.yandex.buggyweatherapp.model.Location

// интерфейс для следования принципам solid и удобного тестирования
interface ILocationRepository {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun getCityNameFromLocation(location: Location): Result<String?>
    fun startLocationTracking()
    fun stopLocationTracking()
}