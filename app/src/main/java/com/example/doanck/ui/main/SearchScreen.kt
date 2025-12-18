package com.example.doanck.ui.main

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.data.api.RetrofitClient
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.data.model.CurrentWeather
import com.example.doanck.data.model.DailyUnits
import com.example.doanck.data.model.HourlyUnits
import com.example.doanck.ui.DynamicWeatherBackground
import com.example.doanck.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { AppDataStore(context) }

    val tempUnit by dataStore.tempUnit.collectAsState(initial = "C")
    val enableAnimation by dataStore.enableAnimation.collectAsState(initial = true)

    var searchText by remember { mutableStateOf("") }
    val searchSuggestions = remember { mutableStateListOf<LocationData>() }
    var selectedLocation by remember { mutableStateOf<LocationData?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var currentBackgroundData by remember {
        mutableStateOf(WeatherBackground(WeatherEffectType.CLOUDY, 0xFFB0E0E6, 0xFFFFFACD))
    }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchSuggestions.clear()
            return
        }
        isSearching = true
        searchError = null
        if (selectedLocation != null) selectedLocation = null

        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5)
                withContext(Dispatchers.Main) {
                    searchSuggestions.clear()
                    if (!addresses.isNullOrEmpty()) {
                        addresses.forEach {
                            val parts = listOfNotNull(
                                it.featureName,
                                it.subAdminArea,
                                it.adminArea,
                                it.countryName
                            )
                            searchSuggestions.add(
                                LocationData(
                                    it.latitude,
                                    it.longitude,
                                    parts.distinct().joinToString(", ")
                                )
                            )
                        }
                    }
                    isSearching = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    searchError = "Lỗi: ${e.message}"
                    isSearching = false
                }
            }
        }
    }

    LaunchedEffect(searchText) {
        if (searchText.length > 1) {
            delay(800)
            performSearch(searchText)
        } else {
            searchSuggestions.clear()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
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

        Column(modifier = Modifier.fillMaxSize()) {
            // header search bảr
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB0E0E6),
                                    Color(0xFF87CEEB),
                                    Color(0xFFFFFACD)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // NÚT BACK
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // text field tìm kiếm
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                if (selectedLocation != null) selectedLocation = null
                            },
                            placeholder = {
                                Text(
                                    "Tìm kiếm...",
                                    color = Color(0xFF1565C0).copy(0.6f),
                                    fontSize = 16.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color(0xFF1976D2),
                                focusedTextColor = Color(0xFF1565C0),
                                unfocusedTextColor = Color(0xFF424242)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    performSearch(searchText)
                                    keyboardController?.hide()
                                }
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        // Button tìm kiếm
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = Color(0xFF1976D2)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 4.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        performSearch(searchText)
                                        keyboardController?.hide()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        "Search",
                                        tint = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    selectedLocation != null -> {
                        SearchWeatherResult(
                            selectedLocation!!.lat,
                            selectedLocation!!.lon,
                            selectedLocation!!.cityName.split(",").firstOrNull()
                                ?: selectedLocation!!.cityName,
                            tempUnit
                        ) { currentBackgroundData = it }
                    }
                    searchSuggestions.isNotEmpty() -> {
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            item {
                                Text(
                                    "Kết quả tìm kiếm:",
                                    color = Color(0xFF1565C0),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                                )
                            }
                            items(searchSuggestions) { loc ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(0.25f),
                                                    Color.Black.copy(0.15f)
                                                )
                                            )
                                        )
                                        .clickable {
                                            selectedLocation = loc
                                            keyboardController?.hide()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1976D2),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            loc.cityName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    searchError != null -> {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                searchError!!,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    else -> {
                        Column(
                            Modifier
                                .align(Alignment.Center)
                                .offset(y = (-50).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = Color.White.copy(0.3f),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchText.isBlank())
                                    "Nhập địa điểm để xem thời tiết"
                                else
                                    "Không tìm thấy kết quả",
                                color = Color.White.copy(0.8f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchWeatherResult(
    lat: Double,
    lon: Double,
    cityName: String,
    tempUnit: String,
    onBackgroundChange: (WeatherBackground) -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherUIData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedDay by remember { mutableStateOf<DailyDisplayItem?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    var rawCurrent by remember { mutableStateOf<CurrentWeather?>(null) }
    var rawDaily by remember { mutableStateOf<DailyUnits?>(null) }
    var rawHourly by remember { mutableStateOf<HourlyUnits?>(null) }
    var elevationM by remember { mutableStateOf<Double?>(null) }
    var currentStartIndex by remember { mutableIntStateOf(0) }

    fun convertTemp(c: Double?): Int {
        if (c == null) return 0
        return if (tempUnit == "F") (c * 1.8 + 32).roundToInt() else c.roundToInt()
    }

    LaunchedEffect(lat, lon, tempUnit) {
        isLoading = true
        errorMessage = null

        try {
            val response = RetrofitClient.api.getWeather(lat, lon)
            val current = response.current
            val daily = response.daily
            val hourly = response.hourly

            if (current == null || daily == null || hourly == null) {
                throw IllegalStateException("API trả về dữ liệu không đầy đủ.")
            }

            val currentTimeStr = current.time
            val currentHour = try {
                currentTimeStr.substring(11, 13).toInt()
            } catch (e: Exception) {
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt()
            }

            val startIndex = hourly.time.indexOfFirst {
                try {
                    it.substring(11, 13).toInt() >= currentHour
                } catch (e: Exception) {
                    false
                }
            }.let { if (it != -1) it else 0 }

            rawCurrent = current
            rawDaily = daily
            rawHourly = hourly
            elevationM = response.elevation
            currentStartIndex = startIndex

            val isDay = current.isDay == 1

            onBackgroundChange(WeatherUtils.getBackgroundData(current.weatherCode, isDay))

            val currentDisplay = CurrentDisplayData(
                cityName,
                convertTemp(current.temperature),
                WeatherUtils.getDescriptionByCode(current.weatherCode),
                convertTemp(daily.maxTemperatures.firstOrNull()),
                convertTemp(daily.minTemperatures.firstOrNull()),
                isDay
            )

            val hourlyList = mutableListOf<HourlyDisplayItem>()
            for (i in startIndex until startIndex + 24) {
                if (i >= hourly.time.size) break
                val hourLabel = if (i == startIndex) "Bây giờ"
                else hourly.time.getOrElse(i) { "00h" }.substring(11, 13) + "h"
                val isHourDay = hourly.isDayList.getOrNull(i) == 1
                val temp = convertTemp(hourly.temperatures.getOrNull(i))
                val code = hourly.weatherCodes.getOrElse(i) { 0 }

                hourlyList.add(
                    HourlyDisplayItem(
                        hourLabel,
                        WeatherUtils.getWeatherIcon(code, isHourDay),
                        temp
                    )
                )
            }

            val hourlyTempsByDate = mutableMapOf<String, MutableList<Int>>()
            val hourlyCodesByDate = mutableMapOf<String, MutableList<Int>>()

            hourly.time.forEachIndexed { idx, timeStr ->
                if (idx < hourly.temperatures.size && idx < hourly.weatherCodes.size) {
                    val dateKey = timeStr.substring(0, 10)
                    hourlyTempsByDate.getOrPut(dateKey) { mutableListOf() }
                        .add(convertTemp(hourly.temperatures.getOrNull(idx)))
                }
            }

            val dailyItems = mutableListOf<DailyDisplayItem>()
            val viLocale = Locale("vi", "VN")
            val dayFormatter = DateTimeFormatter.ofPattern("E", viLocale)

            daily.time.forEachIndexed { index, dateStr ->
                val localDate = java.time.LocalDate.parse(dateStr)
                val label = when (index) {
                    0 -> "Hôm nay"
                    1 -> "Ngày mai"
                    else -> dayFormatter.format(localDate)
                }

                dailyItems.add(
                    DailyDisplayItem(
                        dayLabel = label,
                        dateLabel = formatDateShort(dateStr),
                        icon = WeatherUtils.getWeatherIcon(
                            daily.weatherCodes.getOrElse(index) { 0 },
                            true
                        ),
                        minTemp = convertTemp(daily.minTemperatures.getOrNull(index)),
                        maxTemp = convertTemp(daily.maxTemperatures.getOrNull(index)),
                        rainProbability = daily.rainProbabilities?.getOrNull(index),
                        rainSumMm = daily.rainSums?.getOrNull(index),
                        hourlyTemps = hourlyTempsByDate[dateStr] ?: emptyList(),
                        hourlyWeatherCodes = hourlyCodesByDate[dateStr] ?: emptyList(),
                        snowfallSum = daily.snowfallSum?.getOrNull(index)
                    )
                )
            }

            val summary = WeatherUtils.generateSummaryText(
                hourly.weatherCodes,
                hourly.windGusts
            )

            weatherData = WeatherUIData(currentDisplay, hourlyList, dailyItems, summary)

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Lỗi tải dữ liệu: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (errorMessage != null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Không thể hiển thị thời tiết",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                errorMessage!!,
                color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    } else weatherData?.let { data ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 0.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainWeatherDisplay(data.current, tempUnit)
                Spacer(Modifier.height(36.dp))
                HourlyForecastSection(data.summary, data.hourly, tempUnit)
                Spacer(Modifier.height(24.dp))
                DailyForecastSection(
                    items = data.daily,
                    unit = tempUnit,
                    onDayClick = { day -> selectedDay = day; showDetailSheet = true }
                )

                if (rawCurrent != null && rawDaily != null && rawHourly != null) {
                    val c = rawCurrent!!
                    val d = rawDaily!!
                    val h = rawHourly!!

                    WeatherCardsSection(
                        feelsLike = convertTemp(c.apparentTemperature ?: c.temperature),
                        actual = convertTemp(c.temperature),
                        dayMin = convertTemp(d.minTemperatures.firstOrNull() ?: 0.0),
                        dayMax = convertTemp(d.maxTemperatures.firstOrNull() ?: 0.0),
                        uvMax = d.uvIndexMax?.firstOrNull()?.toFloat(),
                        windSpeedKmh = c.windSpeed10m?.roundToInt(),
                        windGustKmh = c.windGusts10m?.roundToInt(),
                        windDirDeg = c.windDirection10m?.roundToInt(),
                        sunriseHHmm = toHHmm(d.sunrise?.firstOrNull()),
                        sunsetHHmm = toHHmm(d.sunset?.firstOrNull()),
                        rainMm = c.rain ?: c.precipitation,
                        rainSumMm = d.rainSums?.firstOrNull() ?: d.rainSum?.firstOrNull(),
                        snowfallMm = d.snowfallSum?.firstOrNull() ?: c.snowfall,
                        humidityPercent = c.humidity,
                        pressureMslHPa = c.pressureMsl,
                        pressureHPa = c.pressure,
                        elevationM = elevationM,
                        cape = h.cape?.getOrNull(currentStartIndex),
                        cloudCover = c.cloudCover,
                        cloudLow = h.cloudCoverLow?.getOrNull(currentStartIndex),
                        cloudMid = h.cloudCoverMid?.getOrNull(currentStartIndex),
                        cloudHigh = h.cloudCoverHigh?.getOrNull(currentStartIndex),
                        soilMoisture0_1 = h.soilMoisture0to1?.getOrNull(currentStartIndex),
                        soilMoisture1_3 = h.soilMoisture3to9?.getOrNull(currentStartIndex),
                        soilMoisture3_9 = h.soilMoisture9to27?.getOrNull(currentStartIndex),
                        dewPoint = c.dewPoint2m,
                        sunshineDurationSeconds = d.sunshineDuration?.firstOrNull()
                    )
                }
            }
        }

        if (showDetailSheet && selectedDay != null) {
            WeatherDetailBottomSheet(
                day = selectedDay!!,
                unit = tempUnit,
                onDismiss = { showDetailSheet = false }
            )
        }
    }
}