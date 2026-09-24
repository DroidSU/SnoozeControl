package com.snoozecontrol.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KtorWeatherApiClient {

    val client: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double): OpenMeteoResponseDto? {
        return try {
            val urlString =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
            client.get(urlString).body<OpenMeteoResponseDto>()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}
