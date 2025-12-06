package com.example.doanck.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current")
    val current: CurrentWeather,
    @SerializedName("daily")
    val daily: DailyUnits,
    @SerializedName("hourly")
    val hourly: HourlyUnits
)

data class CurrentWeather(
    @SerializedName("temperature_2m")
    val temperature: Double,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("is_day")
    val isDay: Int // 1 là ngày, 0 là đêm
)

data class DailyUnits(
    @SerializedName("temperature_2m_max")
    val maxTemperatures: List<Double>,
    @SerializedName("temperature_2m_min")
    val minTemperatures: List<Double>
)

data class HourlyUnits(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperatures: List<Double>,
    @SerializedName("weather_code")
    val weatherCodes: List<Int>,
    @SerializedName("windgusts_10m")
    val windGusts: List<Double>,
    @SerializedName("is_day")
    val isDayList: List<Int> = emptyList() // KHỞI TẠO MẶC ĐỊNH)
)