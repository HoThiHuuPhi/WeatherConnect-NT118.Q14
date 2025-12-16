package com.example.doanck.ui.main

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.util.Locale

@Composable
fun LocationPicker(
    initialLat: Double,
    initialLon: Double,
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var addressPreview by remember { mutableStateOf("Đang xác định vị trí...") }
    var addressJob by remember { mutableStateOf<Job?>(null) }

    // 👇 STATE MỚI CHO THANH TÌM KIẾM
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    // 👆 STATE MỚI CHO THANH TÌM KIẾM

    // Hàm lấy địa chỉ từ tọa độ
    fun getAddress(lat: Double, lon: Double) {
        addressJob?.cancel()
        addressJob = scope.launch(Dispatchers.IO) {
            try {
                delay(800)
                withContext(Dispatchers.Main) { addressPreview = "Đang tải tên đường..." }

                val geocoder = Geocoder(context, Locale("vi", "VN"))
                val addresses = geocoder.getFromLocation(lat, lon, 1)

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        addressPreview = addr.getAddressLine(0) ?: addr.featureName ?: "Vị trí đã chọn"
                    } else {
                        addressPreview = "Tọa độ: ${String.format("%.4f", lat)}, ${String.format("%.4f", lon)}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { addressPreview = "Chưa rõ tên đường (Vẫn lưu được tọa độ)" }
            }
        }
    }

    // 👇 HÀM MỚI TÌM VỊ TRÍ THEO TÊN VÀ DI CHUYỂN MAP
    fun searchMap(query: String) {
        if (query.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập tên địa điểm.", Toast.LENGTH_SHORT).show()
            return
        }
        isSearching = true
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("vi", "VN"))
                val addresses = geocoder.getFromLocationName(query, 1)

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val newGeoPoint = GeoPoint(addr.latitude, addr.longitude)

                        mapViewRef?.controller?.apply {
                            animateTo(newGeoPoint)
                            setZoom(17.0)
                        }

                        // Cập nhật lại address preview cho vị trí mới
                        getAddress(addr.latitude, addr.longitude)

                        // Hiển thị toast tên địa chỉ tìm được
                        Toast.makeText(context, "Đã tìm thấy: ${addr.getAddressLine(0) ?: addr.featureName}", Toast.LENGTH_LONG).show()

                    } else {
                        Toast.makeText(context, "Không tìm thấy kết quả cho '$query'.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi tìm kiếm: Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSearching = false
                }
            }
        }
    }
    // 👆 HÀM MỚI TÌM VỊ TRÍ THEO TÊN VÀ DI CHUYỂN MAP


    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        getAddress(initialLat, initialLon)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 1. BẢN ĐỒ
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    val controller = this.controller
                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(initialLat, initialLon))

                    mapViewRef = this

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            addressPreview = "Thả tay để chọn..."
                            addressJob?.cancel()
                            val center = this@apply.mapCenter
                            getAddress(center.latitude, center.longitude)
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean = true
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. CÁI GHIM CỐ ĐỊNH Ở GIỮA
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .offset(y = (-24).dp)
        )

        // 3. THANH TÌM KIẾM (MỚI)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập tên đường, địa điểm, tỉnh...") },
                singleLine = true,
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { searchMap(searchText) }) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFFD32F2F))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        // 4. UI ĐIỀU KHIỂN (PHÍA DƯỚI)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Di chuyển hoặc tìm kiếm để ghim vị trí", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))

                // Hiển thị địa chỉ
                Text(
                    text = addressPreview,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            val center = mapViewRef?.mapCenter ?: GeoPoint(initialLat, initialLon)
                            onLocationSelected(center.latitude, center.longitude, addressPreview)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Xác nhận")
                    }
                }
            }
        }
    }
}