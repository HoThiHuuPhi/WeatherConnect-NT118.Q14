package com.example.doanck.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.doanck.data.model.SOSRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vn.vietmap.vietmapsdk.Vietmap
import vn.vietmap.vietmapsdk.annotations.MarkerOptions
import vn.vietmap.vietmapsdk.camera.CameraPosition
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory
import vn.vietmap.vietmapsdk.geometry.LatLng
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions
import vn.vietmap.vietmapsdk.location.engine.LocationEngineDefault
import vn.vietmap.vietmapsdk.location.modes.CameraMode
import vn.vietmap.vietmapsdk.location.modes.RenderMode
import vn.vietmap.vietmapsdk.maps.MapView
import vn.vietmap.vietmapsdk.maps.Style
import vn.vietmap.vietmapsdk.maps.VietMapGL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSOverviewMapScreen(
    onBack: () -> Unit,
    onOpenList: () -> Unit,
    onOpenRescueMap: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember { Vietmap.getInstance(context) }

    var sosList by remember { mutableStateOf<List<SOSRequest>>(emptyList()) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var vietMapGL by remember { mutableStateOf<VietMapGL?>(null) }

    var selectedSOS by remember { mutableStateOf<SOSRequest?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Quản lý quyền vị trí
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasLocationPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 2. Lắng nghe SOS realtime từ Firebase
    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("sos_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                sosList = snapshot?.toObjects(SOSRequest::class.java) ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    // 3. Cập nhật markers lên bản đồ
    LaunchedEffect(sosList, vietMapGL) {
        vietMapGL?.let { map ->
            map.clear() // Xóa marker cũ
            sosList.forEach { sos ->
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(sos.lat, sos.lon))
                        .title(if (sos.phone.isNotEmpty()) sos.phone else "SOS")
                        .snippet(sos.message ?: "Cần hỗ trợ!")
                )
            }
        }
    }

    // 4. Lifecycle observer cho MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(containerColor = Color.White) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // --- BẢN ĐỒ VIETMAP ---
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        getMapAsync { map ->
                            vietMapGL = map

                            map.setStyle(
                                Style.Builder()
                                    .fromUri("https://maps.vietmap.vn/api/maps/light/styles.json?apikey=1b2a23b390ba308ebdc68c0e33e0090d339f9fcd3a4cfb42")
                            ) { style ->
                                val initialPosition = CameraPosition.Builder()
                                    .target(LatLng(16.4637, 107.5908))
                                    .zoom(4.0)
                                    .build()
                                map.cameraPosition = initialPosition

                                if (hasLocationPermission) {
                                    try {
                                        val locationComponent = map.locationComponent
                                        locationComponent.activateLocationComponent(
                                            LocationComponentActivationOptions.builder(ctx, style).build()
                                        )
                                        locationComponent.isLocationComponentEnabled = true
                                        locationComponent.cameraMode = CameraMode.NONE
                                        locationComponent.renderMode = RenderMode.COMPASS
                                        locationComponent.locationEngine = LocationEngineDefault.getDefaultLocationEngine(ctx)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            // Xử lý sự kiện click marker
                            map.setOnMarkerClickListener { marker ->
                                val clickedSOS = sosList.find { sos ->
                                    val latDiff = kotlin.math.abs(sos.lat - marker.position.latitude)
                                    val lonDiff = kotlin.math.abs(sos.lon - marker.position.longitude)
                                    latDiff < 0.0001 && lonDiff < 0.0001
                                }

                                if (clickedSOS != null) {
                                    selectedSOS = clickedSOS
                                    showInfoSheet = true

                                    vietMapGL?.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.Builder()
                                                .target(LatLng(clickedSOS.lat, clickedSOS.lon))
                                                .zoom(9.0)
                                                .build()
                                        )
                                    )
                                } else {
                                    Toast.makeText(ctx, marker.title, Toast.LENGTH_SHORT).show()
                                }
                                true
                            }
                        }
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- HEADER ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
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
                    // Nút Back
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1976D2))
                        }
                    }

                    Spacer(Modifier.width(12.dp))

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

                    // Nút Refresh
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = {
                            vietMapGL?.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(16.4637, 107.5908))
                                        .zoom(4.0)
                                        .build()
                                )
                            )
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF1976D2))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zoom In
                SmallFloatingActionButton(
                    onClick = { vietMapGL?.animateCamera(CameraUpdateFactory.zoomIn()) },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { vietMapGL?.animateCamera(CameraUpdateFactory.zoomOut()) },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }

            // --- NÚT DƯỚI CÙNG (Vị trí tôi & Danh sách) ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        vietMapGL?.locationComponent?.lastKnownLocation?.let { location ->
                            val position = CameraPosition.Builder()
                                .target(LatLng(location.latitude, location.longitude))
                                .zoom(15.0)
                                .build()
                            vietMapGL?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                        } ?: Toast.makeText(context, "Chưa lấy được vị trí", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2),
                    icon = {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                    },
                    text = {
                        Text("Vị trí của tôi")
                    }
                )

                // Nút Danh sách SOS
                ExtendedFloatingActionButton(
                    onClick = onOpenList,
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    text = { Text("Danh sách") }
                )
            }
        }

        // --- BOTTOM SHEET CHI TIẾT ---
        if (showInfoSheet && selectedSOS != null) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Thông tin cứu trợ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SĐT:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
                        Text(selectedSOS!!.phone, color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Text("Mô tả:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
                        Text(
                            text = selectedSOS!!.message ?: "Không có nội dung",
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showInfoSheet = false
                            onOpenRescueMap(selectedSOS!!.lat, selectedSOS!!.lon, selectedSOS!!.phone)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mở bản đồ cứu hộ")
                    }
                }
            }
        }
    }
}
