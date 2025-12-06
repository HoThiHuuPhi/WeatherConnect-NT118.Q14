package com.example.doanck.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.ui.theme.DoAnCKTheme
import com.example.doanck.ui.theme.SFProDisplay


data class CurrentDisplayData(
    val cityName: String,
    val currentTemp: Int,
    val description: String,
    val maxTemp: Int,
    val minTemp: Int,
    val isDay: Boolean
)

private val SoftShadowTextStyle = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
)

@Composable
fun MainWeatherDisplay(data: CurrentDisplayData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = data.cityName,
            color = Color.White,
            fontSize = 40.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Normal,
            style = SoftShadowTextStyle
        )

        Spacer(modifier = Modifier.height(4.dp))

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
                text = "°",
                color = Color.White,
                fontSize = 120.sp,
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Thin,
                style = SoftShadowTextStyle
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = data.description,
            color = Color.White,
            fontSize = 24.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Medium,
            style = SoftShadowTextStyle
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "C:${data.maxTemp}° T:${data.minTemp}°",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Medium,
            style = SoftShadowTextStyle
        )
    }
}

@Preview
@Composable
fun MainWeatherDisplayPreview() {
    val mockData = CurrentDisplayData(
        cityName = "Hồ Chí Minh",
        currentTemp = 32,
        description = "Chủ yếu nắng",
        maxTemp = 35,
        minTemp = 25,
        isDay = true
    )

    DoAnCKTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFB3E5FC))
        ) {
            MainWeatherDisplay(data = mockData)
        }
    }
}