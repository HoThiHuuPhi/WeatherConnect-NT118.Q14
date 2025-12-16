package com.example.doanck.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import com.example.doanck.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalTime
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.clipPath

private val GlassDark = Color(0xFF020617).copy(alpha = 0.40f)
private val Track = Color.White.copy(alpha = 0.20f)
private val Fill = Color.White.copy(alpha = 0.55f)

@Composable
fun WeatherCardsSection(
    feelsLike: Int,
    actual: Int,
    dayMin: Int,
    dayMax: Int,
    uvMax: Float?,
    windSpeedKmh: Int?,
    windGustKmh: Int?,
    windDirDeg: Int?,
    sunriseHHmm: String?,
    sunsetHHmm: String?,
    rainMm: Double?,
    rainSumMm: Double?,
    snowfallMm: Double?,
    humidityPercent: Double?,
    pressureMslHPa: Double?, // áp suất chuẩn hóa trên mực nước biển
    pressureHPa: Double?, // áp suất so với độ cao địa hình
    elevationM: Double?,

    cape: Double?,
    cloudCover: Double?,
    cloudLow: Double?,
    cloudMid: Double?,
    cloudHigh: Double?,
    soilMoisture0_1: Double?,
    soilMoisture1_3: Double?,
    soilMoisture3_9: Double?,
    dewPoint: Double?,
    sunshineDurationSeconds: Double?,
    modifier: Modifier = Modifier
) {
    val sectionGap = 24.dp
    val colGap = 24.dp

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(sectionGap))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            FeelsLikeCard(feelsLike, actual, dayMin, dayMax, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(colGap))
            UvIndexCard(uvMax, sunsetHHmm, Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(sectionGap))

        WindCard(windSpeedKmh, windGustKmh, windDirDeg, Modifier.fillMaxWidth())

        Spacer(Modifier.height(sectionGap))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            SunTimesCard(sunriseHHmm, sunsetHHmm, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(colGap))
            RainCard(rainMm, rainSumMm, Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(sectionGap))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HumidityCard(humidityPercent, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(colGap))
            PressureSeaLevelCard(
                presMsl = pressureMslHPa,
                presSurface = pressureHPa,
                elevationM = elevationM,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(sectionGap))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            CapeCard(cape, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(24.dp))
            SunshineDurationCard(sunshineDurationSeconds,sunriseHHmm,  sunsetHHmm,Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(sectionGap))

        DewPointCard(dewPoint, actual, Modifier.fillMaxWidth())

        Spacer(Modifier.height(sectionGap))

        CloudCoverCard(cloudCover, cloudLow, cloudMid, cloudHigh, Modifier.fillMaxWidth())

        Spacer(Modifier.height(sectionGap))

        SoilMoistureCard(soilMoisture0_1, soilMoisture1_3, soilMoisture3_9, Modifier.fillMaxWidth())

        if (snowfallMm != null && snowfallMm > 0) {
            Spacer(Modifier.height(sectionGap))
            SnowfallCard(snowfallMm, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CardHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(0.85f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White.copy(0.75f), fontSize = 14.sp)
    }
}
@Composable
private fun FrostedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}
@Composable
private fun FeelsLikeCard(feelsLike: Int, actual: Int, dayMin: Int, dayMax: Int, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Thermostat, "CẢM NHẬN")
        Spacer(Modifier.height(10.dp))
        Text(text = "$feelsLike°", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
        Text(text = "Thực tế: $actual°", color = Color.White.copy(0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        TemperatureDifferenceBar(actual, feelsLike, actual - feelsLike, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                actual - feelsLike >= 3 -> "Gió/lạnh làm bạn cảm thấy lạnh hơn."
                actual - feelsLike <= -3 -> "Độ ẩm/nắng làm bạn cảm thấy nóng hơn."
                else -> "Cảm nhận gần giống nhiệt độ thực tế."
            },
            color = Color.White.copy(0.9f),
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
    }
}
@Composable
private fun TemperatureDifferenceBar(actual: Int, feelsLike: Int, diff: Int, modifier: Modifier) {
    Column(modifier = modifier) {
        if (diff != 0) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                contentAlignment = if (diff > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth((abs(diff) / 15f).coerceIn(0f, 1f) * 0.9f),
                    contentAlignment = if (diff > 0) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Text(
                        text = "${if (diff > 0) "+" else ""}${diff}°",
                        color = Color(0xFF64B5F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$feelsLike°",
                color = Color.White.copy(0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(32.dp)
            )
            Spacer(Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f).height(8.dp), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp))
                        .background(Track)
                )

                val prog = (abs(diff) / 15f).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(prog),
                    horizontalArrangement = if (diff > 0) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        Modifier.fillMaxHeight().weight(1f).clip(RoundedCornerShape(99.dp))
                            .background(Fill)
                    )
                    Canvas(modifier = Modifier.size(8.dp)) {
                        val sz = size.width
                        val cy = size.height / 2f
                        if (diff > 0) {
                            drawLine(
                                Color.White,
                                Offset(sz, 0f),
                                Offset(0f, cy),
                                2.dp.toPx(),
                                StrokeCap.Round
                            )
                            drawLine(
                                Color.White,
                                Offset(sz, sz),
                                Offset(0f, cy),
                                2.dp.toPx(),
                                StrokeCap.Round
                            )
                        } else {
                            drawLine(
                                Color.White,
                                Offset(0f, 0f),
                                Offset(sz, cy),
                                2.dp.toPx(),
                                StrokeCap.Round
                            )
                            drawLine(
                                Color.White,
                                Offset(0f, sz),
                                Offset(sz, cy),
                                2.dp.toPx(),
                                StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            Text(
                text = "$actual°",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}
@Composable
private fun UvIndexCard(uvMax: Float?, sunset: String?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.WbSunny, "CHỈ SỐ UV")
        Spacer(Modifier.height(10.dp))
        Text(
            text = uvMax?.roundToInt()?.toString() ?: "—",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = uvMax?.let {
                when {
                    it < 3f -> "Thấp"
                    it < 6f -> "Trung bình"
                    it < 8f -> "Cao"
                    it < 11f -> "Rất cao"
                    else -> "Cực cao"
                }
            } ?: "Không có dữ liệu",
            color = Color.White.copy(0.9f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        UvGradientBar(uvMax)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (sunset != null) "Tránh nắng đến $sunset." else "Hạn chế nắng gắt vào trưa/chiều.",
            color = Color.White.copy(0.9f),
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
    }
}
@Composable
private fun UvGradientBar(value: Float?) {
    val t = ((value ?: 0f) / 11f).coerceIn(0f, 1f)

    val gradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF2ECC71),
            Color(0xFFF1C40F),
            Color(0xFFF39C12),
            Color(0xFFE74C3C),
            Color(0xFF9B59B6)
        )
    )

    Box(Modifier.fillMaxWidth().height(18.dp)) {

        val base = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .align(Alignment.Center)
            .clip(RoundedCornerShape(99.dp))

        if (value == null) {
            Box(base.background(Track))
        } else {
            Box(base.background(gradient))
        }

        if (value != null) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * t
                val center = Offset(x, size.height / 2f)

                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = center
                )

                // ✅ style phải là named arg
                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = 7.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
@Composable
private fun WindCard(
    speed: Number?,
    gust: Number?,
    dir: Number?,
    modifier: Modifier
) {
    val speedVal = speed?.toDouble()
    val gustVal = gust?.toDouble()
    val dirVal = dir?.toDouble()

    FrostedCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Air, contentDescription = null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("GIÓ", color = Color.White.copy(0.7f), fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // CỘT TRÁI
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Hàng 1: Gió
                WindInfoRow("Gió", speedVal?.let { "${it.roundToInt()} km/h" } ?: "—")
                Divider(color = Color.White.copy(0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Hàng 2: Gió giật
                WindInfoRow("Gió giật", gustVal?.let { "${it.roundToInt()} km/h" } ?: "—")
                Divider(color = Color.White.copy(0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Hàng 3: Hướng
                val dirText = if (dirVal != null) "${dirVal.roundToInt()}° ${degToCompassVi(dirVal)}" else "—"
                WindInfoRow("Hướng", dirText)
            }

            Spacer(Modifier.width(12.dp))

            // CỘT PHẢI: LA BÀN
            WindCompassGauge(
                speed = speedVal?.toFloat(),
                dir = dirVal?.toFloat(),
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
@Composable
private fun WindInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
        Text(text = value, color = Color.White.copy(0.9f), fontSize = 15.sp, fontWeight = FontWeight.Normal)
    }
}
@Composable
private fun WindCompassGauge(speed: Float?, dir: Float?, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 1. LỚP VẼ CANVAS (Vạch chia & Mũi tên)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Vẽ các vạch chia độ (Ticks)
            // Vẽ 72 vạch (mỗi vạch cách nhau 5 độ)
            val tickLength = 4.dp.toPx()
            val longTickLength = 6.dp.toPx()

            repeat(72) { i ->
                val angleDeg = i * 5f
                val angleRad = Math.toRadians((angleDeg - 90).toDouble()) // -90 để 0 độ bắt đầu từ đỉnh (Bắc)

                // Xác định vạch chính (B, Đ, N, T) để vẽ dài hơn chút hoặc đậm hơn
                val isCardinal = (i % 18 == 0) // 0, 90, 180, 270
                val length = if (isCardinal) longTickLength else tickLength
                val alpha = if (isCardinal) 0.5f else 0.3f
                val width = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()

                val start = Offset(
                    (center.x + (radius - length) * cos(angleRad)).toFloat(),
                    (center.y + (radius - length) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = Color.White.copy(alpha),
                    start = start,
                    end = end,
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }

            // Vẽ Mũi tên chỉ hướng (Kim gió)
            if (dir != null) {
                // Chuyển độ la bàn (0=Bắc) sang độ lượng giác (0=Đông, 270=Bắc)
                // Công thức: angleRad = (dir - 90) * PI/180
                val angleRad = Math.toRadians((dir - 90).toDouble())

                // Khoảng cách từ tâm để chừa chỗ cho chữ số ở giữa
                val innerGap = radius * 0.45f
                val arrowTipRadius = radius * 0.95f
                val tailRadius = radius * 0.95f

                // Tọa độ Đuôi (Tròn) - Hướng gió đến (Wind Source)
                // cos(angleRad) là hướng gió đến.
                val tailCenter = Offset(
                    (center.x + tailRadius * cos(angleRad)).toFloat(),
                    (center.y + tailRadius * sin(angleRad)).toFloat()
                )

                // Tọa độ Đầu (Nhọn) - Hướng gió đi (Wind Destination)
                // Ngược lại 180 độ -> dùng dấu trừ (-)
                val headTip = Offset(
                    (center.x - arrowTipRadius * cos(angleRad)).toFloat(),
                    (center.y - arrowTipRadius * sin(angleRad)).toFloat()
                )

                // 1. Vẽ Đuôi Tròn
                drawCircle(Color.White, radius = 5.dp.toPx(), center = tailCenter)

                // 2. Vẽ Thân kim (Phần đuôi -> Tâm)
                val tailInner = Offset(
                    (center.x + innerGap * cos(angleRad)).toFloat(),
                    (center.y + innerGap * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = tailCenter,
                    end = tailInner,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3. Vẽ Thân kim (Tâm -> Đầu)
                val headInner = Offset(
                    (center.x - innerGap * cos(angleRad)).toFloat(),
                    (center.y - innerGap * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = headInner,
                    end = headTip,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 4. Vẽ Mũi tên nhọn
                // Tạo path hình tam giác cho đầu mũi tên
                val arrowSize = 8.dp.toPx()
                val path = Path().apply {
                    moveTo(headTip.x, headTip.y)
                    // Tính 2 điểm cánh của mũi tên
                    // Góc vuông góc với đường thẳng kim
                    val wingAngle1 = angleRad + Math.PI / 2 + Math.PI // Ngược chiều + vuông góc
                    val wingAngle2 = angleRad - Math.PI / 2 + Math.PI

                    // Xoay 1 chút để tạo hình mũi tên (khoảng 30 độ so với trục)
                    val arrowWingAngle1 = angleRad + Math.PI - 0.5 // ~30 degree offset
                    val arrowWingAngle2 = angleRad + Math.PI + 0.5

                    lineTo(
                        (headTip.x + arrowSize * cos(arrowWingAngle1)).toFloat(),
                        (headTip.y + arrowSize * sin(arrowWingAngle1)).toFloat()
                    )
                    lineTo(
                        (headTip.x + arrowSize * cos(arrowWingAngle2)).toFloat(),
                        (headTip.y + arrowSize * sin(arrowWingAngle2)).toFloat()
                    )
                    close()
                }
                drawPath(path, Color.White)
            }
        }

        // 2. CÁC KÝ TỰ HƯỚNG (B, Đ, N, T)
        // Dùng Box alignment để đặt chữ chính xác
        Box(Modifier.fillMaxSize()) {
            Text("B", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp))
            Text("N", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
            Text("T", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp))
            Text("Đ", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
        }

        // 3. THÔNG SỐ TỐC ĐỘ (Ở GIỮA)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed?.roundToInt()?.toString() ?: "0",
                color = Color.White,
                fontSize = 20.sp, // Số to vừa phải để không lấn kim
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun degToCompassVi(deg: Double): String {
    val d = ((deg % 360) + 360) % 360
    return when {
        d < 22.5 || d >= 337.5 -> "B"
        d < 67.5 -> "ĐB"
        d < 112.5 -> "Đ"
        d < 157.5 -> "ĐN"
        d < 202.5 -> "N"
        d < 247.5 -> "TN"
        d < 292.5 -> "T"
        else -> "TB"
    }
}
@Composable
private fun SunTimesCard(sunrise: String?, sunset: String?, modifier: Modifier) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute
    val sunriseMinutes = parseTimeToMinutes(sunrise) ?: (6 * 60)
    val sunsetMinutes = parseTimeToMinutes(sunset) ?: (18 * 60)

    val isDayTime = currentMinutes in sunriseMinutes until sunsetMinutes
    val title = if (isDayTime) "MẶT TRỜI LẶN" else "MẶT TRỜI MỌC"
    val mainTimeDisplay = if (isDayTime) sunset else sunrise
    val subLabel = if (isDayTime) "Mặt trời mọc: $sunrise" else "Mặt trời lặn: $sunset"

    val iconRes = if (isDayTime) R.drawable.sunset else R.drawable.sunrise

    FrostedCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start, // Header nằm bên trái
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White.copy(0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color.White.copy(0.75f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = mainTimeDisplay ?: "--:--",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                SunGraph(
                    currentMinutes = currentMinutes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Khoảng cách giữa Biểu đồ và Chữ dưới
            Spacer(Modifier.height(8.dp))

            Text(
                text = subLabel,
                color = Color.White.copy(0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun SunGraph(
    currentMinutes: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // --- CẤU HÌNH VẼ ---
        // Đặt đường chân trời ở 60% chiều cao Box.
        // Điều này giúp sóng (cả đỉnh và đáy) nằm trọn trong Box, không bị tràn xuống chữ.
        val horizonY = height * 0.60f
        val amplitude = height * 0.35f // Độ cao sóng vừa phải

        // 1. Vẽ đường chân trời
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, horizonY),
            end = Offset(width, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        fun getYForTime(minutes: Int): Float {
            val progress = (minutes / 1440f) * 2 * Math.PI
            val rawY = -cos(progress - Math.PI).toFloat()
            return horizonY + (rawY * amplitude)
        }

        // 2. Path & Gradient
        val path = Path()
        val gradientPath = Path()
        gradientPath.moveTo(0f, horizonY)

        for (x in 0..width.toInt()) {
            val progressX = x / width
            val minutes = progressX * 1440
            val y = getYForTime(minutes.toInt())

            if (x == 0) {
                path.moveTo(x.toFloat(), y)
                gradientPath.lineTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
                gradientPath.lineTo(x.toFloat(), y)
            }
        }

        gradientPath.lineTo(width, horizonY)
        gradientPath.close()

        // Vẽ Gradient (Chỉ phần trên đường chân trời)
        drawContext.canvas.save()
        drawContext.canvas.clipRect(androidx.compose.ui.geometry.Rect(0f, 0f, width, horizonY))
        drawPath(
            path = gradientPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFDB813).copy(alpha = 0.35f),
                    Color(0xFFFDB813).copy(alpha = 0.05f)
                ),
                startY = horizonY - amplitude,
                endY = horizonY
            )
        )
        drawContext.canvas.restore()

        // Vẽ đường cong
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.6f),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Vẽ Chấm Mặt Trời
        val currentX = (currentMinutes / 1440f) * width
        val currentY = getYForTime(currentMinutes)

        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = 14.dp.toPx(),
            center = Offset(currentX, currentY)
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(), // Giảm nhỏ xíu cho tinh tế
            center = Offset(currentX, currentY)
        )
    }
}
fun parseTimeToMinutes(timeStr: String?): Int? {
    if (timeStr.isNullOrEmpty()) return null
    return try {
        val parts = timeStr.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        h * 60 + m
    } catch (e: Exception) {
        null
    }
}
@Composable
private fun RainCard(rain: Double?, sum: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Water, "LƯỢNG MƯA")
        Spacer(Modifier.height(12.dp))

        val v = rain ?: sum ?: 0.0
        Text(text = "${v.roundToInt()} mm", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Text(
            text = if (sum != null && sum > 0) "Tổng hôm nay: ${sum.roundToInt()} mm"
            else if (rain != null && rain > 0) "Đang có mưa"
            else "Không có mưa",
            color = Color.White.copy(0.9f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            val int = ((rain ?: 0.0) / 10.0).coerceIn(0.0, 1.0).toFloat()
            repeat(8) { i ->
                val x = size.width * (i / 8f) + (size.width / 16f)
                drawLine(
                    Color(0xFF64B5F6).copy(0.3f + int * 0.4f),
                    Offset(x, size.height * 0.2f),
                    Offset(x, size.height * 0.2f + size.height * 0.3f * (0.5f + int * 0.5f)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun HumidityCard(hum: Double?, modifier: Modifier = Modifier) {
    FrostedCard(modifier) {
        val h = hum?.roundToInt()?.coerceIn(0, 100) ?: 0

        val status = when {
            h >= 70 -> "Ẩm ướt"
            h >= 50 -> "Thoải mái"
            h >= 30 -> "Khô"
            else -> "Rất khô"
        }

        CardHeader(Icons.Outlined.WaterDrop, "ĐỘ ẨM")

        Spacer(Modifier.height(4.dp))

        Text(
            text = "$h%",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = status,
            color = Color.White.copy(0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Animation cho sóng
            val infiniteTransition = rememberInfiniteTransition(label = "wave")
            val wavePhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 2 * PI.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "phase"
            )

            // Canvas vẽ vòng tròn nước
            Canvas(modifier = Modifier.size(140.dp)) {
                val radius = size.minDimension / 2f
                val centerOffset = center

                // 1. Vẽ đường tròn nền (Track) - Viền mờ bên ngoài
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )

                // 2. Vẽ Sóng Nước bên trong
                val waterPath = Path()

                // Tính chiều cao mực nước dựa trên % độ ẩm
                // 100% là đầy (top), 0% là đáy (bottom)
                val progress = h / 100f
                val waterLevelY = size.height * (1 - progress)

                // Tạo đường Path hình sóng (Sine wave)
                val waveAmplitude = 10.dp.toPx() // Độ cao gợn sóng
                val waveFrequency = 1.5f // Số lượng gợn sóng

                waterPath.moveTo(0f, size.height) // Bắt đầu từ góc dưới trái
                waterPath.lineTo(0f, waterLevelY) // Lên đến mực nước

                // Vẽ đường cong sin từ trái qua phải
                // Nếu muốn sóng đứng yên, xóa biến wavePhase và thay bằng 0f
                var x = 0f
                val step = 5f // Độ mịn của đường cong
                while (x <= size.width) {
                    val angle = (x / size.width) * (2 * PI) * waveFrequency + wavePhase
                    val y = waterLevelY + waveAmplitude * sin(angle).toFloat()
                    waterPath.lineTo(x, y)
                    x += step
                }

                waterPath.lineTo(size.width, size.height) // Xuống góc dưới phải
                waterPath.close()

                // Cắt (Clip) hình sóng để nó nằm gọn trong hình tròn
                val circlePath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) }

                clipPath(circlePath) {
                    // Fill gradient màu xanh nước biển
                    drawPath(
                        path = waterPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF26C6DA), // Màu Cyan sáng (trên)
                                Color(0xFF01579B)  // Màu Blue đậm (dưới)
                            ),
                            startY = waterLevelY - waveAmplitude,
                            endY = size.height
                        )
                    )
                }

                // 3. Vẽ viền sáng bao quanh vòng tròn (Glass effect)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(0.3f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            Text(
                text = "$h%",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(0.3f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

private fun formatPressureVN(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(".").reversed()
@Composable
private fun PressureSeaLevelCard(presMsl: Double?, presSurface: Double?, elevationM: Double?, modifier: Modifier
) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Speed, "ÁP SUẤT")
        Spacer(Modifier.height(10.dp))

        val mslInt = presMsl?.roundToInt()

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = mslInt?.let { formatPressureVN(it) } ?: "—",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "hPa",
                color = Color.White.copy(0.9f),
                fontSize = 14.sp,
                modifier = Modifier.alignByBaseline()
            )
        }


        Spacer(Modifier.height(6.dp))

        PressureGaugeIOS(value = mslInt, modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Độ cao: ${(elevationM ?: 0.0).roundToInt()} m",
            color = Color.White.copy(0.85f),
            fontSize = 13.sp
        )

        val surfaceInt = presSurface?.roundToInt()
        Text(
            text = surfaceInt?.let { "Áp suất theo địa hình: ${formatPressureVN(it)} hPa" }
                ?: "Áp suất theo địa hình: —",
            color = Color.White.copy(0.85f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PressureGaugeIOS(value: Int?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val cx = w / 2f
            val cy = h * 0.72f

            val outerR = min(w, h) * 0.48f
            val minorLen = 8.dp.toPx()
            val majorLen = 14.dp.toPx()
            val minorW = 2.dp.toPx()
            val majorW = 3.dp.toPx()

            val minP = 980f
            val maxP = 1040f
            val norm = if (value != null) ((value - minP) / (maxP - minP)).coerceIn(0f, 1f) else null

            val totalTicks = 40
            for (i in 0..totalTicks) {
                val t = i / totalTicks.toFloat()
                val angleDeg = 180f - 180f * t
                val a = Math.toRadians(angleDeg.toDouble())

                val isMajor = (i % 5 == 0)
                val len = if (isMajor) majorLen else minorLen
                val sw = if (isMajor) majorW else minorW
                val alpha = if (isMajor) 0.35f else 0.22f

                val x1 = cx + cos(a).toFloat() * outerR
                val y1 = cy - sin(a).toFloat() * outerR
                val x2 = cx + cos(a).toFloat() * (outerR - len)
                val y2 = cy - sin(a).toFloat() * (outerR - len)

                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
            }

            // Kim trắng
            if (norm != null) {
                val needleAngleDeg = 180f - 180f * norm
                val na = Math.toRadians(needleAngleDeg.toDouble())

                val needleOuter = outerR + 2.dp.toPx()
                val needleInner = outerR - 22.dp.toPx()

                val nx1 = cx + cos(na).toFloat() * needleOuter
                val ny1 = cy - sin(na).toFloat() * needleOuter
                val nx2 = cx + cos(na).toFloat() * needleInner
                val ny2 = cy - sin(na).toFloat() * needleInner

                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(nx1, ny1),
                    end = Offset(nx2, ny2),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.North,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .size(26.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Thấp", color = Color.White.copy(0.9f), fontSize = 14.sp)
            Text("Cao", color = Color.White.copy(0.9f), fontSize = 14.sp)
        }
    }
}
@Composable
private fun SnowfallCard(snow: Double, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.AcUnit, "TUYẾT")
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${snow.roundToInt()} mm",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tuyết rơi hôm nay",
                    color = Color.White.copy(0.9f),
                    fontSize = 14.sp
                )
            }
            Icon(
                Icons.Outlined.AcUnit,
                contentDescription = null,
                tint = Color(0xFF90CAF9),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
@Composable
private fun CapeCard(cape: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.FlashOn, "CHỈ SỐ GIÔNG BÃO")
        Spacer(Modifier.height(12.dp))

        val value = cape?.roundToInt() ?: 0
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "$value", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(text = "J/kg", color = Color.White.copy(0.7f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
        }

        Spacer(Modifier.height(20.dp))

        val (status, color, desc) = when {
            value < 1000 -> Triple("ỔN ĐỊNH", Color(0xFF00B8D4), "Ít khả năng có giông.")
            value < 2500 -> Triple("BẤT ỔN VỪA", Color(0xFFAEEA00), "Có thể xuất hiện giông.")
            value < 4000 -> Triple("BẤT ỔN MẠNH", Color(0xFFFF6D00), "Khả năng cao có giông mạnh.")
            else -> Triple("CỰC KỲ BẤT ỔN", Color(0xFFDD2C00), "Nguy hiểm! Cảnh báo bão tố.")
        }

        Text(text = status, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp))) {
            Box(Modifier.fillMaxSize().background(Track))
            val progress = (value / 4500f).coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFF44336))
                        )
                    )
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(text = desc, color = Color.White.copy(0.9f), fontSize = 14.sp)
    }
}
@Composable
private fun CloudCoverCard(
    total: Double?,
    low: Double?,
    mid: Double?,
    high: Double?,
    modifier: Modifier
) {
    FrostedCard(modifier) {
        // 1. Header dùng Icon Drawable
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.cloud_cover),
                contentDescription = null,
                tint = Color.White.copy(0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("ĐỘ CHE PHỦ MÂY", color = Color.White.copy(0.75f), fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        val t = total?.roundToInt() ?: 0

        // 2. Logic màu sắc trạng thái
        // Ít mây -> Màu xanh (Sky Blue), Nhiều mây -> Màu trắng xám
        val (status, statusColor) = when {
            t < 20 -> "Quang đãng" to Color(0xFF64B5F6) // Blue
            t < 50 -> "Ít mây" to Color(0xFF90CAF9)
            t < 80 -> "Nhiều mây" to Color(0xFFB0BEC5)
            else -> "U ám" to Color(0xFFECEFF1) // White/Gray
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cột bên trái: Số to + Trạng thái
            Column {
                Text(
                    text = "$t%",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cột bên phải: Chi tiết 3 tầng mây
            // Sắp xếp: Cao -> Trung -> Thấp (theo đúng vật lý)
            Column(
                Modifier.width(150.dp), // Tăng độ rộng chút cho thoáng
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CloudLayerRow("Cao", high)
                CloudLayerRow("Trung", mid)
                CloudLayerRow("Thấp", low)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Lời khuyên (Footer)
        val advice = when {
            t < 30 -> "Bầu trời trong xanh, rất tốt để ngắm sao hoặc chụp ảnh phong cảnh."
            t > 80 -> "Trời âm u, ánh sáng khuếch tán mềm, tốt cho chụp chân dung ngoài trời."
            else -> "Nắng gián đoạn, thích hợp cho các hoạt động ngoài trời."
        }

        Text(
            text = advice,
            color = Color.White.copy(0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun CloudLayerRow(label: String, value: Double?) {
    val v = value?.roundToInt() ?: 0
    // Scale 0..100 -> 0..1
    val progress = (v / 100f).coerceIn(0f, 1f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Label (Canh lề phải cho đẹp)
        Text(
            text = label,
            color = Color.White.copy(0.6f),
            fontSize = 11.sp,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Start
        )

        // Thanh Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp) // Dày hơn chút
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(0.1f)) // Track mờ
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        // Gradient từ mờ sang rõ tạo hiệu ứng "khí/mây"
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(0.3f),
                                Color.White.copy(0.9f)
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.width(8.dp))

        // Giá trị %
        Text(
            text = "$v%",
            color = Color.White.copy(0.9f),
            fontSize = 11.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}


// SOIL MOISTURE CARD (ĐỘ ẨM ĐẤT)
private enum class SoilLevel { DRY, OK, WET, FLOOD }
private data class SoilUi(
    val level: SoilLevel,
    val label: String,
    val color: Color,
    val advice: String,
    val iconRes: Int
)

private fun soilUi(mainVal: Double): SoilUi {
    val v = mainVal.coerceIn(0.0, 0.6)
    return when {
        v < 0.15 -> SoilUi(
            level = SoilLevel.DRY,
            label = "Khô",
            color = Color(0xFFFFB74D),
            advice = "Đất hơi khô, phù hợp tưới bổ sung nếu trời nắng.",
            iconRes = R.drawable.plant_dry
        )
        v < 0.30 -> SoilUi(
            level = SoilLevel.OK,
            label = "Đủ ẩm",
            color = Color(0xFF66BB6A),
            advice = "Độ ẩm ổn, phù hợp cho cây phát triển.",
            iconRes = R.drawable.plant_ok
        )
        v < 0.45 -> SoilUi(
            level = SoilLevel.WET,
            label = "Ẩm",
            color = Color(0xFF4FC3F7),
            advice = "Đất đang ẩm, hạn chế tưới thêm.",
            iconRes = R.drawable.plant_wet
        )
        else -> SoilUi(
            level = SoilLevel.FLOOD,
            label = "Rất ướt",
            color = Color(0xFF42A5F5),
            advice = "Đất quá ướt, coi chừng úng rễ (nếu là cây trồng).",
            iconRes = R.drawable.plant_flood
        )
    }
}

@Composable
private fun SoilLayerBar(label: String, value: Double?) {
    val v = (value ?: 0.0).coerceIn(0.0, 0.6)
    val progress = (v / 0.5).coerceIn(0.0, 1.0).toFloat()

    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
            Text(String.format("%.2f", v), color = Color.White.copy(0.9f), fontSize = 11.sp)
        }

        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(0.14f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF60443A).copy(0.55f), // khô (nâu đất)
                                Color(0xFF13E31B).copy(0.55f), // đủ ẩm (xanh)
                                Color(0xFF1293FC).copy(0.55f)  // ẩm (xanh biển)
                            )
                        )
                    )
            )

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (progress.coerceIn(0f, 1f) * 1f).let { 0.dp })
            )
        }
    }
}
@Composable
private fun SoilMoistureCard(
    l0_1: Double?,
    l1_3: Double?,
    l3_9: Double?,
    modifier: Modifier
) {
    FrostedCard(modifier) {
        val mainVal = (l0_1 ?: 0.0).coerceIn(0.0, 0.6)
        val ui = soilUi(mainVal)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_soil),
                contentDescription = null,
                tint = Color.White.copy(0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("ĐỘ ẨM ĐẤT", color = Color.White.copy(0.75f), fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.2f", mainVal),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "m³/m³",
                        color = Color.White.copy(0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                }

                Text("Bề mặt (0–1 cm)", color = Color.White.copy(0.7f), fontSize = 12.sp)

                Spacer(Modifier.height(6.dp))
                Text(ui.label, color = ui.color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(ui.advice, color = Color.White.copy(0.85f), fontSize = 13.sp, lineHeight = 16.sp)
            }

            Spacer(Modifier.width(12.dp))

            Image(
                painter = painterResource(id = ui.iconRes),
                contentDescription = null,
                modifier = Modifier.size(82.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        SoilLayerBar("0–1 cm", l0_1)
        SoilLayerBar("1–3 cm", l1_3)
        SoilLayerBar("3–9 cm", l3_9)
    }
}


// DEW POINT CARD (ĐIỂM SƯƠNG)
@Composable
private fun DewPointCard(dew: Double?, actual: Int, modifier: Modifier) {
    FrostedCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.dew_point),
                contentDescription = null,
                tint = Color.White.copy(0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("ĐIỂM SƯƠNG", color = Color.White.copy(0.75f), fontSize = 12.sp)
        }

        Spacer(Modifier.height(10.dp))

        val d = dew?.roundToInt() ?: 0

        val (status, color) = when {
            d < 10 -> "Rất khô" to Color(0xFF64B5F6)
            d < 16 -> "Thoải mái" to Color(0xFF81C784)
            d < 21 -> "Hơi ẩm" to Color(0xFFFFD54F)
            d < 24 -> "Oi bức" to Color(0xFFFF8A65)
            else -> "Ngột ngạt" to Color(0xFFE57373)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$d°",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Thực tế: $actual°C",
                color = Color.White.copy(0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(bottom = 6.dp)
            )
        }

        Text(
            text = status,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF64B5F6), // Khô
                                Color(0xFF81C784), // Thoải mái
                                Color(0xFFFFD54F), // Ẩm
                                Color(0xFFE57373)  // Ngột ngạt
                            )
                        )
                    )
            )
            val progress = (d / 28f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(height = 8.dp, width = 4.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .shadow(2.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Điểm sương càng cao, mồ hôi càng khó bay hơi, khiến cơ thể cảm thấy nóng bức hơn nhiệt độ thực tế.",
            color = Color.White.copy(0.7f),
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
    }
}

// SUNSHINE DURATION CARD (THỜI GIAN NẮNG)
@Composable
fun SunshineDurationCard(
    durationSeconds: Double?,
    sunrise: String?,
    sunset: String?,
    modifier: Modifier = Modifier
) {
    val totalSeconds = durationSeconds?.toLong() ?: 0
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    FrostedCard(modifier) {
        // --- HEADER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.sun_duration),
                contentDescription = null,
                tint = Color.White.copy(0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("THỜI GIAN NẮNG", color = Color.White.copy(0.75f), fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${hours}h ${minutes}m",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(Color.Black.copy(0.3f), Offset(0f, 4f), 8f)
                )
            )
            Text(
                text = "Tổng giờ nắng trong ngày",
                color = Color.White.copy(0.7f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            // --- VÙNG VẼ MẶT TRỜI TỎA SÁNG ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp) // Tăng chiều cao để hào quang không bị cắt
            ) {
                // 1. Vẽ mặt trời tỏa sáng ở giữa
                CentralGlowingSun(
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Label Giờ Mọc - Lặn ở 2 góc dưới
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                ) {
                    Text(
                        text = "M: " + sunrise ?: "--:--",
                        color = Color.White.copy(0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    Text(
                        text = "L: " + sunset ?: "--:--",
                        color = Color.White.copy(0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
fun CentralGlowingSun(
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Kích thước mặt trời
        val coreRadius = 28.dp.toPx()  // Lõi mặt trời
        val glowRadius = size.height * 0.6f // Vùng tỏa sáng lan rộng

        // 1. Vẽ vùng tỏa sáng (Gradient từ vàng -> trong suốt)
        val glowBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = 0.5f), // Vàng sáng ở tâm
                Color(0xFFFFD54F).copy(alpha = 0.15f), // Mờ dần
                Color.Transparent                     // Hết
            ),
            center = Offset(centerX, centerY),
            radius = glowRadius
        )

        drawCircle(
            brush = glowBrush,
            radius = glowRadius,
            center = Offset(centerX, centerY)
        )

        // 2. Vẽ Lõi mặt trời (Màu vàng cam đậm hơn chút cho rõ khối)
        drawCircle(
            color = Color(0xFFFFCA28), // Màu chính
            radius = coreRadius,
            center = Offset(centerX, centerY)
        )

        // 3. Vẽ tâm trắng nhỏ xíu tạo độ bóng (Highlight)
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = coreRadius * 0.3f,
            center = Offset(centerX - coreRadius * 0.3f, centerY - coreRadius * 0.3f)
        )

        val rayCount = 12
        val rayLength = 15.dp.toPx()
        val rayStart = coreRadius + 8.dp.toPx()

        for (i in 0 until rayCount) {
            val angleRad = Math.toRadians((i * (360 / rayCount)).toDouble())
            val startX = (centerX + rayStart * Math.cos(angleRad)).toFloat()
            val startY = (centerY + rayStart * Math.sin(angleRad)).toFloat()
            val endX = (centerX + (rayStart + rayLength) * Math.cos(angleRad)).toFloat()
            val endY = (centerY + (rayStart + rayLength) * Math.sin(angleRad)).toFloat()

            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}