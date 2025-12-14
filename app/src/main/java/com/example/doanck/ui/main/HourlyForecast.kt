package com.example.doanck.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.ui.theme.SFProDisplay

data class HourlyDisplayItem(
    val time: String,
    val icon: Int,
    val temp: Int
)

private val HourlyGlassDark = Color(0xFF020617).copy(alpha = 0.60f)

@Composable
fun HourlyForecastSection(
    summaryText: String,
    hourlyData: List<HourlyDisplayItem>,
    unit: String // <--- THAM SỐ MỚI
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HourlyGlassDark)
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                items(hourlyData) { item ->
                    HourlyItem(item, unit) // <--- TRUYỀN UNIT XUỐNG
                }
            }
        }
    }
}

@Composable
fun HourlyItem(item: HourlyDisplayItem, unit: String) { // <--- NHẬN UNIT
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

        Image(
            painter = painterResource(id = item.icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${item.temp}°$unit", // <--- HIỂN THỊ ĐƠN VỊ
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = SFProDisplay
        )
    }
}
