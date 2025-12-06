package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.ui.components.*
import com.example.doanck.utils.*
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

    // --- Mặc định nền trước khi có dữ liệu ---
    val defaultBackgroundData = WeatherBackground(
        effectType = WeatherEffectType.CLOUDY,
        gradientStartColor = 0xFFB0E0E6,
        gradientEndColor = 0xFFFFFACD
    )
    var currentBackgroundData by remember { mutableStateOf(defaultBackgroundData) }

    var locationData by remember { mutableStateOf<LocationData?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    // Yêu cầu permission ngay khi mở app
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Khi có permission → fetch location
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && locationData == null) {
            val loc = LocationHelper.fetchLocation(locationClient)
            if (loc != null) {
                val name = LocationHelper.getCityName(context, loc.latitude, loc.longitude)
                locationData = LocationData(loc.latitude, loc.longitude, name)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- Luôn vẽ background ---
        DynamicWeatherBackground(
            backgroundData = currentBackgroundData,
            modifier = Modifier.fillMaxSize()
        )

        when {
            !permissionGranted -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vui lòng cấp quyền vị trí để xem thời tiết", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            locationData == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            else -> {
                // Khi có location → load weather
                WeatherContent(
                    lat = locationData!!.lat,
                    lon = locationData!!.lon,
                    cityName = locationData!!.cityName,
                    onOpenCommunityChat = onOpenCommunityChat,
                    onBackgroundChange = { bg -> currentBackgroundData = bg }
                )
            }
        }
    }
}

@Composable
fun WeatherContent(
    lat: Double,
    lon: Double,
    cityName: String,
    onOpenCommunityChat: () -> Unit,
    onBackgroundChange: (WeatherBackground) -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lat, lon) {
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
                description = WeatherUtils.getDescriptionByCode(current.weatherCode),
                maxTemp = daily.maxTemperatures.first().roundToInt(),
                minTemp = daily.minTemperatures.first().roundToInt(),
                isDay = isDay
            )

            val currentHour = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            val startIndex = hourly.time.indexOfFirst { it.substring(11, 13).toInt() >= currentHour }
            val start = if (startIndex != -1) startIndex else 0

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in start until start + 24) {
                if (i >= hourly.time.size) break
                val hourLabel =
                    if (i == start) "Bây giờ" else hourly.time[i].substring(11, 13) + " giờ"

                // <<< ĐIỂM CHỈNH SỬA QUAN TRỌNG NHẤT >>>
                // Lấy trạng thái ngày/đêm của GIỜ [i] đó
                val isHourDay = hourly.isDayList[i] == 1
                // ------------------------------------

                hourlyList.add(
                    HourlyDisplayItem(
                        time = hourLabel,
                        temp = hourly.temperatures[i].roundToInt(),
                        // Dùng isHourDay thay vì isDay (của thời điểm hiện tại)
                        icon = WeatherUtils.getWeatherIcon(hourly.weatherCodes[i], isHourDay)
                    )
                )
            }

            val summary = WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)

            // Cập nhật background dựa theo thời tiết mới
            onBackgroundChange(WeatherUtils.getBackgroundData(current.weatherCode, isDay))

            weatherData = WeatherUIData(currentDisplay, hourlyList, summary)
            isLoading = false
        } catch (e: Exception) {
            errorText = "Lỗi tải dữ liệu: ${e.message}"
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            errorText != null -> {
                Text(
                    errorText!!,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(20.dp)
                )
            }
            weatherData != null -> {
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
                    HourlyForecastSection(data.summary, data.hourly)
                    Spacer(modifier = Modifier.height(50.dp))

                    Button(
                        onClick = onOpenCommunityChat,
                        modifier = Modifier.size(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Chat, "Chat", tint = Color.White)
                    }
                }
            }
        }
    }
}
