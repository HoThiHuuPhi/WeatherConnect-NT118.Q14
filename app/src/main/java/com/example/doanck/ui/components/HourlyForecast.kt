package com.example.doanck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.doanck.ui.theme.SFProDisplay
import com.example.doanck.ui.theme.DoAnCKTheme

import kotlin.math.roundToInt

data class HourlyDisplayItem(
    val time: String,
    val icon: ImageVector,
    val temp: Int
)

@Composable
fun HourlyForecastSection(
    summaryText: String,
    hourlyData: List<HourlyDisplayItem>
)

{
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF262C42).copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = summaryText,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(hourlyData) { item ->
                    HourlyItem(item)
                }
            }
        }
    }
}

@Composable
fun HourlyItem(item: HourlyDisplayItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.time,
            color = Color.White,
            fontFamily = SFProDisplay,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${item.temp}°",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SFProDisplay
        )
    }
}

@Preview
@Composable
fun HourlyForecastSectionPreview() {
    val mockHourlyData = listOf(
        HourlyDisplayItem("10:00", Icons.Default.WbSunny, 28),
        HourlyDisplayItem("11:00", Icons.Default.WbSunny, 30),
        HourlyDisplayItem("12:00", Icons.Default.Cloud, 29),
        HourlyDisplayItem("13:00", Icons.Default.Cloud, 27),
        HourlyDisplayItem("14:00", Icons.Default.FlashOn, 26),
        HourlyDisplayItem("15:00", Icons.Default.Cloud, 25),
    )

    DoAnCKTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2437))) {
            HourlyForecastSection(
                summaryText = "Có mây sẽ tiếp tục đến hết ngày. Gió giật lên đến 15km/h.",
                hourlyData = mockHourlyData
            )
        }
    }
}