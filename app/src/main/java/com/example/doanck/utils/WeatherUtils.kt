package com.example.doanck.utils

import com.example.doanck.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatDateShort(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: return "")
    } catch (e: Exception) {
        ""
    }
}


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

            // ================= TRỜI QUANG (CLEAR) =================
            // 0: Trời quang đãng (Clear Sky)
            0 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.SUNNY,
                    gradientStartColor = 0xFF1E88E5, // Xanh dương đậm chuẩn iOS
                    gradientEndColor = 0xFF6DD5FA  // Xanh cyan sáng
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.STARRY_NIGHT,
                    gradientStartColor = 0xFF0B1026, // Đen xanh sâu
                    gradientEndColor = 0xFF2B32B2  // Tím than nhẹ
                )
            }

            // ================= CÓ MÂY (CLOUDS) =================
            // 1: Ít mây (Mainly Clear) - Nhạt hơn 0 một chút
            1 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF4A90E2,
                    gradientEndColor = 0xFF93C6F9
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF141E30,
                    gradientEndColor = 0xFF243B55
                )
            }

            // 2: Mây rải rác (Partly Cloudy) - Style iOS (Xanh đêm sâu)
            2 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF4A90E2, // Xanh Blue sáng
                    gradientEndColor = 0xFFADC4E5   // Xanh nhạt
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF1A2A6C, // Xanh đêm (Midnight Blue)
                    gradientEndColor = 0xFF2D3436   // Xám đen nhẹ (Charcoal)
                )
            }

            // 3: Trời nhiều mây (Overcast) - Tối hơn và mịt mù hơn
            3 -> if (isDay) {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF6082B6, // Xanh xám
                    gradientEndColor = 0xFFBDC3C7   // Xám bạc
                )
            } else {
                WeatherBackground(
                    effectType = WeatherEffectType.CLOUDY,
                    gradientStartColor = 0xFF0B101B, // Gần như đen (Deep Space)
                    gradientEndColor = 0xFF1C2541   // Xanh đen mờ
                )
            }

            // ================= SƯƠNG MÙ (FOG) =================
            // 45: Sương mù (Fog)
            45 -> WeatherBackground(
                effectType = WeatherEffectType.CLOUDY,
                gradientStartColor = 0xFF607D8B, // Blue Grey
                gradientEndColor = 0xFFB0BEC5
            )

            // 48: Sương muối (Depositing Rime Fog) - Lạnh hơn
            48 -> WeatherBackground(
                effectType = WeatherEffectType.CLOUDY,
                gradientStartColor = 0xFF546E7A,
                gradientEndColor = 0xFFCFD8DC
            )

            // ================= MƯA PHÙN (DRIZZLE) =================
            // 51: Mưa phùn nhẹ (Light Drizzle)
            51 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF6190E8, // Xanh nhạt
                gradientEndColor = 0xFFA7BFE8
            )

            // 53: Mưa phùn vừa (Moderate Drizzle) - Đậm hơn chút
            53 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF5078B3,
                gradientEndColor = 0xFF8E9EAB
            )

            // 55: Mưa phùn dày (Dense Drizzle) - Gần giống mưa rào
            55 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF3E5F8A,
                gradientEndColor = 0xFF708596
            )

            // ================= MƯA ĐÓNG BĂNG (FREEZING DRIZZLE) =================
            // 56: Mưa phùn băng nhẹ
            56 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF78909C,
                gradientEndColor = 0xFFECE9E6
            )

            // 57: Mưa phùn băng dày
            57 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF546E7A,
                gradientEndColor = 0xFFBDC3C7
            )

            // ================= MƯA (RAIN) =================
            // 61: Mưa nhỏ (Slight Rain) - Màu iOS Classic Rain
            61 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF4B6CB7, // Xanh biển tối
                gradientEndColor = 0xFF182848  // Navy
            )

            // 63: Mưa vừa (Moderate Rain) - Tối hơn
            63 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF375492,
                gradientEndColor = 0xFF131D33
            )

            // 65: Mưa to (Heavy Rain) - Rất tối, cảm giác nặng hạt
            65 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF243B55, // Dark Slate
                gradientEndColor = 0xFF141E30
            )

            // ================= MƯA BĂNG (FREEZING RAIN) =================
            // 66: Mưa băng nhẹ
            66 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF2C3E50,
                gradientEndColor = 0xFF4CA1AF
            )

            // 67: Mưa băng nặng
            67 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF203040,
                gradientEndColor = 0xFF3C8090
            )

            // ================= TUYẾT (SNOW) =================
            // 71: Tuyết rơi nhẹ (Slight Snow) - Xanh băng (Ice Blue)
            71 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF83A4D4,
                gradientEndColor = 0xFFE2EAF7
            )

            // 73: Tuyết rơi vừa (Moderate Snow)
            73 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF6184B6,
                gradientEndColor = 0xFFC8D7EB
            )

            // 75: Tuyết rơi dày (Heavy Snow) - Xám trắng mịt mù
            75 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF4A5F7A,
                gradientEndColor = 0xFF9BAABF
            )

            // 77: Tuyết hạt (Snow Grains)
            77 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF607D8B,
                gradientEndColor = 0xFFECEFF1
            )

            // ================= MƯA RÀO (SHOWERS) =================
            // 80: Mưa rào nhẹ (Slight Rain Showers)
            80 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF3A6073,
                gradientEndColor = 0xFF5A7083
            )

            // 81: Mưa rào vừa (Moderate Rain Showers)
            81 -> WeatherBackground(
                effectType = WeatherEffectType.RAIN,
                gradientStartColor = 0xFF29323C,
                gradientEndColor = 0xFF485563
            )

            // 82: Mưa rào rất to (Violent Rain Showers) - Đen kịt
            82 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF101010, // Gần như đen
                gradientEndColor = 0xFF434343
            )

            // ================= TUYẾT RÀO (SNOW SHOWERS) =================
            // 85: Tuyết rào nhẹ
            85 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF455A64,
                gradientEndColor = 0xFF90A4AE
            )

            // 86: Tuyết rào nặng
            86 -> WeatherBackground(
                effectType = WeatherEffectType.SNOW,
                gradientStartColor = 0xFF37474F,
                gradientEndColor = 0xFF78909C
            )

            // ================= DÔNG BÃO (THUNDERSTORM) =================
            // 95: Có sấm sét (Thunderstorm) - Tím than
            95 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF0F0C29, // Xanh tím đen (Midnight Blue)
                gradientEndColor = 0xFF302B63    // Tím than (Deep Purple Slate)
            )

            // 96: Dông bão + Mưa đá nhẹ
            96 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF0F2027,
                gradientEndColor = 0xFF2C5364
            )

            // 99: Dông bão + Mưa đá to - Rất nguy hiểm
            99 -> WeatherBackground(
                effectType = WeatherEffectType.STORM,
                gradientStartColor = 0xFF000428, // Midnight Blue
                gradientEndColor = 0xFF004E92
            )

            // Mặc định (Default)
            else -> WeatherBackground(
                effectType = WeatherEffectType.CLOUDY,
                gradientStartColor = 0xFF42A5F5,
                gradientEndColor = 0xFF90CAF9
            )
        }
    }
}

data class DetailedWeatherItem(val code: Int, val description: String, val isDay: Boolean)

@Preview(showBackground = true, widthDp = 400, heightDp = 2000)
@Composable
fun FullWeatherCodePreview() {
    val weatherCodes = listOf(
        DetailedWeatherItem(0, "0: Clear Sky (Day)", true),
        DetailedWeatherItem(0, "0: Clear Sky (Night)", false),
        DetailedWeatherItem(1, "1: Mainly Clear", true),
        DetailedWeatherItem(2, "2: Partly Cloudy", true),
        DetailedWeatherItem(2, "2: Partly Cloudy", false),
        DetailedWeatherItem(3, "3: Overcast", true),
        DetailedWeatherItem(3, "3: Overcast", false),
        DetailedWeatherItem(45, "45: Fog", true),
        DetailedWeatherItem(51, "51: Light Drizzle", true),
        DetailedWeatherItem(53, "53: Mod Drizzle", true),
        DetailedWeatherItem(55, "55: Dense Drizzle", true),
        DetailedWeatherItem(61, "61: Slight Rain", true),
        DetailedWeatherItem(63, "63: Mod Rain", true),
        DetailedWeatherItem(65, "65: Heavy Rain", true),
        DetailedWeatherItem(71, "71: Light Snow", true),
        DetailedWeatherItem(75, "75: Heavy Snow", true),
        DetailedWeatherItem(80, "80: Rain Showers", true),
        DetailedWeatherItem(82, "82: Violent Showers", true),
        DetailedWeatherItem(95, "95: Thunderstorm", false),
        DetailedWeatherItem(99, "99: Thunder + Hail", false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Full Weather Code Colors",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        weatherCodes.forEach { item ->
            val bg = WeatherUtils.getBackgroundData(item.code, item.isDay)

            WeatherCodeStrip(item, bg)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun WeatherCodeStrip(item: DetailedWeatherItem, data: WeatherBackground) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(data.gradientStartColor), Color(data.gradientEndColor))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.description, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Start: #${data.gradientStartColor.toString(16).uppercase().takeLast(6)}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}