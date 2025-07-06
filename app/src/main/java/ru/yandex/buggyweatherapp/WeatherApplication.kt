package ru.yandex.buggyweatherapp

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import ru.yandex.buggyweatherapp.utils.ImageLoader
import ru.yandex.buggyweatherapp.utils.LocationTracker

@HiltAndroidApp
class WeatherApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        ImageLoader.initialize(this)
        LocationTracker.getInstance(this)
    }
}