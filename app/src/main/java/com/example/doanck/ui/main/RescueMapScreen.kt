package com.example.doanck.ui.main

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
import androidx.preference.PreferenceManager
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
fun RescueMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var sosList by remember { mutableStateOf<List<SOSRequest>>(emptyList()) }

    // 1) Listen SOS realtime
    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("sos_requests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                sosList = snapshot?.toObjects(SOSRequest::class.java) ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    // 2) Init osmdroid + MapView (IMPORTANT: done BEFORE creating MapView)
    val mapView = remember {
        val cfg = Configuration.getInstance()
        cfg.load(context, PreferenceManager.getDefaultSharedPreferences(context))
        cfg.userAgentValue = context.packageName

        // (optional nhưng rất nên) đưa cache về thư mục app để tránh lỗi trên vài máy
        val base = File(context.filesDir, "osmdroid").apply { mkdirs() }
        val tile = File(base, "tile").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = tile

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(16.0471, 108.2068)) // VN
        }
    }

    // 3) Overlays: giữ cố định 2 overlay (my location + folder SOS)
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }
    val sosOverlay = remember { FolderOverlay() }

    // add overlays 1 lần
    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(myLocationOverlay)) mapView.overlays.add(myLocationOverlay)
        if (!mapView.overlays.contains(sosOverlay)) mapView.overlays.add(sosOverlay)

        // runOnFirstFix chạy background thread -> phải post về UI thread
        myLocationOverlay.runOnFirstFix {
            val p = myLocationOverlay.myLocation
            if (p != null) {
                mapView.post {
                    mapView.controller.setZoom(14.0)
                    mapView.controller.animateTo(p)
                }
            }
        }
    }

    // 4) Update SOS markers (chỉ clear folder sosOverlay, KHÔNG clear toàn bộ map.overlays)
    LaunchedEffect(sosList) {
        sosOverlay.items.clear()

        sosList.forEach { sos ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(sos.lat, sos.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "SĐT: ${sos.phone}"
                snippet = sos.message
                subDescription = "Nhấn để xem"

                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }
            }
            sosOverlay.add(marker)
        }

        mapView.invalidate()
    }

    // 5) MapView lifecycle (tránh leak/crash)
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
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Header
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                color = Color.White,
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
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

                    IconButton(onClick = {
                        val p = myLocationOverlay.myLocation
                        if (p != null) {
                            mapView.post {
                                mapView.controller.setZoom(14.0)
                                mapView.controller.animateTo(p)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Về vị trí tôi")
                    }
                }
            }

            // FAB quay lại danh sách
            FloatingActionButton(
                onClick = onBack,
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
