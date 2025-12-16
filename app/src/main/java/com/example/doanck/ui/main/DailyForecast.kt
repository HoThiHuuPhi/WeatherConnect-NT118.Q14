package com.example.doanck.ui.main

import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AndroidColor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.R
import com.example.doanck.ui.theme.SFProDisplay
import com.example.doanck.utils.WeatherUtils
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.div
import kotlin.text.toInt
import kotlin.times


data class DailyDisplayItem(
    val dayLabel: String,
    val dateLabel: String,
    val icon: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val rainProbability: Int?,
    val rainSumMm: Double?,
    val hourlyTemps: List<Int> = emptyList(),
    val hourlyWeatherCodes: List<Int> = emptyList(),
    val feelsLikeMin: Int? = null,
    val feelsLikeMax: Int? = null,
    val humidityMean: Int? = null,
    val windSpeedMax: Int? = null
)

data class ChartPoint(
    val temp: Int,
    val hourLabel: String,
    val iconRes: Int
)

private val DailyGlassDark = Color(0xFF020617).copy(alpha = 0.4f)

@Composable
fun DailyForecastSection(
    items: List<DailyDisplayItem>,
    unit: String,
    onDayClick: (DailyDisplayItem) -> Unit = {}
) {
    if (items.isEmpty()) return

    val defaultCount = 7
    var visibleItemCount by remember { mutableIntStateOf(defaultCount) }

    val globalMin = items.minOf { it.minTemp }
    val globalMax = items.maxOf { it.maxTemp }

    val displayItems = items.take(visibleItemCount)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DailyGlassDark)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DỰ BÁO $visibleItemCount NGÀY",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SFProDisplay
                )
            }

            Divider(color = Color.White.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(12.dp))

            displayItems.forEachIndexed { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                DailyRow(
                    item = item,
                    unit = unit,
                    globalMin = globalMin,
                    globalMax = globalMax,
                    onClick = { onDayClick(item) }
                )
            }

            if (items.size > defaultCount) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))

                val isExpanded = visibleItemCount >= items.size
                val buttonText =
                    if (isExpanded) "Thu gọn" else "Xem thêm ${items.size - visibleItemCount} ngày tới"
                val arrowRotation = if (isExpanded) 180f else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(16.dp)
                        .clickable {
                            visibleItemCount = if (isExpanded) defaultCount else items.size
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buttonText,
                            color = Color(0xFF40C4FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SFProDisplay
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF40C4FF),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRow(
    item: DailyDisplayItem,
    unit: String,
    globalMin: Int,
    globalMax: Int,
    onClick: () -> Unit
) {
    val totalRange = (globalMax - globalMin).coerceAtLeast(1)
    val rawStart = (item.minTemp - globalMin).toFloat() / totalRange
    val rawEnd = (item.maxTemp - globalMin).toFloat() / totalRange

    val startFrac = rawStart.coerceIn(0f, 1f)
    val endFrac = rawEnd.coerceIn(startFrac, 1f)

    val barStartWeight = startFrac.coerceAtLeast(0.0001f)
    val barWidthWeight = (endFrac - startFrac).coerceAtLeast(0.05f)
    val barEndWeight = (1f - endFrac).coerceAtLeast(0.0001f)

    val avgTemp = (item.minTemp + item.maxTemp) / 2

    val barGradient = when {
        avgTemp <= 0 -> Brush.horizontalGradient(listOf(Color(0xFF4BBAEE), Color(0xFF81D4FA))) // xanh biển
        avgTemp in 1..15 -> Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFFAEEA00))) // xanh lá nhạt
        avgTemp in 16..25 -> Brush.horizontalGradient(listOf(Color(0xFFFFD600), Color(0xFFE5D33B))) // vàng nhạt
        avgTemp in 26..32 -> Brush.horizontalGradient(listOf(Color(0xFFFF6D00), Color(0xFFFFAB00))) // cam nhạt
        else -> Brush.horizontalGradient(listOf(Color(0xFFEE4152), Color(0xFFE57373))) // đỏ nhạt
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.dayLabel,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SFProDisplay,
            modifier = Modifier.width(80.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(40.dp)
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )

            if (item.rainProbability != null && item.rainProbability > 20) {
                Text(
                    text = "${item.rainProbability}%",
                    color = Color(0xFF40C4FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SFProDisplay
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${item.minTemp}°$unit",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SFProDisplay
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(barStartWeight))
                Box(
                    modifier = Modifier
                        .weight(barWidthWeight)
                        .fillMaxHeight()
                        .background(barGradient)
                )
                Spacer(modifier = Modifier.weight(barEndWeight))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${item.maxTemp}°$unit",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SFProDisplay
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailBottomSheet(
    day: DailyDisplayItem,
    unit: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tab_weather),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Điều kiện thời tiết",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SFProDisplay
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = day.dayLabel + ", " + day.dateLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp,
                fontFamily = SFProDisplay
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${day.maxTemp}°$unit",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SFProDisplay
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${day.minTemp}°$unit",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 28.sp,
                    fontFamily = SFProDisplay
                )
            }
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(16.dp)
            ) {
                val chartData = remember(day.hourlyTemps, day.hourlyWeatherCodes) {
                    val temps = day.hourlyTemps
                    val codes = day.hourlyWeatherCodes

                    if (temps.isEmpty()) emptyList()
                    else {
                        temps.mapIndexed { index, temp ->
                            val code = codes.getOrElse(index) { 0 }
                            val isDay = index in 6..18

                            ChartPoint(
                                temp = temp,
                                hourLabel = if (index == 0) "00g" else String.format("%02dg", index),
                                iconRes = WeatherUtils.getWeatherIcon(code, isDay)
                            )
                        }
                    }
                }

                AdvancedTemperatureChart(
                    dataPoints = chartData,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(24.dp))

            InfoRow("Khả năng có mưa", day.rainProbability?.let { "Khoảng $it%" } ?: "Chưa có dữ liệu.")
            Spacer(Modifier.height(16.dp))
            InfoRow("Tổng lượng mưa (mm)", day.rainSumMm?.let { String.format("%.1f mm", it) } ?: "Chưa có dữ liệu lượng mưa.")
            Spacer(Modifier.height(16.dp))
            val dailySummary = buildString {
                append("Nhiệt độ dao động từ ${day.minTemp}°$unit đến ${day.maxTemp}°$unit.")
                day.rainProbability?.let {
                    append(" Khả năng mưa khoảng $it%.")
                }
                day.rainSumMm?.let {
                    if (it > 0.0) append(" Tổng lượng mưa dự kiến khoảng ${"%.1f".format(it)} mm.")
                }
                day.humidityMean?.let {
                    append(" Độ ẩm trung bình khoảng $it%.")
                }
            }

            InfoRow(
                title = "Tóm tắt hàng ngày",
                content = dailySummary
            )

            Spacer(Modifier.height(16.dp))

            val feelsLikeText = day.feelsLikeMin?.let { flMin ->
                val flMax = day.feelsLikeMax ?: flMin
                buildString {
                    append("Theo thông tin của Open-Meteo, cơ thể cảm nhận khoảng từ $flMin°$unit đến $flMax°$unit.")
                    day.windSpeedMax?.let { ws ->
                        append(" Gió tối đa khoảng $ws km/h nên khi gió mạnh có thể cảm thấy lạnh hơn thực tế.")
                    }
                    day.humidityMean?.let { h ->
                        append(" Độ ẩm khoảng $h% cũng làm cảm giác oi bức hoặc lạnh hơn.")
                    }
                }
            } ?: "Nhiệt độ cảm nhận còn phụ thuộc vào gió, độ ẩm và bức xạ mặt trời, nên đôi khi sẽ khác một chút so với nhiệt độ đo được."

            InfoRow(
                title = "Giới thiệu về nhiệt độ cảm nhận",
                content = feelsLikeText
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, content: String) {
    Column {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = SFProDisplay)
        Text(content, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontFamily = SFProDisplay)
    }
}


@Composable
private fun AdvancedTemperatureChart(
    dataPoints: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Chưa đủ dữ liệu", color = Color.Gray)
        }
        return
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(bottom = 4.dp)
        ) {
            val totalWidth = maxWidth
            val chartWidth = totalWidth - 30.dp
            val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

            val keyIndices = listOf(0, 6, 12, 18, 23)

            keyIndices.forEach { index ->
                if (index < dataPoints.size) {
                    val p = dataPoints[index]
                    val xOffset = stepX * index

                    Image(
                        painter = painterResource(id = p.iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .absoluteOffset(x = xOffset - 12.dp)
                    )
                }
            }
        }

        // canvas vẽ chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val temps = dataPoints.map { it.temp }
            val maxTemp = temps.maxOrNull() ?: 100
            val minTemp = temps.minOrNull() ?: 0
            val maxIndex = temps.indexOf(maxTemp)
            val minIndex = temps.indexOf(minTemp)

            val w = size.width
            val h = size.height
            val paddingRight = 30.dp.toPx()
            val paddingBottom = 20.dp.toPx()
            val chartW = w - paddingRight
            val chartH = h - paddingBottom

            val range = (maxTemp - minTemp).coerceAtLeast(5)
            fun getY(t: Int): Float {
                val fraction = (t - (minTemp - 1)).toFloat() / (range + 2)
                return chartH - (fraction * chartH)
            }

            val gridColor = Color.White.copy(alpha = 0.2f)
            val dotColor = Color(0xFF2C2C2E)

            val avgTemp = if (temps.isNotEmpty()) temps.average().toInt() else (minTemp + maxTemp) / 2
            val (lineStartColor, lineEndColor) = when {
                avgTemp <= 0 -> Pair(Color(0xFF4BBAEE), Color(0xFF81D4FA)) // xanh biển
                avgTemp in 1..15 -> Pair(Color(0xFF00C853), Color(0xFFAEEA00)) // xanh lá nhạt
                avgTemp in 16..25 -> Pair(Color(0xFFFFD600), Color(0xFFE5D33B)) // vàng nhạt
                avgTemp in 26..32 -> Pair(Color(0xFFFF6D00), Color(0xFFFFAB00)) // cam nhạt
                else -> Pair(Color(0xFFEE4152), Color(0xFFE57373)) // đỏ nhạt
            }

            val fillBrush = Brush.verticalGradient(
                colors = listOf(lineStartColor.copy(alpha = 0.5f), lineEndColor.copy(alpha = 0.05f)),
                startY = 0f,
                endY = chartH
            )
            val lineBrush = Brush.linearGradient(
                colors = listOf(lineStartColor, lineEndColor),
                start = Offset(0f, 0f),
                end = Offset(chartW, 0f)
            )

            val textPaint = AndroidPaint().apply {
                color = AndroidColor.LTGRAY
                textSize = 30f
                textAlign = AndroidPaint.Align.CENTER
                isAntiAlias = true
            }
            val tempTextPaint = AndroidPaint().apply {
                color = AndroidColor.GRAY
                textSize = 28f
                textAlign = AndroidPaint.Align.LEFT
                isAntiAlias = true
            }
            val labelPaint = AndroidPaint().apply {
                color = AndroidColor.WHITE
                textSize = 36f
                textAlign = AndroidPaint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = chartH * (i.toFloat() / gridSteps)
                drawLine(gridColor, Offset(0f, y), Offset(chartW, y), strokeWidth = 1.dp.toPx())
                val tempVal = maxTemp - ((maxTemp - minTemp) * (i.toFloat() / gridSteps)).toInt()
                drawContext.canvas.nativeCanvas.drawText("$tempVal°", chartW + 8f, y + 10f, tempTextPaint)
            }

            val stepX = chartW / (dataPoints.size - 1).coerceAtLeast(1)
            val points = dataPoints.mapIndexed { index, p ->
                PointF(index * stepX, getY(p.temp))
            }

            val keyIndices = listOf(0, 6, 12, 18, 23)
            keyIndices.forEach { index ->
                if (index < points.size) {
                    val x = points[index].x
                    drawLine(gridColor, Offset(x, 0f), Offset(x, chartH), strokeWidth = 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(dataPoints[index].hourLabel, x, h - 5f, textPaint)
                }
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val cx = (p1.x + p2.x) / 2f
                    cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }
            }
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, chartH)
                lineTo(points.first().x, chartH)
                close()
            }

            drawPath(
                path = fillPath,
                brush = fillBrush
            )

            drawPath(
                path = path,
                brush = lineBrush,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            keyIndices.forEach { index ->
                if (index < points.size) {
                    val p = points[index]
                    drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(p.x, p.y))
                    drawCircle(lineStartColor, radius = 4.dp.toPx(), center = Offset(p.x, p.y),
                        style = Stroke(2.dp.toPx()))
                }
            }

            val pMax = points[maxIndex]
            drawContext.canvas.nativeCanvas.drawText("C", pMax.x, pMax.y - 15.dp.toPx(), labelPaint)

            if (maxIndex != minIndex) {
                val pMin = points[minIndex]
                drawContext.canvas.nativeCanvas.drawText("T", pMin.x, pMin.y - 15.dp.toPx(), labelPaint)
            }
        }
    }
}