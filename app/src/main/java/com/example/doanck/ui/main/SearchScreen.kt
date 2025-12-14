package com.example.doanck.ui.main

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // ✅ ĐÃ THÊM CÁI NÀY
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dataStore = remember { AppDataStore(context) }

    // Lấy cài đặt đơn vị nhiệt độ
    val tempUnit by dataStore.tempUnit.collectAsState(initial = "C")
    val enableAnimation by dataStore.enableAnimation.collectAsState(initial = true)

    var searchText by remember { mutableStateOf("") }
    var searchLocation by remember { mutableStateOf<LocationData?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Nền mặc định cho màn hình tìm kiếm
    var currentBackgroundData by remember { mutableStateOf(WeatherBackground(WeatherEffectType.CLOUDY, 0xFFB0E0E6, 0xFFFFFACD)) }

    fun performSearch() {
        if (searchText.isBlank()) return
        isSearching = true
        searchError = null
        searchLocation = null
        keyboardController?.hide()

        Thread {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                // Suppress warning deprecated cho Geocoder cũ
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(searchText, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val cityName = address.adminArea ?: address.locality ?: address.featureName ?: searchText
                    searchLocation = LocationData(address.latitude, address.longitude, cityName)
                } else {
                    searchError = "Không tìm thấy địa điểm '$searchText'"
                }
            } catch (e: Exception) {
                searchError = "Lỗi kết nối: ${e.message}"
            } finally {
                isSearching = false
            }
        }.start()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Nền động (hoặc tĩnh)
        if (enableAnimation) {
            DynamicWeatherBackground(currentBackgroundData, Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(currentBackgroundData.gradientStartColor), Color(currentBackgroundData.gradientEndColor)))))
        }

        Scaffold(
            containerColor = Color.Transparent, // Để hiện nền bên dưới
            topBar = {
                // Thanh tìm kiếm tùy chỉnh
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            // Dùng AutoMirrored.Filled.ArrowBack để tránh warning deprecated
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                        }

                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Nhập tên thành phố...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color.Black,
                                focusedTextColor = Color.Black
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performSearch() })
                        )

                        IconButton(onClick = { performSearch() }) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Blue)
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    searchError != null -> {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            // Sẽ hết lỗi .sp tại đây
                            Text(searchError!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    searchLocation != null -> {
                        // Hiển thị kết quả thời tiết
                        SearchWeatherResult(
                            lat = searchLocation!!.lat,
                            lon = searchLocation!!.lon,
                            cityName = searchLocation!!.cityName,
                            tempUnit = tempUnit,
                            onBackgroundChange = { currentBackgroundData = it }
                        )
                    }
                    else -> {
                        // Màn hình chờ
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(80.dp))
                            Spacer(Modifier.height(16.dp))
                            // Sẽ hết lỗi .sp tại đây
                            Text("Nhập địa điểm để xem thời tiết", color = Color.White.copy(0.8f), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// Component hiển thị thời tiết cho màn hình tìm kiếm (Tương tự WeatherContent bên Main)
@Composable
fun SearchWeatherResult(
    lat: Double, lon: Double, cityName: String, tempUnit: String,
    onBackgroundChange: (WeatherBackground) -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun convertTemp(c: Double): Int = if (tempUnit == "F") (c * 1.8 + 32).roundToInt() else c.roundToInt()

    LaunchedEffect(lat, lon, tempUnit) {
        isLoading = true
        try {
            val response = RetrofitClient.api.getWeather(lat, lon)
            val current = response.current; val daily = response.daily; val hourly = response.hourly; val isDay = current.isDay == 1

            // Cập nhật background cho màn hình tìm kiếm
            onBackgroundChange(WeatherUtils.getBackgroundData(current.weatherCode, isDay))

            val currentDisplay = CurrentDisplayData(cityName, convertTemp(current.temperature), WeatherUtils.getDescriptionByCode(current.weatherCode), convertTemp(daily.maxTemperatures.first()), convertTemp(daily.minTemperatures.first()), isDay)
            val currentHour = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            val startIndex = hourly.time.indexOfFirst { it.substring(11, 13).toInt() >= currentHour }.let { if (it != -1) it else 0 }
            val hourlyList = mutableListOf<HourlyDisplayItem>()

            for (i in startIndex until startIndex + 24) {
                if (i >= hourly.time.size) break
                val hourLabel = if (i == startIndex) "Bây giờ" else hourly.time[i].substring(11, 13) + "h"
                val isHourDay = hourly.isDayList[i] == 1
                hourlyList.add(HourlyDisplayItem(hourLabel, WeatherUtils.getWeatherIcon(hourly.weatherCodes[i], isHourDay), convertTemp(hourly.temperatures[i])))
            }
            val summary = WeatherUtils.generateSummaryText(hourly.weatherCodes, hourly.windGusts)
            weatherData = WeatherUIData(currentDisplay, hourlyList, summary)
        } catch (e: Exception) {
            // Xử lý lỗi
        } finally { isLoading = false }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
    } else weatherData?.let { data ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MainWeatherDisplay(data.current, tempUnit)
            Spacer(Modifier.height(40.dp))
            HourlyForecastSection(data.summary, data.hourly, tempUnit)
            Spacer(Modifier.height(50.dp))
        }
    }
}