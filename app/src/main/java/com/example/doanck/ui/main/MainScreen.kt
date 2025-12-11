package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import com.google.android.gms.location.LocationServices
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ----------------------------
// Data dùng cho UI
// ----------------------------
data class WeatherUIData(
    val current: CurrentDisplayData,
    val hourly: List<HourlyDisplayItem>,
    val summary: String
)

// ----------------------------
// Màn hình chính
// ----------------------------
@Composable
fun MainScreen(
    onOpenCommunityChat: () -> Unit = {},
    onOpenWeatherMap: () -> Unit = {},
    onOpenRescueMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Nền mặc định
    val defaultBackgroundData = WeatherBackground(
        effectType = WeatherEffectType.CLOUDY,
        gradientStartColor = 0xFFB0E0E6,
        gradientEndColor = 0xFFFFFACD
    )
    var currentBackgroundData by remember { mutableStateOf(defaultBackgroundData) }

    var locationData by remember { mutableStateOf<LocationData?>(null) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    // Xin quyền lần đầu
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Khi đã có quyền → lấy vị trí
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && locationData == null) {
            val loc = LocationHelper.fetchLocation(locationClient)
            val name = LocationHelper.getCityName(context, loc.latitude, loc.longitude)
            locationData = LocationData(loc.latitude, loc.longitude, name)
        }
    }

    // State tab + trạng thái load thời tiết
    var selectedTab by remember { mutableStateOf(MainTab.WEATHER) }
    var isWeatherReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Background động
        DynamicWeatherBackground(
            backgroundData = currentBackgroundData,
            modifier = Modifier.fillMaxSize()
        )

        when {
            // Chưa cho quyền
            !permissionGranted -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Vui lòng cấp quyền vị trí để xem thời tiết",
                        color = Color.White
                    )
                }
            }

            // Đang lấy GPS
            locationData == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Đã có location → show nội dung + nav bar
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ------------ VÙNG NỘI DUNG (trên) ------------
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            MainTab.WEATHER -> {
                                WeatherContent(
                                    lat = locationData!!.lat,
                                    lon = locationData!!.lon,
                                    cityName = locationData!!.cityName,
                                    onBackgroundChange = { bg -> currentBackgroundData = bg },
                                    onContentReady = { isWeatherReady = true }
                                )
                            }

                            MainTab.SEARCH -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(120.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "Màn TÌM KIẾM sẽ được nhóm em bổ sung sau.",
                                        color = Color.Black
                                    )
                                }
                            }

                            MainTab.SETTINGS -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(120.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "Màn CÀI ĐẶT (đơn vị, AI Stylist, thông báo bão...) sẽ bổ sung sau.",
                                        color = Color.Black
                                    )
                                }
                            }

                            // Chat cộng đồng – chuyển qua màn khác nên không vẽ gì ở đây
                            MainTab.COMMUNITY -> { /* no-op */ }
                        }
                    }

                    // ------------ NAV BAR DƯỚI ------------
                    if (isWeatherReady) {
                        Column {                       // bọc thêm 1 Column nhỏ
                            MainTopNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        MainTab.WEATHER   -> selectedTab = MainTab.WEATHER
                                        MainTab.COMMUNITY -> onOpenCommunityChat()
                                        MainTab.SEARCH    -> selectedTab = MainTab.SEARCH
                                        MainTab.SETTINGS  -> selectedTab = MainTab.SETTINGS
                                    }
                                },
                                onOpenWeatherMap = onOpenWeatherMap,
                                onOpenRescueMap = onOpenRescueMap
                            )
                            Spacer(Modifier.height(24.dp))   // 👈 khoảng cách với lề dưới
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------
// Nội dung phần THỜI TIẾT
// ----------------------------
@Composable
fun WeatherContent(
    lat: Double,
    lon: Double,
    cityName: String,
    onBackgroundChange: (WeatherBackground) -> Unit,
    onContentReady: () -> Unit = {}
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

            // Lấy 24h kế tiếp
            val currentHour =
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            val startIndex = hourly.time.indexOfFirst {
                it.substring(11, 13).toInt() >= currentHour
            }
            val start = if (startIndex != -1) startIndex else 0

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in start until start + 24) {
                if (i >= hourly.time.size) break
                val hourLabel =
                    if (i == start) "Bây giờ" else hourly.time[i].substring(11, 13) + " giờ"

                val isHourDay = hourly.isDayList[i] == 1

                hourlyList.add(
                    HourlyDisplayItem(
                        time = hourLabel,
                        temp = hourly.temperatures[i].roundToInt(),
                        icon = WeatherUtils.getWeatherIcon(
                            hourly.weatherCodes[i],
                            isHourDay
                        )
                    )
                )
            }

            val summary =
                WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)

            // Cập nhật nền
            onBackgroundChange(
                WeatherUtils.getBackgroundData(
                    current.weatherCode,
                    isDay
                )
            )

            weatherData = WeatherUIData(currentDisplay, hourlyList, summary)
        } catch (e: Exception) {
            errorText = "Lỗi tải dữ liệu: ${e.message}"
        } finally {
            isLoading = false
            onContentReady()      // ✅ báo cho MainScreen: đã xong (dù thành công hay lỗi)
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
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
                }
            }
        }
    }
}
