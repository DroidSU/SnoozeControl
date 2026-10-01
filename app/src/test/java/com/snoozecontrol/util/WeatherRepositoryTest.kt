package com.snoozecontrol.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun cacheTtl_isOneHour() {
        val expectedOneHourMillis = 60 * 60 * 1000L
        assertEquals(expectedOneHourMillis, WeatherRepository.CACHE_TTL_MILLIS)
    }

    @Test
    fun weatherInfo_defaultIsSuccess_isTrue() {
        val weatherInfo = WeatherInfo(
            temperatureCelsius = 25,
            conditionText = "Sunny",
        )
        assertTrue(weatherInfo.isSuccess)
        assertEquals(25, weatherInfo.temperatureCelsius)
        assertEquals("Sunny", weatherInfo.conditionText)
    }
}
