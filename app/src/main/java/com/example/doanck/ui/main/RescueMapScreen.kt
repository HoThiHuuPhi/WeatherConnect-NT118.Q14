package com.example.doanck.ui.main

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val TAG = "SOSMapScreen"
private const val SAME_LOCATION_THRESHOLD_M = 30.0

private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val res = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res)
    return res[0].toDouble()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSMapScreen(
    lat: Double,
    lon: Double,
    name: String,
    onBack: () -> Unit,
    onOpenRescueMap: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mapController by remember { mutableStateOf<org.osmdroid.api.IMapController?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    var currentRoadOverlay by remember { mutableStateOf<Polyline?>(null) }
    var isRouting by remember { mutableStateOf(false) }

    var showInfoSheet by remember { mutableStateOf(false) }
    var infoTitle by remember { mutableStateOf(name) }
    var infoSnippet by remember { mutableStateOf("Cần hỗ trợ tại đây!") }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(containerColor = Color.White) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ===== MAP =====
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        val controller = this.controller
                        controller.setZoom(18.0)
                        val targetPoint = GeoPoint(lat, lon)
                        controller.setCenter(targetPoint)

                        mapController = controller
                        mapView = this

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        overlays.add(locationOverlay)
                        myLocationOverlay = locationOverlay

                        val marker = Marker(this).apply {
                            position = targetPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = name
                            snippet = "Cần hỗ trợ tại đây!"
                            setOnMarkerClickListener { m, _ ->
                                infoTitle = m.title ?: name
                                infoSnippet = m.snippet ?: ""
                                showInfoSheet = true
                                true
                            }
                        }
                        overlays.add(marker)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ===== HEADER =====
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            maxLines = 1
                        )
                        Text(
                            text = String.format("%.6f, %.6f", lat, lon),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242),
                            maxLines = 1
                        )
                    }

                    if (isRouting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1976D2),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocationSearching,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                }
            }

            // ===== ZOOM BUTTONS =====
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
                                colors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { mapView?.controller?.zoomIn() }) {
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
                                colors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { mapView?.controller?.zoomOut() }) {
                        Icon(
                            Icons.Default.Remove,
                            "Thu nhỏ",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF1565C0)
                        )
                    }
                }
            }

            // ===== ACTION BUTTONS (BOTTOM END) =====
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // ---- Google Maps Navigation ----
                ActionButtonWithLabel(
                    label = "Dẫn đường",
                    icon = Icons.Default.Navigation,
                    containerColor = Color(0xFF34A853),
                    contentColor = Color.White,
                    onClick = {
                        val target = GeoPoint(lat, lon)
                        val myLoc = myLocationOverlay?.myLocation

                        // ✅ nếu đang ở ngay điểm SOS -> không mở Google Maps
                        if (myLoc != null) {
                            val d = distanceMeters(myLoc, target)
                            if (d <= SAME_LOCATION_THRESHOLD_M) {
                                Toast.makeText(
                                    context,
                                    "Bạn đang ở ngay điểm SOS (~${d.toInt()}m)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                mapController?.animateTo(target)
                                mapController?.setZoom(18.5)
                                mapView?.invalidate()
                                return@ActionButtonWithLabel
                            }
                        }

                        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }

                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val browserUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon&travelmode=driving"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                        }
                    }
                )

                // ---- View route inside OSMDroid ----
                ActionButtonWithLabel(
                    label = "Xem đường",
                    icon = Icons.Default.Directions,
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White,
                    onClick = {
                        val myLoc = myLocationOverlay?.myLocation
                        if (myLoc == null) {
                            Toast.makeText(context, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show()
                            return@ActionButtonWithLabel
                        }

                        val target = GeoPoint(lat, lon)
                        val d = distanceMeters(myLoc, target)

                        // ✅ TRÙNG / RẤT GẦN: không routing, xoá route cũ, focus target
                        if (d <= SAME_LOCATION_THRESHOLD_M) {
                            Toast.makeText(
                                context,
                                "Bạn đang ở ngay điểm SOS (~${d.toInt()}m)",
                                Toast.LENGTH_SHORT
                            ).show()

                            currentRoadOverlay?.let { mapView?.overlays?.remove(it) }
                            currentRoadOverlay = null

                            mapController?.animateTo(target)
                            mapController?.setZoom(18.5)
                            mapView?.invalidate()
                            return@ActionButtonWithLabel
                        }

                        isRouting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val roadManager = OSRMRoadManager(context, context.packageName)
                                roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)

                                val waypoints = arrayListOf(myLoc, target)
                                val road = roadManager.getRoad(waypoints)

                                if (road.mStatus != Road.STATUS_OK) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Không tìm thấy đường!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val roadOverlay = RoadManager.buildRoadOverlay(road).apply {
                                        outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                                        outlinePaint.strokeWidth = 18f
                                    }

                                    withContext(Dispatchers.Main) {
                                        val dist = road.mLength

                                        Toast.makeText(
                                            context,
                                            "📍 ${String.format("%.1f", dist)} km",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        currentRoadOverlay?.let { mapView?.overlays?.remove(it) }
                                        mapView?.overlays?.add(0, roadOverlay)
                                        currentRoadOverlay = roadOverlay
                                        mapView?.invalidate()

                                        val bb = road.mBoundingBox
                                        val degenerate =
                                            (bb?.latNorth == bb?.latSouth) && (bb?.lonEast == bb?.lonWest)

                                        if (bb != null && !degenerate) {
                                            mapView?.zoomToBoundingBox(bb, true, 80)
                                        } else {
                                            mapController?.animateTo(target)
                                            mapController?.setZoom(18.0)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) { isRouting = false }
                            }
                        }
                    }
                )

                // ---- Focus SOS point ----
                ActionButtonWithLabel(
                    label = "Điểm SOS",
                    icon = Icons.Default.Place,
                    containerColor = Color(0xFFEF5350),
                    contentColor = Color.White,
                    onClick = {
                        mapController?.animateTo(GeoPoint(lat, lon))
                        mapController?.setZoom(18.0)
                    }
                )

                // ---- My location ----
                ActionButtonWithLabel(
                    label = "Vị trí của tôi",
                    icon = Icons.Default.MyLocation,
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    onClick = {
                        val myLoc = myLocationOverlay?.myLocation
                        if (myLoc != null) {
                            mapController?.animateTo(myLoc)
                            mapController?.setZoom(18.5)
                        } else {
                            Toast.makeText(context, "Chưa lấy được vị trí", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // ===== BOTTOM SHEET INFO =====
            if (showInfoSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = infoTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = String.format("%.6f, %.6f", lat, lon),
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(infoSnippet, color = Color(0xFF424242))

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showInfoSheet = false
                                onOpenRescueMap()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Xem bản đồ cứu trợ", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val target = GeoPoint(lat, lon)
                                val myLoc = myLocationOverlay?.myLocation

                                if (myLoc != null) {
                                    val d = distanceMeters(myLoc, target)
                                    if (d <= SAME_LOCATION_THRESHOLD_M) {
                                        Toast.makeText(
                                            context,
                                            "Bạn đang ở ngay điểm SOS (~${d.toInt()}m)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        mapController?.animateTo(target)
                                        mapController?.setZoom(18.5)
                                        mapView?.invalidate()
                                        return@OutlinedButton
                                    }
                                }

                                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val browserUri = Uri.parse(
                                        "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon&travelmode=driving"
                                    )
                                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dẫn đường")
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonWithLabel(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
        ) {
            Surface(
                color = Color(0xFF212121).copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
                hoveredElevation = 10.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
