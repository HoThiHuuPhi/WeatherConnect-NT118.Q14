package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.ui.components.MainWeatherDisplay
import com.example.doanck.ui.components.HourlyForecastSection
import com.example.doanck.ui.components.CurrentDisplayData
import com.example.doanck.ui.components.HourlyDisplayItem
import com.example.doanck.utils.LocationHelper
import com.example.doanck.utils.LocationData
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.WeatherBackground
import com.example.doanck.utils.WeatherEffectType
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class WeatherUIData(
    val current: CurrentDisplayData,
    val hourly: List<HourlyDisplayItem>,
    val summary: String
)

@Composable
fun MainScreen(
    onOpenCommunityChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var locationData by remember {
        mutableStateOf<LocationData?>(LocationData(10.8231, 106.6297, "Hồ Chí Minh"))
    }

    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val newLocation = LocationHelper.fetchLocation(locationClient)
            if (newLocation != null) {
                val cityName = LocationHelper.getCityName(context, newLocation.latitude, newLocation.longitude)
                locationData = LocationData(newLocation.latitude, newLocation.longitude, cityName)
            }
        }
    }

    locationData?.let { data ->
        WeatherContent(lat = data.lat, lon = data.lon, cityName = data.cityName, requiresPermission = !permissionGranted, onOpenCommunityChat)
    }
}

@Composable
fun WeatherContent(
    lat: Double,
    lon: Double,
    cityName: String,
    requiresPermission: Boolean,
    onOpenCommunityChat: () -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val defaultBackgroundData = WeatherBackground(
        effectType = WeatherEffectType.SUNNY,
        gradientStartColor = 0xFF4A90E2,
        gradientEndColor = 0xFF8AC7FF
    )

    var currentBackgroundData by remember { mutableStateOf(defaultBackgroundData) }

    LaunchedEffect(lat, lon) {
        if (requiresPermission) {
            isLoading = false
            errorText = "Vui lòng cấp quyền vị trí để xem thời tiết hiện tại."
            currentBackgroundData = defaultBackgroundData
            return@LaunchedEffect
        }

        isLoading = true
        errorText = null

        try {
            val response = RetrofitClient.api.getWeather(lat, lon)
            val current = response.current
            val daily = response.daily
            val hourly = response.hourly

            val isDay = current.isDay == 1
            val currentDisplay = CurrentDisplayData(
                cityName = cityName,
                currentTemp = current.temperature.roundToInt(),
                description = com.example.doanck.utils.WeatherUtils.getDescriptionByCode(current.weatherCode),
                maxTemp = daily.maxTemperatures.first().roundToInt(),
                minTemp = daily.minTemperatures.first().roundToInt(),
                isDay = isDay
            )

            val currentDateTime = ZonedDateTime.now()
            val currentHour = currentDateTime.format(DateTimeFormatter.ofPattern("HH")).toInt()

            val startIndex = hourly.time.indexOfFirst { rawTime ->
                rawTime.substring(11, 13).toInt() >= currentHour
            }
            val start = if (startIndex != -1) startIndex else 0

            val processedHourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in start until start + 24) {
                if (i < hourly.time.size) {
                    val rawTime = hourly.time[i]
                    val hourString = rawTime.substring(11, 13)
                    val displayTime = if (i == start) "Bây giờ" else "$hourString giờ"
                    val temperature = hourly.temperatures.getOrNull(i)?.roundToInt() ?: 0
                    val weatherCode = hourly.weatherCodes.getOrNull(i) ?: 0

                    processedHourlyList.add(
                        HourlyDisplayItem(
                            time = displayTime,
                            temp = temperature,
                            icon = com.example.doanck.utils.WeatherUtils.getIconByCode(weatherCode)
                        )
                    )
                }
            }

            val summary = com.example.doanck.utils.WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)
            currentBackgroundData = com.example.doanck.utils.WeatherUtils.getBackgroundData(current.weatherCode, isDay)

            weatherData = WeatherUIData(currentDisplay, processedHourlyList, summary)
            isLoading = false

        } catch (e: Exception) {
            errorText = "Lỗi tải dữ liệu: ${e.message}. Vui lòng kiểm tra Internet."
            isLoading = false
            currentBackgroundData = defaultBackgroundData
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Nền động
        DynamicWeatherBackground(
            backgroundData = currentBackgroundData,
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (errorText != null) {
            Text(text = errorText!!, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp))
        } else {
            val data = weatherData!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainWeatherDisplay(data.current)
                Spacer(modifier = Modifier.height(40.dp))
                HourlyForecastSection(summaryText = data.summary, hourlyData = data.hourly)
                Spacer(modifier = Modifier.height(50.dp))

                // Button Chat cộng đồng với icon tròn
                Button(
                    onClick = onOpenCommunityChat,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    // Preview với dữ liệu giả lập
    val dummyCurrent = CurrentDisplayData(
        cityName = "Hồ Chí Minh",
        currentTemp = 30,
        description = "Nắng nhẹ",
        maxTemp = 32,
        minTemp = 28,
        isDay = true
    )

    val dummyHourly = List(24) { i ->
        HourlyDisplayItem(
            time = if (i == 0) "Bây giờ" else "${i} giờ",
            temp = 28 + i % 5,
            icon = Icons.Default.Chat        )
    }

    val dummyWeatherData = WeatherUIData(
        current = dummyCurrent,
        hourly = dummyHourly,
        summary = "Trời nắng, gió nhẹ"
    )

    var backgroundData = WeatherBackground(
        effectType = WeatherEffectType.SUNNY,
        gradientStartColor = 0xFF4A90E2,
        gradientEndColor = 0xFF8AC7FF
    )

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicWeatherBackground(backgroundData, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainWeatherDisplay(dummyWeatherData.current)
            Spacer(modifier = Modifier.height(40.dp))
            HourlyForecastSection(
                summaryText = dummyWeatherData.summary,
                hourlyData = dummyWeatherData.hourly
            )
            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = { /* preview click */ },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
            }
        }
    }
}

