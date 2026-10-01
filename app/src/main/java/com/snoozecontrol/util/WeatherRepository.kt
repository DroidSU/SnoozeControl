package com.snoozecontrol.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.snoozecontrol.api.KtorWeatherApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class WeatherInfo(
    val temperatureCelsius: Int,
    val conditionText: String,
    val isSuccess: Boolean = true,
)

object WeatherRepository {

    const val CACHE_TTL_MILLIS = 60 * 60 * 1000L // 1 hour TTL
    private const val PREFS_NAME = "weather_cache_prefs"
    private const val KEY_TEMP = "cached_temp"
    private const val KEY_CONDITION = "cached_condition"
    private const val KEY_TIMESTAMP = "cached_timestamp"
    private const val KEY_IS_DEFAULT_LOC = "cached_is_default_loc"

    private data class CachedWeatherData(
        val weatherInfo: WeatherInfo,
        val timestamp: Long,
        val isDefaultLocation: Boolean = false
    )

    @Volatile
    private var memoryCache: CachedWeatherData? = null

    private fun getCachedWeather(context: Context): CachedWeatherData? {
        memoryCache?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TIMESTAMP)) return null

        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val temp = prefs.getInt(KEY_TEMP, 0)
        val condition = prefs.getString(KEY_CONDITION, null) ?: return null
        val isDefaultLoc = prefs.getBoolean(KEY_IS_DEFAULT_LOC, true)

        val cached = CachedWeatherData(
            weatherInfo = WeatherInfo(
                temperatureCelsius = temp,
                conditionText = condition,
                isSuccess = true
            ),
            timestamp = timestamp,
            isDefaultLocation = isDefaultLoc
        )
        memoryCache = cached
        return cached
    }

    private fun saveCachedWeather(
        context: Context,
        weatherInfo: WeatherInfo,
        timestamp: Long,
        isDefaultLocation: Boolean = false
    ) {
        val cached = CachedWeatherData(weatherInfo, timestamp, isDefaultLocation)
        memoryCache = cached

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_TEMP, weatherInfo.temperatureCelsius)
            putString(KEY_CONDITION, weatherInfo.conditionText)
            putLong(KEY_TIMESTAMP, timestamp)
            putBoolean(KEY_IS_DEFAULT_LOC, isDefaultLocation)
        }
    }

    fun clearCache(context: Context) {
        memoryCache = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

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
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    } else {
                        // Fallback to lastLocation if getCurrentLocation returns null
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) {
                                    continuation.resume(lastLoc)
                                }
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("WeatherService", "Failed to fetch current location", exception)
                    // Try lastLocation on failure
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            if (continuation.isActive) {
                                continuation.resume(lastLoc)
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Exception while requesting location fix", e)
            null
        }
    }

    suspend fun fetchCurrentWeather(
        context: Context,
        forceRefresh: Boolean = false
    ): WeatherInfo = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val hasLocationPermission = PermissionManager.hasLocationPermission(context)

        if (!forceRefresh) {
            val cachedData = getCachedWeather(context)
            if (cachedData != null) {
                val isCacheValid = (now - cachedData.timestamp) < CACHE_TTL_MILLIS
                // If cache was generated with default location but user now granted location, bypass cache
                val needsRealLocationRefresh = cachedData.isDefaultLocation && hasLocationPermission
                if (isCacheValid && !needsRealLocationRefresh) {
                    Log.d(
                        "WeatherService",
                        "Returning cached weather data (age: ${(now - cachedData.timestamp) / 1000}s)"
                    )
                    return@withContext cachedData.weatherInfo
                }
            }
        }

        var lat = 40.7128 // Default New York latitude
        var lon = -74.0060 // Default New York longitude
        var isDefaultLocationUsed = true

        try {
            // 1. Fetch fresh location fix if permissions are granted (5 second timeout)
            val currentLocation = withTimeoutOrNull(5000L) {
                getFreshLocation(context)
            }
            if (currentLocation != null) {
                lat = currentLocation.latitude
                lon = currentLocation.longitude
                isDefaultLocationUsed = false
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
                val freshInfo = WeatherInfo(
                    temperatureCelsius = temp,
                    conditionText = condition,
                    isSuccess = true
                )
                saveCachedWeather(
                    context,
                    freshInfo,
                    now,
                    isDefaultLocation = isDefaultLocationUsed
                )
                return@withContext freshInfo
            }
        } catch (e: Throwable) {
            Log.e("WeatherService", "Network or location error occurred during weather fetch", e)
        }

        // Fallback response on network or parsing failure:
        val staleCache = getCachedWeather(context)
        if (staleCache != null) {
            Log.w("WeatherService", "Fetch failed. Returning stale cached weather info.")
            return@withContext staleCache.weatherInfo
        }

        // Default fallback
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
