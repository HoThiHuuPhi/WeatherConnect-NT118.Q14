package com.example.doanck.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.doanck.data.model.SOSRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescueMapScreen(
    onBack: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSOSDetail: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    var sosList by remember { mutableStateOf<List<SOSRequest>>(emptyList()) }

    var selectedSOS by remember { mutableStateOf<SOSRequest?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    val SkyBlueChat = Color(0xFF87CEFA)

    // Lắng nghe SOS realtime
    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("sos_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                sosList = snapshot?.toObjects(SOSRequest::class.java) ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    // Init osmdroid + MapView
    val mapView = remember {
        val cfg = Configuration.getInstance()
        cfg.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        cfg.userAgentValue = context.packageName

        val base = File(context.filesDir, "osmdroid").apply { mkdirs() }
        val tile = File(base, "tile").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = tile

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            // Tăng zoom lên 7.5 theo yêu cầu (nhìn rõ hơn)
            controller.setZoom(7.5)
            controller.setCenter(GeoPoint(16.0471, 108.2068))
        }
    }

    // Overlays
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }
    val sosOverlay = remember { FolderOverlay() }

    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(myLocationOverlay)) {
            mapView.overlays.add(myLocationOverlay)
        }
        if (!mapView.overlays.contains(sosOverlay)) {
            mapView.overlays.add(sosOverlay)
        }
    }

    // Cập nhật Markers
    LaunchedEffect(sosList) {
        sosOverlay.items.clear()
        sosList.forEach { sos ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(sos.lat, sos.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "SĐT: ${sos.phone}"
                snippet = sos.message
                subDescription = "Nhấn để xem chi tiết"
                setOnMarkerClickListener { m, _ ->
                    selectedSOS = sos
                    showInfoSheet = true
                    true
                }
            }
            sosOverlay.add(marker)
        }
        mapView.invalidate()
    }

    // Lifecycle
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Scaffold(containerColor = Color.White) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // BẢN ĐỒ
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // HEADER
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 0.dp, start = 16.dp, end = 16.dp)
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
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NÚT BACK
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // TITLE
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bản đồ cứu trợ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "${sosList.size} ca cần hỗ trợ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // NÚT REFRESH (Đặt lại góc nhìn)
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = {
                                mapView.controller.setZoom(7.5)
                                mapView.controller.animateTo(GeoPoint(16.0471, 108.2068))
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Đặt lại góc nhìn",
                                tint = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }

            // NÚT ZOOM IN/OUT
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { mapView.controller.zoomIn() }) {
                        Icon(
                            Icons.Default.Add,
                            "Phóng to",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF1565C0)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { mapView.controller.zoomOut() }) {
                        Icon(
                            Icons.Default.Remove,
                            "Thu nhỏ",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF1565C0)
                        )
                    }
                }
            }

            // CỘT CHỨA NÚT ĐỊNH VỊ VÀ DANH SÁCH (Góc dưới phải)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End, // Căn lề phải cho cột
                verticalArrangement = Arrangement.spacedBy(16.dp) // Khoảng cách giữa 2 cụm nút
            ) {
                // --- CỤM NÚT ĐỊNH VỊ (Nằm trên) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label Text: Vị trí của tôi
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable {
                            val p = myLocationOverlay.myLocation
                            if (p != null) {
                                mapView.post {
                                    mapView.controller.setZoom(15.0)
                                    mapView.controller.animateTo(p)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Vị trí của tôi",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Floating Button: Định vị
                    FloatingActionButton(
                        onClick = {
                            val p = myLocationOverlay.myLocation
                            if (p != null) {
                                mapView.post {
                                    mapView.controller.setZoom(15.0)
                                    mapView.controller.animateTo(p)
                                }
                            }
                        },
                        containerColor = Color.White,
                        contentColor = Color(0xFF1976D2),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Vị trí của tôi",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // --- CỤM NÚT DANH SÁCH (Nằm dưới) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label Text: Danh sách
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable { onOpenList() }
                    ) {
                        Text(
                            text = "Danh sách cứu hộ/ Cứu trợ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Floating Button: Danh sách
                    FloatingActionButton(
                        onClick = onOpenList,
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Xem danh sách",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // BOTTOM SHEET CHI TIẾT
            if (showInfoSheet && selectedSOS != null) {
                val s = selectedSOS!!
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White
                ) {
                    // Header gradient
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SkyBlueChat, Color(0xFFB0E0E6), Color(0xFFFFFACD))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            "📍 Thông tin SOS",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("SĐT: ${s.phone}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("Mô tả: ${s.message ?: "Không có thông tin"}", color = Color(0xFF424242))

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showInfoSheet = false
                                val safeName = "SĐT: ${s.phone}".replace("/", "-")
                                onOpenSOSDetail(s.lat, s.lon, safeName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Xem bản đồ cứu hộ", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}