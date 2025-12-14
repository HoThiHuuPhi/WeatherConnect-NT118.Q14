package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.* import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
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
    onOpenSettings: () -> Unit = {}, // <--- 1. THÊM THAM SỐ NÀY
    onOpenWeatherMap: () -> Unit = {},
    onOpenRescueMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Khởi tạo DataStore & Lấy dữ liệu Setting
    val dataStore = remember { AppDataStore(context) }
    // Mặc định bật hiệu ứng (true), mặc định độ C
    val enableAnimation by dataStore.enableAnimation.collectAsState(initial = true)
    val tempUnit by dataStore.tempUnit.collectAsState(initial = "C")

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

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && locationData == null) {
            val loc = LocationHelper.fetchLocation(locationClient)
            val name = LocationHelper.getCityName(context, loc.latitude, loc.longitude)
            locationData = LocationData(loc.latitude, loc.longitude, name)
        }
    }

    var selectedTab by remember { mutableStateOf(MainTab.WEATHER) }
    var isWeatherReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 2. Xử lý logic hiển thị nền (Động hoặc Tĩnh) dựa vào Setting
        if (enableAnimation) {
            DynamicWeatherBackground(
                backgroundData = currentBackgroundData,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Nền tĩnh: Gradient đơn giản để tiết kiệm pin/RAM
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(currentBackgroundData.gradientStartColor),
                                Color(currentBackgroundData.gradientEndColor)
                            )
                        )
                    )
            )
        }

        when {
            !permissionGranted -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Vui lòng cấp quyền vị trí", color = Color.White)
                }
            }

            locationData == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            MainTab.WEATHER -> {
                                WeatherContent(
                                    lat = locationData!!.lat,
                                    lon = locationData!!.lon,
                                    cityName = locationData!!.cityName,
                                    tempUnit = tempUnit, // Truyền đơn vị xuống
                                    onBackgroundChange = { bg -> currentBackgroundData = bg },
                                    onContentReady = { isWeatherReady = true }
                                )
                            }

                            MainTab.SEARCH -> {
                                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) {
                                    Text("Màn TÌM KIẾM (Coming soon)", color = Color.Black)
                                }
                            }

                            // 2. CHỖ NÀY ĐỂ TRỐNG (Không vẽ Settings ở đây nữa)
                            MainTab.SETTINGS -> {
                                // Không làm gì cả vì Navigation sẽ chuyển màn hình ngay
                            }

                            MainTab.COMMUNITY -> { /* no-op */ }
                        }
                    }

                    if (isWeatherReady) {
                        Column {
                            MainTopNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        MainTab.WEATHER -> selectedTab = MainTab.WEATHER

                                        // Bấm Chat -> Chuyển màn hình
                                        MainTab.COMMUNITY -> onOpenCommunityChat()

                                        MainTab.SEARCH -> selectedTab = MainTab.SEARCH

                                        // 3. SỬA CHỖ NÀY: Bấm Settings -> Gọi hàm chuyển màn hình
                                        MainTab.SETTINGS -> onOpenSettings()
                                    }
                                },
                                onOpenWeatherMap = onOpenWeatherMap,
                                onOpenRescueMap = onOpenRescueMap
                            )
                            Spacer(Modifier.height(24.dp))
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
    tempUnit: String, // Nhận đơn vị (C hoặc F)
    onBackgroundChange: (WeatherBackground) -> Unit,
    onContentReady: () -> Unit = {}
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Hàm chuyển đổi nhiệt độ
    fun convertTemp(celsius: Double): Int {
        return if (tempUnit == "F") {
            (celsius * 1.8 + 32).roundToInt()
        } else {
            celsius.roundToInt()
        }
    }

    LaunchedEffect(lat, lon, tempUnit) {
        isLoading = true
        errorText = null

        try {
            val response = RetrofitClient.api.getWeather(lat, lon)
            val current = response.current
            val daily = response.daily
            val hourly = response.hourly

            val isDay = current.isDay == 1

            // Xử lý dữ liệu hiện tại
            val currentDisplay = CurrentDisplayData(
                cityName = cityName,
                currentTemp = convertTemp(current.temperature), // Convert
                description = WeatherUtils.getDescriptionByCode(current.weatherCode),
                maxTemp = convertTemp(daily.maxTemperatures.first()), // Convert
                minTemp = convertTemp(daily.minTemperatures.first()), // Convert
                isDay = isDay
            )

            // Xử lý dữ liệu hàng giờ
            val currentHour = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            val startIndex = hourly.time.indexOfFirst {
                it.substring(11, 13).toInt() >= currentHour
            }
            val start = if (startIndex != -1) startIndex else 0

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in start until start + 24) {
                if (i >= hourly.time.size) break
                val hourLabel = if (i == start) "Bây giờ" else hourly.time[i].substring(11, 13) + "h"
                val isHourDay = hourly.isDayList[i] == 1

                hourlyList.add(
                    HourlyDisplayItem(
                        time = hourLabel,
                        temp = convertTemp(hourly.temperatures[i]), // Convert
                        icon = WeatherUtils.getWeatherIcon(hourly.weatherCodes[i], isHourDay)
                    )
                )
            }

            val summary = WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)

            onBackgroundChange(WeatherUtils.getBackgroundData(current.weatherCode, isDay))
            weatherData = WeatherUIData(currentDisplay, hourlyList, summary)

        } catch (e: Exception) {
            errorText = "Lỗi: ${e.message}"
        } finally {
            isLoading = false
            onContentReady()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            errorText != null -> Text(errorText!!, color = Color.White, modifier = Modifier.align(Alignment.Center))
            weatherData != null -> {
                val data = weatherData!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- GỌI COMPOSABLE ĐÃ SỬA ---
                    MainWeatherDisplay(
                        data = data.current,
                        unit = tempUnit // Truyền đơn vị vào
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- GỌI COMPOSABLE ĐÃ SỬA ---
                    HourlyForecastSection(
                        summaryText = data.summary,
                        hourlyData = data.hourly,
                        unit = tempUnit // Truyền đơn vị vào
                    )

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}