package com.example.doanck.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions // Icon chỉ đường
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.bonuspack.routing.Road

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSMapScreen(
    lat: Double,
    lon: Double,
    name: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Dùng để chạy tác vụ tìm đường (nặng)

    // Các biến quản lý Map
    var mapController by remember { mutableStateOf<org.osmdroid.api.IMapController?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Biến lưu đường đi đã vẽ (để xóa nếu vẽ lại)
    var currentRoadOverlay by remember { mutableStateOf<Polyline?>(null) }
    var isRouting by remember { mutableStateOf(false) } // Hiển thị loading khi đang tìm đường

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {

            // ------------------------------------------------
            // 1. BẢN ĐỒ
            // ------------------------------------------------
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                        val controller = this.controller
                        controller.setZoom(18.0)
                        val targetPoint = GeoPoint(lat, lon)
                        controller.setCenter(targetPoint)

                        mapController = controller
                        mapView = this // Lưu tham chiếu mapView để vẽ đường sau này

                        // Vị trí của tôi
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        this.overlays.add(locationOverlay)
                        myLocationOverlay = locationOverlay

                        // La bàn & Thước đo
                        val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this)
                        compassOverlay.enableCompass()
                        this.overlays.add(compassOverlay)

                        val scaleBarOverlay = ScaleBarOverlay(this)
                        scaleBarOverlay.setCentred(true)
                        scaleBarOverlay.setScaleBarOffset(context.resources.displayMetrics.widthPixels / 2, 50)
                        this.overlays.add(scaleBarOverlay)

                        // Marker Đích (Người SOS)
                        val marker = Marker(this)
                        marker.position = targetPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = name
                        marker.snippet = "Cần hỗ trợ tại đây!"
                        marker.showInfoWindow()
                        this.overlays.add(marker)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ------------------------------------------------
            // 2. THANH UI TRÊN CÙNG
            // ------------------------------------------------
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)),
                    color = Color.White, shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Tọa độ: $lat, $lon", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                        }
                        // Loading khi đang tìm đường
                        if (isRouting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF4A90E2), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, null, tint = Color(0xFFEF5350))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            // ------------------------------------------------
            // 3. CÁC NÚT CHỨC NĂNG (BÊN PHẢI)
            // ------------------------------------------------
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Khoảng cách giữa các nút
            ) {

                // 🔥 NÚT 1: CHỈ ĐƯỜNG (MỚI)
                FloatingActionButton(
                    onClick = {
                        val myLoc = myLocationOverlay?.myLocation
                        if (myLoc == null) {
                            Toast.makeText(context, "Đang lấy vị trí của bạn...", Toast.LENGTH_SHORT).show()
                            return@FloatingActionButton
                        }

                        // Bắt đầu tìm đường
                        isRouting = true
                        scope.launch(Dispatchers.IO) { // Chạy luồng background
                            try {
                                val roadManager = OSRMRoadManager(context, "WeatherConnectUserAgent")
                                // Chế độ: Đi xe (MEAN_BY_CAR), Đi bộ (MEAN_BY_FOOT), Xe đạp (MEAN_BY_BIKE)
                                roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)

                                val waypoints = arrayListOf(myLoc, GeoPoint(lat, lon))
                                val road = roadManager.getRoad(waypoints)

                                if (road.mStatus != Road.STATUS_OK) {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Không tìm thấy đường!", Toast.LENGTH_SHORT).show() }
                                } else {
                                    val roadOverlay = RoadManager.buildRoadOverlay(road)
                                    roadOverlay.outlinePaint.color = android.graphics.Color.BLUE // Màu đường đi
                                    roadOverlay.outlinePaint.strokeWidth = 15f // Độ dày

                                    withContext(Dispatchers.Main) {
                                        // Xóa đường cũ nếu có
                                        if (currentRoadOverlay != null) mapView?.overlays?.remove(currentRoadOverlay)

                                        // Vẽ đường mới
                                        mapView?.overlays?.add(0, roadOverlay) // add(0) để vẽ dưới Marker
                                        currentRoadOverlay = roadOverlay

                                        mapView?.invalidate() // Refresh map

                                        // Zoom để thấy toàn bộ đường đi
                                        // mapView?.zoomToBoundingBox(road.mBoundingBox, true)
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show() }
                            } finally {
                                isRouting = false
                            }
                        }
                    },
                    containerColor = Color(0xFF4A90E2), // Màu xanh dương
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Directions, contentDescription = "Chỉ đường")
                }

                // NÚT 2: ZOOM VỀ MỤC TIÊU
                FloatingActionButton(
                    onClick = {
                        mapController?.animateTo(GeoPoint(lat, lon))
                        mapController?.setZoom(18.0)
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFFEF5350),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Mục tiêu")
                }

                // NÚT 3: VỊ TRÍ CỦA TÔI
                FloatingActionButton(
                    onClick = {
                        val myLoc = myLocationOverlay?.myLocation
                        if (myLoc != null) {
                            mapController?.animateTo(myLoc)
                            mapController?.setZoom(18.5)
                        }
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của tôi")
                }
            }
        }
    }
}