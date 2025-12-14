package com.example.doanck.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.ui.theme.SFProDisplay

// --- KHAI BÁO CLASS DỮ LIỆU (Để sửa lỗi Unresolved reference 'CurrentDisplayData') ---
data class CurrentDisplayData(
    val cityName: String,
    val currentTemp: Int,
    val description: String,
    val maxTemp: Int,
    val minTemp: Int,
    val isDay: Boolean
)

// --- KHAI BÁO STYLE CHỮ (Để sửa lỗi Unresolved reference 'SoftShadowTextStyle') ---
private val SoftShadowTextStyle = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
)

@Composable
fun MainWeatherDisplay(
    data: CurrentDisplayData,
    unit: String // Tham số đơn vị (C hoặc F)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tên thành phố
        Text(
            text = data.cityName,
            color = Color.White,
            fontSize = 40.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Normal,
            style = SoftShadowTextStyle
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Nhiệt độ
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = data.currentTemp.toString(),
                color = Color.White,
                fontSize = 120.sp,
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Thin,
                style = SoftShadowTextStyle
            )

            Text(
                text = "°$unit",
                color = Color.White,
                fontSize = 120.sp,
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Thin,
                style = SoftShadowTextStyle
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mô tả
        Text(
            text = data.description,
            color = Color.White,
            fontSize = 24.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Medium,
            style = SoftShadowTextStyle
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Max - Min
        Text(
            text = "C:${data.maxTemp}°$unit  T:${data.minTemp}°$unit",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Medium,
            style = SoftShadowTextStyle
        )
    }
}