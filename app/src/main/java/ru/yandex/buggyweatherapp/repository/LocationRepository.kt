package ru.yandex.buggyweatherapp.repository

import android.app.Application
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.utils.LocationTracker
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/*
- подключение hilt
- замена callback на корутины
- результат через Result
- получение локации через continuation
- отписка от трекинга
 */

@Singleton
class LocationRepository @Inject constructor(
    private val application: Application
) : ILocationRepository {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)
    
    
    private var currentLocation: Location? = null


    private var locationCallback: LocationCallback? = null


    override suspend fun getCurrentLocation(): Result<Location> =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val userLocation = Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            currentLocation = userLocation
                            continuation.resume(Result.success(userLocation))
                        } else {
                            requestNewLocation(continuation)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationRepository", "Error getting location", e)
                        continuation.resume(Result.failure(e))
                    }

                continuation.invokeOnCancellation {
                    locationCallback?.let { callback ->
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("LocationRepository", "Location permission not granted", e)
                continuation.resume(Result.failure(e))
            }
        }


    private fun requestNewLocation(continuation: kotlinx.coroutines.CancellableContinuation<Result<Location>>) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build()
            
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation

                        stopLocationTracking()

                        if (continuation.isActive) {
                            continuation.resume(Result.success(userLocation))
                        }
                    }
                }
            }
            
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationRepository", "Location permission not granted", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    override suspend fun getCityNameFromLocation(location: Location): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(application, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val cityName = when {
                        address.locality != null -> address.locality
                        address.subAdminArea != null -> address.subAdminArea
                        else -> address.adminArea
                    }
                    Result.success(cityName)
                } else {
                    Result.failure(Exception("addresses is null or empty"))
                }
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error getting city name", e)
                Result.failure(e)
            }
        }

    override fun startLocationTracking() {

    }

    override fun stopLocationTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
}