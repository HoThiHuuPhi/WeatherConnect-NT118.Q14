package com.example.doanck.data.api

import com.example.doanck.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code,is_day," +
                "apparent_temperature,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
                "relative_humidity_2m,precipitation,visibility,surface_pressure," +
                "rain,snowfall",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min," +
                "weather_code,precipitation_probability_max,precipitation_sum," +
                "apparent_temperature_max,apparent_temperature_min," +
                "relative_humidity_2m_mean,wind_speed_10m_max," +
                "uv_index_max,sunset,sunrise,snowfall_sum,rain_sum",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,wind_gusts_10m,is_day",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 10
    ): WeatherResponse
}