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
        @Query("daily") daily: String =
                    "temperature_2m_max,temperature_2m_min,weather_code," +
                    "precipitation_probability_max,precipitation_sum," +
                    "apparent_temperature_max,apparent_temperature_min," +
                    "relative_humidity_2m_mean,windspeed_10m_max",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,windgusts_10m,is_day",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") tempUnit: String = "celsius",
        @Query("forecast_days") days: Int = 10
    ): WeatherResponse
}