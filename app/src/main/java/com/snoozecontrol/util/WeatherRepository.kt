package com.snoozecontrol.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.snoozecontrol.api.KtorWeatherApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class WeatherInfo(
    val temperatureCelsius: Int,
    val conditionText: String,
    val isSuccess: Boolean = true
)

object WeatherRepository {

    private suspend fun getFreshLocation(context: Context): Location? {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.e("WeatherService", "Location permissions are not granted.")
            return null
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        return try {
            suspendCancellableCoroutine { continuation ->
                // Cancel Google Play Services location task if coroutine is cancelled
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }.addOnFailureListener { exception ->
                    Log.e("WeatherService", "Failed to fetch current location", exception)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Exception while requesting location fix", e)
            null
        }
    }

    suspend fun fetchCurrentWeather(context: Context): WeatherInfo = withContext(Dispatchers.IO) {
        var lat = 40.7128 // Default New York latitude
        var lon = -74.0060 // Default New York longitude

        try {
            // 1. Fetch fresh location fix if permissions are granted
            val currentLocation = getFreshLocation(context)
            if (currentLocation != null) {
                lat = currentLocation.latitude
                lon = currentLocation.longitude
            } else {
                Log.w(
                    "WeatherService",
                    "Could not acquire location fix. Falling back to default coordinates."
                )
            }

            // 2. Query weather data via Ktor API Client
            val dto = KtorWeatherApiClient.fetchWeather(lat, lon)
            if (dto?.current != null) {
                val temp = dto.current.temperature2m.roundToInt()
                val code = dto.current.weatherCode
                val condition = mapWeatherCodeToText(code)
                return@withContext WeatherInfo(
                    temperatureCelsius = temp,
                    conditionText = condition,
                    isSuccess = true
                )
            }
        } catch (e: Throwable) {
            Log.e("WeatherService", "Network or location error occurred during weather fetch", e)
        }

        // Fallback response on network or parsing failure
        WeatherInfo(
            temperatureCelsius = 22,
            conditionText = "Partly Cloudy",
            isSuccess = false
        )
    }


    private fun mapWeatherCodeToText(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Sunny"
        }
    }
}
