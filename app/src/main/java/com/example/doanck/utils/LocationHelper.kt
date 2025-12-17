package com.example.doanck.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(val lat: Double, val lon: Double, val cityName: String)

object LocationHelper {
    private const val TAG = "LocationHelper"
    private const val LOCATION_TIMEOUT = 4000L // Timeout 4 giây
    private const val LAST_KNOWN_MAX_AGE = 5 * 60 * 1000 // Vị trí cũ tối đa 5 phút

    @SuppressLint("MissingPermission")
    suspend fun fetchLocation(context: Context): Location {
        Log.d(TAG, "Bắt đầu lấy vị trí...")

        val result = withTimeoutOrNull(LOCATION_TIMEOUT) {
            suspendCancellableCoroutine<Location> { continuation ->
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                // Kiểm tra các nhà cung cấp vị trí có bật không
                val isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                Log.d(TAG, "GPS bật: $isGps, Mạng bật: $isNetwork")

                // Nếu không có nhà cung cấp nào → dùng vị trí mặc định
                if (!isGps && !isNetwork) {
                    Log.w(TAG, "Không có nhà cung cấp vị trí nào được bật")
                    if (continuation.isActive) {
                        continuation.resume(getDefaultLocation())
                    }
                    return@suspendCancellableCoroutine
                }

                try {
                    // Lấy vị trí đã biết gần nhất từ Network
                    val lastKnownNet = if (isNetwork) {
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } else null

                    // Lấy vị trí đã biết gần nhất từ GPS
                    val lastKnownGps = if (isGps) {
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } else null

                    // Chọn vị trí tốt nhất (ưu tiên Network vì nhanh hơn)
                    val bestLastKnown = when {
                        // Network mới (< 5 phút) → dùng ngay
                        lastKnownNet != null && isFreshLocation(lastKnownNet) -> {
                            Log.d(TAG, "Dùng vị trí Network mới (độ chính xác ${lastKnownNet.accuracy}m)")
                            lastKnownNet
                        }
                        // GPS mới (< 5 phút) → dùng
                        lastKnownGps != null && isFreshLocation(lastKnownGps) -> {
                            Log.d(TAG, "Dùng vị trí GPS mới (độ chính xác ${lastKnownGps.accuracy}m)")
                            lastKnownGps
                        }
                        // Network cũ → vẫn dùng được
                        lastKnownNet != null -> {
                            Log.d(TAG, "Dùng vị trí Network cũ")
                            lastKnownNet
                        }
                        // GPS cũ
                        lastKnownGps != null -> {
                            Log.d(TAG, "Dùng vị trí GPS cũ")
                            lastKnownGps
                        }
                        else -> null
                    }

                    // Nếu có vị trí cũ hợp lệ → trả về luôn
                    if (bestLastKnown != null) {
                        if (continuation.isActive) {
                            continuation.resume(bestLastKnown)
                        }
                        return@suspendCancellableCoroutine
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi lấy vị trí đã biết", e)
                }

                // Không có vị trí cũ → yêu cầu cập nhật vị trí mới
                Log.d(TAG, "Không có vị trí cũ hợp lệ, đang yêu cầu cập nhật...")

                // Listener để nhận vị trí mới
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.d(TAG, "Nhận được vị trí mới: ${location.latitude}, ${location.longitude} (độ chính xác ${location.accuracy}m, từ: ${location.provider})")
                        if (continuation.isActive) {
                            locationManager.removeUpdates(this)
                            continuation.resume(location)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                        Log.d(TAG, "Trạng thái nhà cung cấp đổi: $provider, trạng thái: $status")
                    }
                    override fun onProviderEnabled(provider: String) {
                        Log.d(TAG, "Nhà cung cấp bật: $provider")
                    }
                    override fun onProviderDisabled(provider: String) {
                        Log.d(TAG, "Nhà cung cấp tắt: $provider")
                    }
                }

                try {
                    var updatesRequested = false

                    // Yêu cầu cập nhật từ Network (ưu tiên vì nhanh)
                    if (isNetwork) {
                        try {
                            locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0L,
                                0f,
                                listener,
                                context.mainLooper
                            )
                            updatesRequested = true
                            Log.d(TAG, "Đã yêu cầu cập nhật vị trí từ Network")
                        } catch (e: Exception) {
                            Log.e(TAG, "Lỗi khi yêu cầu cập nhật Network", e)
                        }
                    }

                    // Yêu cầu cập nhật từ GPS (song song)
                    if (isGps) {
                        try {
                            locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                0L,
                                0f,
                                listener,
                                context.mainLooper
                            )
                            updatesRequested = true
                            Log.d(TAG, "Đã yêu cầu cập nhật vị trí từ GPS")
                        } catch (e: Exception) {
                            Log.e(TAG, "Lỗi khi yêu cầu cập nhật GPS", e)
                        }
                    }

                    // Nếu không yêu cầu được → dùng vị trí mặc định
                    if (!updatesRequested) {
                        Log.w(TAG, "Không thể yêu cầu cập nhật vị trí")
                        if (continuation.isActive) {
                            continuation.resume(getDefaultLocation())
                        }
                        return@suspendCancellableCoroutine
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi yêu cầu cập nhật vị trí", e)
                    if (continuation.isActive) {
                        continuation.resume(getDefaultLocation())
                    }
                }

                // Hủy yêu cầu khi bị cancel
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Yêu cầu vị trí bị hủy, đang xóa cập nhật")
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xóa cập nhật", e)
                    }
                }
            }
        }

        // Xử lý kết quả
        return if (result != null) {
            Log.d(TAG, "Lấy vị trí thành công: ${result.latitude}, ${result.longitude}")
            result
        } else {
            Log.w(TAG, "⏱Hết thời gian chờ, dùng vị trí mặc định")
            getDefaultLocation()
        }
    }

    /**
     * Kiểm tra vị trí có còn mới không (< 5 phút)
     */
    private fun isFreshLocation(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < LAST_KNOWN_MAX_AGE
    }

    /**
     * Trả về vị trí mặc định (Hồ Chí Minh)
     */
    private fun getDefaultLocation() = Location("default").apply {
        latitude = 10.8231
        longitude = 106.6297
        Log.d(TAG, "📍 Dùng vị trí mặc định (Thành phố Hồ Chí Minh)")
    }

    /**
     * Lấy tên thành phố từ tọa độ
     */
    suspend fun getCityName(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Đang lấy tên thành phố cho: $lat, $lon")
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val subLocality = address.subLocality
                val locality = address.locality ?: address.subAdminArea
                val name = subLocality ?: locality ?: address.adminArea
                val result = name?.trim() ?: "Vị trí hiện tại"
                Log.d(TAG, "Tên thành phố: $result")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy tên thành phố", e)
        }
        Log.d(TAG, "Không thể xác định tên thành phố, dùng mặc định")
        return@withContext "Việt Nam"
    }

    /**
     * Lấy tên tỉnh/thành từ tọa độ
     */
    suspend fun getProvinceFromCoordinates(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Đang lấy tên tỉnh cho: $lat, $lon")
        try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val result = addresses[0].adminArea ?: addresses[0].locality ?: "Không xác định"
                Log.d(TAG, "Tên tỉnh: $result")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy tên tỉnh", e)
        }
        Log.d(TAG, "Không thể xác định tên tỉnh")
        return@withContext "Không xác định"
    }
}