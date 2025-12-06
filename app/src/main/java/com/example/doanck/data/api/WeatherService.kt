package com.example.doanck.data.api

import com.example.doanck.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,windgusts_10m",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") tempUnit: String = "celsius"
    ): WeatherResponse
}