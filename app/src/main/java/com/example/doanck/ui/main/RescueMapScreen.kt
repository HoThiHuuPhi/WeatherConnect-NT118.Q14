package com.example.doanck.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.android.gms.location.LocationServices
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
fun RescueMapScreen(
    lat: Double,
    lon: Double,
    name: String,
    onBack: () -> Unit,
    onOpenOverview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val apiKey = "1b2a23b390ba308ebdc68c0e33e0090d339f9fcd3a4cfb42"
    val styleUrl = "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=$apiKey"

    remember { Vietmap.getInstance(context) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var vietMapGL by remember { mutableStateOf<VietMapGL?>(null) }
    var myLocation by remember { mutableStateOf<Location?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasLocationPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Lấy lastLocation để nút "Vị trí của tôi" đỡ bị null
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) myLocation = loc
            }
        }
    }

    // Lifecycle MapView
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun moveToMyLocation() {
        val loc = myLocation ?: vietMapGL?.locationComponent?.lastKnownLocation
        if (loc == null) {
            Toast.makeText(context, "Đang lấy vị trí của bạn...", Toast.LENGTH_SHORT).show()
            return
        }
        vietMapGL?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(loc.latitude, loc.longitude))
                    .zoom(16.0)
                    .build()
            )
        )
    }

    fun moveToSOS() {
        vietMapGL?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(lat, lon))
                    .zoom(17.0)
                    .build()
            )
        )
    }

    Scaffold(containerColor = Color.White) { padding ->
        Box(Modifier.fillMaxSize()) {

            // MAP
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        getMapAsync { map ->
                            vietMapGL = map
                            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                                if (hasLocationPermission) {
                                    try {
                                        val lc = map.locationComponent
                                        lc.activateLocationComponent(
                                            LocationComponentActivationOptions.builder(ctx, style).build()
                                        )
                                        lc.isLocationComponentEnabled = true
                                        lc.cameraMode = CameraMode.NONE
                                        lc.renderMode = RenderMode.GPS
                                        lc.locationEngine = LocationEngineDefault.getDefaultLocationEngine(ctx)
                                        lc.lastKnownLocation?.let { myLocation = it }
                                    } catch (_: Exception) {}
                                }

                                // SOS Marker
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(lat, lon))
                                        .title(name)
                                        .snippet("Cần hỗ trợ khẩn cấp!")
                                )

                                moveToSOS()
                            }
                        }
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // HEADER
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFB0E0E6), Color(0xFF87CEEB), Color(0xFFFFFACD))
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF1976D2))
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
                            text = String.format("%.5f, %.5f", lat, lon),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242),
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onOpenOverview) {
                        Icon(Icons.Default.Map, null, tint = Color(0xFF1976D2))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 24.dp)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openGoogleMapsNavigation(context, lat, lon) },
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Directions, contentDescription = null) },
                    text = { Text("Dẫn đường") },
                    expanded = true
                )

                ExtendedFloatingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { moveToMyLocation() },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2),
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text = { Text("Vị trí của tôi") },
                    expanded = true
                )

                ExtendedFloatingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { moveToSOS() },
                    containerColor = Color.White,
                    contentColor = Color(0xFFD32F2F),
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    text = { Text("Vị trí SOS") },
                    expanded = true
                )
            }
        }
    }
}

private fun openGoogleMapsNavigation(context: Context, lat: Double, lon: Double) {
    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        // fallback nếu máy không có Google Maps
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    }
}
