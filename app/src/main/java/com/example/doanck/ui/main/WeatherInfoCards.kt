package com.example.doanck.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private val GlassDark = Color(0xFF020617).copy(alpha = 0.40f)
private val GlassBorder = Color.White.copy(alpha = 0.20f)
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
    visibilityKm: Double?,
    humidityPercent: Double?,
    pressureHPa: Double?,
    elevationM: Double?,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            FeelsLikeCard(feelsLike, actual, dayMin, dayMax, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(24.dp))
            UvIndexCard(uvMax, sunsetHHmm, Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(24.dp))
        WindCard(windSpeedKmh, windGustKmh, windDirDeg, Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            SunTimesCard(sunriseHHmm, sunsetHHmm, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(24.dp))
            RainCard(rainMm, rainSumMm, Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            VisibilityCard(visibilityKm, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(24.dp))
            HumidityCard(humidityPercent, Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            PressureCard(pressureHPa, Modifier.weight(1f).fillMaxHeight())
            Spacer(Modifier.width(24.dp))
            ElevationCard(elevationM, Modifier.weight(1f).fillMaxHeight())
        }

        if (snowfallMm != null && snowfallMm > 0) {
            Spacer(Modifier.height(24.dp))
            SnowfallCard(snowfallMm, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CardHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(0.85f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White.copy(0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun FrostedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassDark),
        border = BorderStroke(1.dp, GlassBorder),
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
private fun WindCard(speed: Int?, gust: Int?, dir: Int?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Air, "GIÓ")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                listOf("Gió" to speed, "Gió giật" to gust, "Hướng" to dir).forEach { (label, v) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = label, color = Color.White.copy(0.9f), fontSize = 16.sp)
                        Text(
                            text = if (label == "Hướng" && v != null) "$v° ${degToCompassVi(v.toDouble())}"
                            else v?.let { "$it km/h" } ?: "—",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (label != "Hướng") Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            WindCompassGauge(speed?.toFloat(), dir?.toFloat(), Modifier.size(110.dp))
        }
    }
}

@Composable
private fun WindCompassGauge(speed: Float?, dir: Float?, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.White.copy(0.22f), r, c, style = Stroke(2.dp.toPx()))

            repeat(36) { i ->
                val ang = Math.toRadians((i * 10f - 90f).toDouble())
                drawLine(
                    Color.White.copy(0.25f),
                    Offset((c.x + r * 0.88f * cos(ang)).toFloat(), (c.y + r * 0.88f * sin(ang)).toFloat()),
                    Offset((c.x + r * 0.98f * cos(ang)).toFloat(), (c.y + r * 0.98f * sin(ang)).toFloat()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            if (dir != null) {
                val a = Math.toRadians((dir - 90f).toDouble())
                drawLine(
                    Color.White,
                    c,
                    Offset((c.x + r * 0.75f * cos(a)).toFloat(), (c.y + r * 0.75f * sin(a)).toFloat()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(Color.White, radius = 6.dp.toPx(), center = c)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "B", color = Color.White.copy(0.9f), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = speed?.roundToInt()?.toString() ?: "—",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "km/h", color = Color.White.copy(0.9f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = "N", color = Color.White.copy(0.9f), fontSize = 12.sp)
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
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.WbSunny, "MẶT TRỜI")
        Spacer(Modifier.height(12.dp))

        listOf(
            Triple(Icons.Outlined.WbSunny, "Mọc", sunrise) to Color(0xFFFFD54F),
            Triple(Icons.Outlined.ModeNight, "Lặn", sunset) to Color(0xFF9FA8DA)
        ).forEach { (data, tint) ->
            val (icon, label, time) = data
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = label, color = Color.White.copy(0.9f), fontSize = 16.sp)
                }
                Text(text = time ?: "—", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            if (label == "Mọc") Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val w = size.width
            val h = size.height
            val r = w * 0.4f
            val cy = h * 0.8f
            drawPath(
                Path().apply {
                    moveTo(w * 0.1f, cy)
                    arcTo(
                        androidx.compose.ui.geometry.Rect(w * 0.1f, cy - r, w * 0.9f, cy + r),
                        180f,
                        180f,
                        false
                    )
                },
                Color.White.copy(0.25f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawCircle(Color(0xFFFFD54F), 8.dp.toPx(), Offset(w * 0.5f, cy - r * 0.7f))
        }
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
private fun VisibilityCard(vis: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Visibility, "TẦM NHÌN")
        Spacer(Modifier.height(12.dp))

        val km = vis?.div(1000.0) ?: 0.0
        Text(text = "${km.roundToInt()} km", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                km >= 10 -> "Tầm nhìn tốt"
                km >= 5 -> "Tầm nhìn khá"
                km >= 2 -> "Tầm nhìn hạn chế"
                else -> "Tầm nhìn kém"
            },
            color = Color.White.copy(0.9f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(6.dp)) {
            Box(
                Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                    .clip(RoundedCornerShape(99.dp)).background(Color.White.copy(0.2f))
            )
            Box(
                Modifier.fillMaxWidth((km / 20.0).coerceIn(0.0, 1.0).toFloat()).height(4.dp)
                    .align(Alignment.CenterStart).clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(0.6f))
            )
        }
    }
}

@Composable
private fun HumidityCard(hum: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.WaterDrop, "ĐỘ ẨM")
        Spacer(Modifier.height(12.dp))

        val h = hum?.roundToInt() ?: 0
        Text(text = "$h%", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                h >= 70 -> "Ẩm ướt"
                h >= 50 -> "Thoải mái"
                h >= 30 -> "Khô"
                else -> "Rất khô"
            },
            color = Color.White.copy(0.9f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val w = size.width / 2f
            val cy = size.width / 2f
            val path = Path().apply {
                moveTo(cy, size.height * 0.1f)
                cubicTo(
                    cy - w * 0.3f,
                    size.height * 0.3f,
                    cy - w * 0.25f,
                    size.height * 0.7f,
                    cy,
                    size.height * 0.9f
                )
                cubicTo(
                    cy + w * 0.25f,
                    size.height * 0.7f,
                    cy + w * 0.3f,
                    size.height * 0.3f,
                    cy,
                    size.height * 0.1f
                )
                close()
            }
            drawPath(path, Color.White.copy(0.25f), style = Stroke(2.dp.toPx()))

            val fh = size.height * 0.9f - (size.height * 0.8f * (h / 100f))
            drawPath(
                Path().apply {
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            cy - w * 0.25f,
                            fh,
                            cy + w * 0.25f,
                            size.height * 0.9f
                        )
                    )
                },
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF42A5F5).copy(0.4f),
                        Color(0xFF1E88E5).copy(0.6f)
                    )
                )
            )
        }
    }
}

@Composable
private fun PressureCard(pres: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Speed, "ÁP SUẤT")
        Spacer(Modifier.height(12.dp))

        val p = pres?.roundToInt() ?: 1013
        Text(text = "$p", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Text(text = "hPa", color = Color.White.copy(0.9f), fontSize = 16.sp)

        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val norm = ((p - 980f) / 60f).coerceIn(0f, 1f)
            drawArc(
                Color.White.copy(0.25f),
                180f,
                180f,
                false,
                style = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 1.6f),
                topLeft = Offset(size.width * 0.1f, size.height / 2f - size.height * 0.8f)
            )
            drawArc(
                Brush.horizontalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6))),
                180f,
                180f * norm,
                false,
                style = Stroke(12.dp.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 1.6f),
                topLeft = Offset(size.width * 0.1f, size.height / 2f - size.height * 0.8f)
            )
        }
    }
}

@Composable
private fun ElevationCard(elev: Double?, modifier: Modifier) {
    FrostedCard(modifier) {
        CardHeader(Icons.Outlined.Terrain, "ĐỘ CAO")
        Spacer(Modifier.height(12.dp))

        Text(text = "${elev?.roundToInt() ?: 0} m", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(text = "So với mực nước biển", color = Color.White.copy(0.9f), fontSize = 14.sp)

        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(50.dp)) {
            drawPath(
                Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width * 0.3f, size.height * 0.4f)
                    lineTo(size.width * 0.5f, size.height * 0.2f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width, size.height * 0.3f)
                    lineTo(size.width, size.height)
                    close()
                },
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF78909C).copy(0.6f),
                        Color(0xFF546E7A).copy(0.3f)
                    )
                )
            )
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