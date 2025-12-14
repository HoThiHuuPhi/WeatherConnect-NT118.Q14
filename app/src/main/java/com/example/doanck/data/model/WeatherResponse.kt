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
    @SerializedName("time")
    val time: String,

    @SerializedName("temperature_2m")
    val temperature: Double,

    @SerializedName("weather_code")
    val weatherCode: Int,

    @SerializedName("is_day")
    val isDay: Int // 1 là ngày, 0 là đêm
)

data class DailyUnits(
    @SerializedName("time")
    val time: List<String>,

    @SerializedName("temperature_2m_max")
    val maxTemperatures: List<Double>,

    @SerializedName("temperature_2m_min")
    val minTemperatures: List<Double>,

    @SerializedName("weather_code")
    val weatherCodes: List<Int>,

    @SerializedName("precipitation_probability_max")
    val rainProbabilities: List<Int>? = null,

    @SerializedName("precipitation_sum")
    val rainSums: List<Double>? = null,

    @SerializedName("apparent_temperature_max")
    val apparentTempMax: List<Double>? = null,

    @SerializedName("apparent_temperature_min")
    val apparentTempMin: List<Double>? = null,

    @SerializedName("relative_humidity_2m_mean")
    val humidityMean: List<Double>? = null,

    @SerializedName("windspeed_10m_max")
    val windSpeedMax: List<Double>? = null

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