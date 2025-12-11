package com.example.doanck.utils

import com.example.doanck.R


enum class WeatherEffectType {
    SUNNY,
    CLOUDY,
    RAIN,
    STORM,
    SNOW,
    STARRY_NIGHT
}

data class WeatherBackground(
    val effectType: WeatherEffectType,
    val gradientStartColor: Long,
    val gradientEndColor: Long
)

object WeatherUtils {

    fun generateSummaryText(weatherCodes: List<Int>, windGusts: List<Double>): String {
        val maxGust = windGusts.take(24).maxOrNull() ?: 0.0
        val windText = "Gió giật lên đến ${maxGust.toInt()} km/h."

        val precipitationIndex = weatherCodes.take(24).indexOfFirst { it in 51..99 }

        val weatherText = if (precipitationIndex != -1) {
            val description = getDescriptionByCode(weatherCodes[precipitationIndex])
            "Dự báo có ${description.lowercase()} vào khoảng ${String.format("%02d:00", precipitationIndex)}."
        } else {
            "Trời nhiều mây, không mưa trong 24h tới."
        }

        return "$weatherText\n$windText"
    }

    fun getWeatherIcon(code: Int, isDay: Boolean): Int {
        return when (code) {

            // CLEAR SKY
            0 -> if (isDay) R.drawable.ic_day_clear else R.drawable.ic_night_clear

            // MAINLY CLEAR
            1 -> if (isDay) R.drawable.ic_day_mostly_clear else R.drawable.ic_night_mostly_clear

            // PARTLY CLOUDY
            2 -> if (isDay) R.drawable.ic_day_partly_cloud else R.drawable.ic_night_partly_cloud

            // OVERCAST
            3 -> R.drawable.ic_overcast

            // FOG
            45, 48 -> if (isDay) R.drawable.ic_fog else R.drawable.ic_fog

            // DRIZZLE
            51, 53, 55 -> if (isDay) R.drawable.ic_day_drizzle else R.drawable.ic_night_drizzle

            // RAIN
            61, 63, 65 -> R.drawable.ic_rain

            // FREEZING RAIN / ICE
            66, 67 -> R.drawable.ic_freezing_rain

            // SNOWFALL
            71, 73, 75 -> if (isDay) R.drawable.ic_day_snowfall else R.drawable.ic_snowy

            // SNOW GRAINS
            77 -> R.drawable.ic_snow

            // RAIN SHOWERS
            80, 81, 82 -> R.drawable.ic_rain

            // SNOW SHOWERS
            85, 86 -> R.drawable.ic_snowy

            // THUNDERSTORM
            95, 96, 99 -> if (isDay) R.drawable.ic_day_storm else R.drawable.ic_night_storm

            // DEFAULT
            else -> R.drawable.ic_shooting_star
        }
    }




    fun getDescriptionByCode(code: Int): String {
        return when (code) {
            0 -> "Trời quang"
            1 -> "Trời gần quang"
            2 -> "Có mây rải rác"
            3 -> "Trời nhiều mây"
            45, 48 -> "Sương mù"

            51 -> "Mưa phùn nhẹ"
            53 -> "Mưa phùn vừa"
            55 -> "Mưa phùn nặng hạt"

            56, 57 -> "Mưa phùn đóng băng"

            61 -> "Mưa rào nhẹ"
            63 -> "Mưa vừa"
            65 -> "Mưa to"

            66, 67 -> "Mưa đóng băng"

            71 -> "Tuyết rơi nhẹ"
            73 -> "Tuyết rơi vừa"
            75 -> "Tuyết rơi dày"
            77 -> "Hạt tuyết"

            80 -> "Mưa rào nhẹ"
            81 -> "Mưa rào vừa"
            82 -> "Mưa rào bạo lực"

            85 -> "Tuyết rào nhẹ"
            86 -> "Tuyết rào nặng hạt"

            95 -> "Dông bão"
            96, 99 -> "Dông bão có mưa đá"

            else -> "Thời tiết không xác định"
        }
    }

    fun getBackgroundData(code: Int, isDay: Boolean): WeatherBackground {
        return when (code) {
            // 0: Clear sky
            0 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.SUNNY,
                    gradientStartColor = 0xFF4A90E2, // Xanh trời
                    gradientEndColor = 0xFF8AC7FF // Xanh nhạt
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.STARRY_NIGHT,
                    gradientStartColor = 0xFF0A1F44, // Xanh đêm đậm
                    gradientEndColor = 0xFF1B3B6F // Xanh đêm nhạt
                )
            }
            // 1, 2, 3, 45, 48: Cloudy, Overcast, Fog (Mây và Sương)
            1, 2, 3, 45, 48 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF607D8B, // Xám
                    gradientEndColor = 0xFFB0BEC5 // Xám nhạt
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY, // Mây đêm
                    gradientStartColor = 0xFF263238, // Xám đêm
                    gradientEndColor = 0xFF455A64 // Xám đen
                )
            }
            // 51, 53, 55, 61, 63, 65, 80, 81, 82: Drizzle, Rain, Rain Showers
            in 51..55, in 61..65, in 80..82 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF1C2D43, // Xanh đen mưa
                gradientEndColor = 0xFF3E506B
            )
            // 56, 57, 66, 67, 71, 73, 75, 77, 85, 86: Freezing Drizzle/Rain, Snow, Snow Grains, Snow Showers
            in 56..57, in 66..67, in 71..79, in 85..86 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF90A4AE,
                gradientEndColor = 0xFFCFD8DC
            )
            // 95, 96, 99: Thunderstorm, Thunderstorm with Hail
            in 95..99 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF000000, // Đen bão
                gradientEndColor = 0xFF303030 // Xám đậm
            )

            else -> WeatherBackground(
                effectType = WeatherEffectType.CLOUDY,
                gradientStartColor = 0xFF4A90E2,
                gradientEndColor = 0xFF8AC7FF
            )
        }
    }
}