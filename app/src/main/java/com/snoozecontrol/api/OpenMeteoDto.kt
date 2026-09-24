package com.snoozecontrol.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("current")
    val current: CurrentWeatherDto? = null
)

@Serializable
data class CurrentWeatherDto(
    @SerialName("temperature_2m")
    val temperature2m: Double = 0.0,
    @SerialName("weather_code")
    val weatherCode: Int = 0
)
