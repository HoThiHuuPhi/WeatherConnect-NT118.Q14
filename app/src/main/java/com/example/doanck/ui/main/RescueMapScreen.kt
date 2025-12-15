package com.example.doanck.ui.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
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
import androidx.core.content.ContextCompat
import com.example.doanck.data.model.SOSRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescueMapScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var sosList by remember { mutableStateOf<List<SOSRequest>>(emptyList()) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // 1. Lắng nghe dữ liệu SOS từ Firebase theo thời gian thực
    DisposableEffect(Unit) {
        val query = Firebase.firestore.collection("sos_requests")
        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                sosList = snapshot.toObjects(SOSRequest::class.java)
            }
        }
        onDispose { listener.remove() }
    }

    // Cấu hình OSM
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // 2. Vẽ lại Marker khi danh sách SOS thay đổi
    LaunchedEffect(sosList) {
        mapView?.let { map ->
            // Giữ lại overlay vị trí của tôi (thường là cái đầu tiên hoặc tìm theo type)
            val myLocationOverlay = map.overlays.firstOrNull { it is MyLocationNewOverlay }

            map.overlays.clear() // Xóa hết marker cũ

            // Thêm lại overlay vị trí của tôi nếu có
            if (myLocationOverlay != null) {
                map.overlays.add(myLocationOverlay)
            }

            // Duyệt danh sách và cắm cờ
            sosList.forEach { sos ->
                val marker = Marker(map)
                marker.position = GeoPoint(sos.lat, sos.lon)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "SĐT: ${sos.phone}"
                marker.snippet = sos.message
                marker.subDescription = "Nhấn để gọi hoặc tìm đường"

                // Sự kiện khi bấm vào Marker
                marker.setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }

                // Thay đổi icon marker thành màu đỏ (Mặc định OSM là màu xanh hơi chán)
                // Nếu bạn có ảnh icon riêng thì dùng: marker.icon = ContextCompat.getDrawable(context, R.drawable.ic_sos_pin)
                // Ở đây mình dùng mặc định nhưng bạn có thể thêm icon 'ic_sos' màu đỏ vào drawable để đẹp hơn

                map.overlays.add(marker)
            }
            map.invalidate() // Vẽ lại bản đồ
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // --- BẢN ĐỒ ---
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                        val controller = this.controller
                        controller.setZoom(14.0) // Zoom xa hơn để nhìn tổng quan
                        controller.setCenter(GeoPoint(16.0471, 108.2068)) // Mặc định ở giữa VN hoặc vị trí bạn muốn

                        // Hiển thị vị trí của người dùng (Chấm xanh)
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        this.overlays.add(locationOverlay)

                        // Tự động zoom đến vị trí người dùng lần đầu
                        locationOverlay.runOnFirstFix {
                            controller.animateTo(locationOverlay.myLocation)
                        }

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- HEADER (Tiêu đề + Nút Back) ---
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                color = Color.White,
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    Text(
                        text = "Bản đồ cứu trợ (${sosList.size} ca)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- NÚT CHUYỂN VỀ DẠNG DANH SÁCH (Góc dưới phải) ---
            FloatingActionButton(
                onClick = onBack, // Quay lại màn hình danh sách
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Danh sách")
            }
        }
    }
}