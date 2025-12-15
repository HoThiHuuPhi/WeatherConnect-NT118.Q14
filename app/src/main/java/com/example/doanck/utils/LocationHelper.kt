package com.example.doanck.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

// ✅ Class này nằm ở đây là ĐÚNG
data class LocationData(val lat: Double, val lon: Double, val cityName: String)

// ✅ Object này nằm ở đây là ĐÚNG
object LocationHelper {
    private val CITIES_TO_PREFIX_TP = setOf("hồ chí minh", "hà nội", "đà nẵng", "hải phòng", "cần thơ", "thủ đức", "huế", "nha trang", "vũng tàu", "đà lạt", "buôn ma thuột", "pleiku", "quy nhơn", "phan thiết", "biên hòa", "thủ dầu một", "mỹ tho", "cà mau", "long xuyên", "rạch giá")

    @SuppressLint("MissingPermission")
    suspend fun fetchLocation(context: Context): Location {
        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine<Location> { continuation ->
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGps && !isNetwork) {
                    if (continuation.isActive) continuation.resume(getDefaultLocation())
                    return@suspendCancellableCoroutine
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) {
                            continuation.resume(location)
                            locationManager.removeUpdates(this)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    if (isNetwork) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, context.mainLooper)
                    if (isGps) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, context.mainLooper)

                    val lastKnownNet = if (isNetwork) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
                    val lastKnownGps = if (isGps) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null

                    if (lastKnownGps != null) {
                        locationManager.removeUpdates(listener)
                        if (continuation.isActive) continuation.resume(lastKnownGps)
                    } else if (lastKnownNet != null) {
                        locationManager.removeUpdates(listener)
                        if (continuation.isActive) continuation.resume(lastKnownNet)
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resume(getDefaultLocation())
                }
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            }
        } ?: getDefaultLocation()
    }

    private fun getDefaultLocation() = Location("default").apply { latitude = 10.8231; longitude = 106.6297 }

    suspend fun getCityName(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val subLocality = address.subLocality
                val locality = address.locality ?: address.subAdminArea
                val name = subLocality ?: locality ?: address.adminArea
                return@withContext name?.trim() ?: "Vị trí hiện tại"
            }
        } catch (e: Exception) { }
        return@withContext "Việt Nam"
    }

    suspend fun getProvinceFromCoordinates(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                return@withContext addresses[0].adminArea ?: addresses[0].locality ?: "Không xác định"
            }
        } catch (e: Exception) { }
        return@withContext "Không xác định"
    }
}