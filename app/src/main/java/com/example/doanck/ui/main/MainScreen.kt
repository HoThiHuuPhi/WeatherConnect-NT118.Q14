package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    onOpenSettings: () -> Unit = {},
    onOpenWeatherMap: () -> Unit = {},
    onOpenRescueMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Khởi tạo DataStore & NetworkMonitor
    val dataStore = remember { AppDataStore(context) }
    val networkMonitor = remember { NetworkMonitor(context) } // Theo dõi mạng

    val enableAnimation by dataStore.enableAnimation.collectAsState(initial = true)
    val tempUnit by dataStore.tempUnit.collectAsState(initial = "C")

    // Lấy trạng thái mạng và hàng chờ SOS
    val isOnline by networkMonitor.isOnlineFlow.collectAsState(initial = false)
    val sosQueue by dataStore.sosQueue.collectAsState(initial = emptyList())

    // ✅ CHỐNG GỬI TRÙNG QUEUE
    var isFlushingQueue by remember { mutableStateOf(false) }

    // ✅ LOGIC TỰ ĐỘNG GỬI (AUTO-SYNC) - an toàn, không gửi trùng
    LaunchedEffect(isOnline, sosQueue) {
        if (isOnline && sosQueue.isNotEmpty() && !isFlushingQueue) {
            isFlushingQueue = true

            val batch = Firebase.firestore.batch()
            sosQueue.forEach { sos ->
                val docRef = Firebase.firestore.collection("sos_requests").document()
                batch.set(docRef, sos)
            }

            batch.commit()
                .addOnSuccessListener {
                    scope.launch {
                        dataStore.clearQueue()
                        Toast.makeText(
                            context,
                            "✅ Đã có mạng! Đã gửi ${sosQueue.size} SOS tồn đọng.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "⚠️ Gửi SOS tồn đọng thất bại, sẽ thử lại khi mạng ổn định.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnCompleteListener {
                    isFlushingQueue = false
                }
        }
    }

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentBackgroundData by remember {
        mutableStateOf(
            WeatherBackground(
                WeatherEffectType.CLOUDY,
                0xFFB0E0E6,
                0xFFFFFACD
            )
        )
    }
    var locationData by remember { mutableStateOf<LocationData?>(null) }
    var showSOSDialog by remember { mutableStateOf(false) }

    // Xử lý quyền vị trí
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted = it
        }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

        // LAYER 1: NỀN
        if (enableAnimation) {
            DynamicWeatherBackground(currentBackgroundData, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(currentBackgroundData.gradientStartColor),
                                Color(currentBackgroundData.gradientEndColor)
                            )
                        )
                    )
            )
        }

        // LAYER 2: NỘI DUNG CHÍNH
        if (!permissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vui lòng cấp quyền vị trí", color = Color.White)
            }
        } else if (locationData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // THANH TRẠNG THÁI MẠNG (hiện khi mất mạng hoặc đang có queue)
                AnimatedVisibility(visible = !isOnline || sosQueue.isNotEmpty()) {
                    NetworkStatusHeader(isOnline, sosQueue.size)
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        MainTab.WEATHER -> {
                            WeatherContent(
                                lat = locationData!!.lat,
                                lon = locationData!!.lon,
                                cityName = locationData!!.cityName,
                                tempUnit = tempUnit,
                                onBackgroundChange = { bg -> currentBackgroundData = bg },
                                onContentReady = { isWeatherReady = true }
                            )
                        }

                        MainTab.SEARCH -> Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 100.dp),
                            Alignment.TopCenter
                        ) {
                            Text("Màn TÌM KIẾM (Coming soon)", color = Color.Black)
                        }

                        MainTab.SETTINGS -> {
                            // không dùng vì settings mở bằng nav
                        }

                        MainTab.COMMUNITY -> {
                            ConnectScreen(onOpenSettings = onOpenSettings)
                        }
                    }
                }

                if (isWeatherReady) {
                    Column {
                        MainTopNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab ->
                                when (tab) {
                                    MainTab.WEATHER -> selectedTab = MainTab.WEATHER
                                    MainTab.COMMUNITY -> selectedTab = MainTab.COMMUNITY
                                    MainTab.SEARCH -> selectedTab = MainTab.SEARCH
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

        // NÚT SOS NỔI (Góc trên phải)
        FloatingActionButton(
            onClick = { showSOSDialog = true },
            containerColor = Color(0xFFEF5350),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 20.dp)
                .size(50.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = "SOS")
        }

        // ✅ DIALOG SOS - truyền dataStore + networkMonitor
        if (showSOSDialog && locationData != null) {
            SOSDialog(
                appDataStore = dataStore,
                networkMonitor = networkMonitor,
                lat = locationData!!.lat,
                lon = locationData!!.lon,
                onDismiss = { showSOSDialog = false }
            )
        }
    }
}

// UI THANH TRẠNG THÁI MẠNG
@Composable
fun NetworkStatusHeader(isOnline: Boolean, queueSize: Int) {
    val bgColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFF616161)
    val text =
        if (isOnline) "Đã có mạng - Đang gửi $queueSize tin SOS..."
        else "Mất kết nối - Đã lưu $queueSize tin chờ gửi"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isOnline) Icons.Default.SignalCellularAlt else Icons.Default.SignalCellularOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ----------------------------
// Nội dung phần THỜI TIẾT (Giữ nguyên)
// ----------------------------
@Composable
fun WeatherContent(
    lat: Double,
    lon: Double,
    cityName: String,
    tempUnit: String,
    onBackgroundChange: (WeatherBackground) -> Unit,
    onContentReady: () -> Unit = {}
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun convertTemp(celsius: Double): Int {
        return if (tempUnit == "F") (celsius * 1.8 + 32).roundToInt() else celsius.roundToInt()
    }

    LaunchedEffect(lat, lon, tempUnit) {
        isLoading = true; errorText = null
        try {
            val response = RetrofitClient.api.getWeather(lat, lon)
            val current = response.current
            val daily = response.daily
            val hourly = response.hourly

            val isDay = current.isDay == 1

            val currentDisplay = CurrentDisplayData(
                cityName,
                convertTemp(current.temperature),
                WeatherUtils.getDescriptionByCode(current.weatherCode),
                convertTemp(daily.maxTemperatures.first()),
                convertTemp(daily.minTemperatures.first()),
                isDay
            )

            val currentHour = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            val startIndex = hourly.time.indexOfFirst { it.substring(11, 13).toInt() >= currentHour }
                .let { if (it != -1) it else 0 }

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in startIndex until startIndex + 24) {
                if (i >= hourly.time.size) break
                val hourLabel = if (i == startIndex) "Bây giờ" else hourly.time[i].substring(11, 13) + "h"
                val isHourDay = hourly.isDayList[i] == 1
                hourlyList.add(
                    HourlyDisplayItem(
                        hourLabel,
                        convertTemp(hourly.temperatures[i]),
                        WeatherUtils.getWeatherIcon(hourly.weatherCodes[i], isHourDay)
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
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )

            errorText != null -> Text(
                errorText!!,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            weatherData != null -> {
                val data = weatherData!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MainWeatherDisplay(data.current, tempUnit)
                    Spacer(Modifier.height(40.dp))
                    HourlyForecastSection(data.summary, data.hourly, tempUnit)
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
    }
}
