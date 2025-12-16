package com.example.doanck.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("daily") val daily: DailyUnits,
    @SerializedName("hourly") val hourly: HourlyUnits,
    @SerializedName("elevation") val elevation: Double? = null
)

data class CurrentWeather(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,

    @SerializedName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection10m: Double? = null,
    @SerializedName(value = "wind_gusts_10m", alternate = ["windgusts_10m"]) val windGusts10m: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Double? = null,
    @SerializedName("precipitation") val precipitation: Double? = null,

    @SerializedName("surface_pressure") //Áp suất so với độ cao địa hình (tại mặt đất)
    val pressure: Double? = null,
    @SerializedName("pressure_msl") //Áp suất chuẩn hóa trên mực nước biển
    val pressureMsl: Double? = null,

    @SerializedName("rain") val rain: Double? = null,
    @SerializedName("snowfall") val snowfall: Double? = null,
    @SerializedName("cloud_cover") val cloudCover: Double?,
    @SerializedName("dew_point_2m") val dewPoint2m: Double?
)

data class DailyUnits(
    @SerializedName("time") val time: List<String>,

    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double>,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double>,

    @SerializedName("weather_code") val weatherCodes: List<Int>,

    @SerializedName("precipitation_probability_max") val rainProbabilities: List<Int>? = null,
    @SerializedName("precipitation_sum") val rainSums: List<Double>? = null,

    @SerializedName("apparent_temperature_max") val apparentTempMax: List<Double>? = null,
    @SerializedName("apparent_temperature_min") val apparentTempMin: List<Double>? = null,

    @SerializedName("relative_humidity_2m_mean") val humidityMean: List<Double>? = null,
    @SerializedName(value = "wind_speed_10m_max", alternate = ["windspeed_10m_max"]) val windSpeedMax: List<Double>? = null,

    @SerializedName("uv_index_max") val uvIndexMax: List<Double>? = null,
    @SerializedName("sunset") val sunset: List<String>? = null,
    @SerializedName("sunrise") val sunrise: List<String>? = null,

    @SerializedName("snowfall_sum") val snowfallSum: List<Double>? = null,
    @SerializedName("rain_sum") val rainSum: List<Double>? = null,

    @SerializedName("sunshine_duration") val sunshineDuration: List<Double>?
)

data class HourlyUnits(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName(value = "wind_gusts_10m", alternate = ["windgusts_10m"]) val windGusts: List<Double> = emptyList(),
    @SerializedName("is_day") val isDayList: List<Int> = emptyList(),

    @SerializedName("cape") val cape: List<Double>?,
    @SerializedName("dew_point_2m") val dewPoint2m: List<Double>?,

    @SerializedName("cloud_cover") val cloudCover: List<Double>?,
    @SerializedName("cloud_cover_low") val cloudCoverLow: List<Double>?,
    @SerializedName("cloud_cover_mid") val cloudCoverMid: List<Double>?,
    @SerializedName("cloud_cover_high") val cloudCoverHigh: List<Double>?,

    @SerializedName("soil_moisture_0_to_1cm") val soilMoisture0to1: List<Double>?,
    @SerializedName("soil_moisture_3_to_9cm") val soilMoisture3to9: List<Double>?,
    @SerializedName("soil_moisture_9_to_27cm") val soilMoisture9to27: List<Double>?
)