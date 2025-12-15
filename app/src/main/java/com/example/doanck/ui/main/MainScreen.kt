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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDate
import kotlin.math.roundToInt

data class WeatherUIData(
    val current: CurrentDisplayData,
    val hourly: List<HourlyDisplayItem>,
    val daily: List<DailyDisplayItem>,
    val summary: String
)

@Composable
fun MainScreen(
    onOpenCommunityChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenWeatherMap: () -> Unit = {},
    onOpenRescueMap: () -> Unit = {},
    onNavigateToSOSMap: (Double, Double, String) -> Unit = { _, _, _ -> },
    onOpenRescueOverview: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { AppDataStore(context) }
    val networkMonitor = remember { NetworkMonitor(context) }

    val enableAnimation by dataStore.enableAnimation.collectAsState(initial = true)
    val tempUnit by dataStore.tempUnit.collectAsState(initial = "C")
    val isOnline by networkMonitor.isOnlineFlow.collectAsState(initial = false)
    val sosQueue by dataStore.sosQueue.collectAsState(initial = emptyList())

    var selectedDay by remember { mutableStateOf<DailyDisplayItem?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    var currentBackgroundData by remember { mutableStateOf(WeatherBackground(WeatherEffectType.CLOUDY, 0xFFB0E0E6, 0xFFFFFACD)) }
    var locationData by remember { mutableStateOf<LocationData?>(null) }
    var showSOSDialog by remember { mutableStateOf(false) }
    var showRescueMonitor by remember { mutableStateOf(false) }
    var isFlushingQueue by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline, sosQueue) {
        if (isOnline && sosQueue.isNotEmpty() && !isFlushingQueue) {
            isFlushingQueue = true
            val batch = Firebase.firestore.batch()
            sosQueue.forEach { sos -> batch.set(Firebase.firestore.collection("sos_requests").document(), sos) }
            batch.commit().addOnSuccessListener {
                scope.launch { dataStore.clearQueue(); Toast.makeText(context, "✅ Đã gửi ${sosQueue.size} SOS tồn đọng.", Toast.LENGTH_LONG).show() }
            }.addOnCompleteListener { isFlushingQueue = false }
        }
    }

    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }

    LaunchedEffect(Unit) { if (!permissionGranted) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && locationData == null) {
            val loc = LocationHelper.fetchLocation(context)
            val name = LocationHelper.getCityName(context, loc.latitude, loc.longitude)
            locationData = LocationData(loc.latitude, loc.longitude, name)
        }
    }

    var selectedTab by remember { mutableStateOf(MainTab.WEATHER) }
    var isWeatherReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (enableAnimation) {
            DynamicWeatherBackground(currentBackgroundData, Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(currentBackgroundData.gradientStartColor), Color(currentBackgroundData.gradientEndColor)))))
        }

        if (!permissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Vui lòng cấp quyền vị trí", color = Color.White) }
        } else if (locationData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = !isOnline || sosQueue.isNotEmpty()) { NetworkStatusHeader(isOnline, sosQueue.size) }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == MainTab.WEATHER) {
                        WeatherContentV2(
                            lat = locationData!!.lat,
                            lon = locationData!!.lon,
                            cityName = locationData!!.cityName,
                            tempUnit = tempUnit,
                            onBackgroundChange = { bg -> currentBackgroundData = bg },
                            onContentReady = { isWeatherReady = true },
                            onDayClick = { day -> selectedDay = day; showDetailSheet = true }
                        )
                    }
                }

                if (isWeatherReady) {
                    Column {
                        MainTopNavBar(
                            onTabSelected = { tab ->
                                when (tab) {
                                    MainTab.SETTINGS -> onOpenSettings()
                                    MainTab.COMMUNITY -> onOpenCommunityChat()
                                    MainTab.SEARCH -> onOpenSearch()
                                    else -> selectedTab = tab
                                }
                            },
                            onOpenWeatherMap = onOpenWeatherMap,
                            onOpenRescueMap = { showRescueMonitor = true }
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isWeatherReady,
            enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 130.dp, end = 28.dp)
        ) {
            FloatingActionButton(onClick = { showSOSDialog = true }, containerColor = Color(0xFFEF5350), contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Warning, contentDescription = "SOS")
            }
        }

        if (showSOSDialog && locationData != null) {
            SOSDialog(dataStore, networkMonitor, locationData!!.lat, locationData!!.lon, { showSOSDialog = false })
        }

        if (showRescueMonitor) {
            Dialog(onDismissRequest = { showRescueMonitor = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                SOSMonitorScreen(
                    onBack = { showRescueMonitor = false },
                    onNavigateToMap = { lat, lon, name -> showRescueMonitor = false; onNavigateToSOSMap(lat, lon, name) },
                    onOpenMapOverview = { showRescueMonitor = false; onOpenRescueOverview() }
                )
            }
        }

        if (showDetailSheet && selectedDay != null) {
            WeatherDetailBottomSheet(day = selectedDay!!, unit = tempUnit, onDismiss = { showDetailSheet = false })
        }
    }
}

// ... (Các phần NetworkStatusHeader và WeatherContentV2 giữ nguyên như cũ)
@Composable
fun NetworkStatusHeader(isOnline: Boolean, queueSize: Int) {
    val bgColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFF616161)
    val text = if (isOnline) "Đã có mạng - Đang gửi $queueSize tin SOS..." else "Mất kết nối - Đã lưu $queueSize tin chờ gửi"
    Row(Modifier.fillMaxWidth().background(bgColor).statusBarsPadding().padding(4.dp), Arrangement.Center, Alignment.CenterVertically) {
        Icon(if (isOnline) Icons.Default.SignalCellularAlt else Icons.Default.SignalCellularOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp)); Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeatherContentV2(
    lat: Double,
    lon: Double,
    cityName: String,
    tempUnit: String,
    onBackgroundChange: (WeatherBackground) -> Unit,
    onContentReady: () -> Unit,
    onDayClick: (DailyDisplayItem) -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun convertTemp(c: Double): Int = if (tempUnit == "F") (c * 1.8 + 32).roundToInt() else c.roundToInt()

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
            val startIndex = hourly.time.indexOfFirst { it.substring(11, 13).toInt() >= currentHour }.let { if (it != -1) it else 0 }

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in startIndex until startIndex + 24) {
                if (i >= hourly.time.size) break
                val hourLabel = if (i == startIndex) "Bây giờ" else hourly.time[i].substring(11, 13) + "h"
                val isHourDay = hourly.isDayList?.getOrNull(i) == 1 || (hourly.time[i].substring(11,13).toInt() in 6..18)

                hourlyList.add(HourlyDisplayItem(
                    time = hourLabel,
                    icon = WeatherUtils.getWeatherIcon(hourly.weatherCodes[i], isHourDay),
                    temp = convertTemp(hourly.temperatures[i])
                ))
            }

            val hourlyTempsByDate = mutableMapOf<String, MutableList<Int>>()
            val hourlyCodesByDate = mutableMapOf<String, MutableList<Int>>()
            hourly.time.forEachIndexed { idx, timeStr ->
                val dateKey = timeStr.substring(0, 10)
                hourlyTempsByDate.getOrPut(dateKey) { mutableListOf() }.add(convertTemp(hourly.temperatures[idx]))
                hourlyCodesByDate.getOrPut(dateKey) { mutableListOf() }.add(hourly.weatherCodes[idx])
            }

            val dailyItems = mutableListOf<DailyDisplayItem>()
            val viLocale = Locale("vi", "VN")
            val dayFormatter = DateTimeFormatter.ofPattern("E", viLocale)

            daily.time.forEachIndexed { index, dateStr ->
                val localDate = LocalDate.parse(dateStr)
                val label = when (index) {
                    0 -> "Hôm nay"
                    1 -> "Ngày mai"
                    else -> dayFormatter.format(localDate)
                }

                dailyItems.add(DailyDisplayItem(
                    dayLabel = label,
                    dateLabel = formatDateShort(dateStr),
                    icon = WeatherUtils.getWeatherIcon(daily.weatherCodes[index], true),
                    minTemp = convertTemp(daily.minTemperatures[index]),
                    maxTemp = convertTemp(daily.maxTemperatures[index]),
                    rainProbability = daily.rainProbabilities?.getOrNull(index),
                    rainSumMm = daily.rainSums?.getOrNull(index),
                    hourlyTemps = hourlyTempsByDate[dateStr] ?: emptyList(),
                    hourlyWeatherCodes = hourlyCodesByDate[dateStr] ?: emptyList(),
                    feelsLikeMin = daily.apparentTempMin?.getOrNull(index)?.let { convertTemp(it) },
                    feelsLikeMax = daily.apparentTempMax?.getOrNull(index)?.let { convertTemp(it) },
                    humidityMean = daily.humidityMean?.getOrNull(index)?.roundToInt(),
                    windSpeedMax = daily.windSpeedMax?.getOrNull(index)?.roundToInt()
                ))
            }

            val summary = WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)
            onBackgroundChange(WeatherUtils.getBackgroundData(current.weatherCode, isDay))
            weatherData = WeatherUIData(currentDisplay, hourlyList, dailyItems, summary)

        } catch (e: Exception) { errorText = "Lỗi: ${e.message}" } finally { isLoading = false; onContentReady() }
    }

    Box(Modifier.fillMaxSize()) {
        if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center), Color.White)
        else if (errorText != null) Text(errorText!!, color = Color.White, modifier = Modifier.align(Alignment.Center))
        else weatherData?.let { data ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainWeatherDisplay(data.current, tempUnit)
                Spacer(Modifier.height(24.dp))
                HourlyForecastSection(summaryText = data.summary, hourlyData = data.hourly, unit = tempUnit)
                Spacer(Modifier.height(12.dp))
                DailyForecastSection(items = data.daily, unit = tempUnit, onDayClick = onDayClick)
            }
        }
    }
}