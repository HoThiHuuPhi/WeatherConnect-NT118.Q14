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
                "relative_humidity_2m,precipitation,surface_pressure,pressure_msl," +
                "rain,snowfall,cloud_cover,dew_point_2m",

        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min," +
                "weather_code,precipitation_probability_max,precipitation_sum," +
                "apparent_temperature_max,apparent_temperature_min," +
                "relative_humidity_2m_mean,wind_speed_10m_max," +
                "uv_index_max,sunset,sunrise,snowfall_sum,rain_sum,sunshine_duration",

        @Query("hourly") hourly: String = "temperature_2m,weather_code,wind_gusts_10m,is_day," +
                "cape,dew_point_2m,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                "soil_moisture_0_to_1cm,soil_moisture_3_to_9cm,soil_moisture_9_to_27cm",

        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 10
    ): WeatherResponse
}