package com.example.doanck.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority // <--- Import Mới
import com.google.android.gms.tasks.CancellationTokenSource // <--- Import Mới
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class CommunityChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<CommunityMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLocationReady = MutableStateFlow(false)
    val isLocationReady = _isLocationReady.asStateFlow()

    private val _currentAddress = MutableStateFlow("Đang xác định vị trí...")
    val currentAddress = _currentAddress.asStateFlow()

    // Biến lưu Nickname
    private val _nickname = MutableStateFlow("")
    val nickname = _nickname.asStateFlow()

    private var currentUserLocation: Location? = null
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        signInAnonymously()
        listenToMessages()
    }

    fun setNickname(name: String) {
        _nickname.value = name
    }

    // --- SỬA LẠI HÀM NÀY: Dùng getCurrentLocation để ép lấy GPS ---
    @SuppressLint("MissingPermission")
    fun getUserLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Tạo token để hủy nếu tìm lâu quá (tránh treo app)
        val cancellationTokenSource = CancellationTokenSource()

        // Sử dụng PRIORITY_HIGH_ACCURACY để ép bật GPS chính xác nhất
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                // 1. Lưu vị trí tìm được
                currentUserLocation = location
                _isLocationReady.value = true

                // 2. Dịch tọa độ sang tên đường
                getAddressFromLocation(context, location.latitude, location.longitude)

                Log.d("GPS", "Tìm thấy: ${location.latitude}, ${location.longitude}")
            } else {
                // Trường hợp vẫn null (hiếm gặp nếu dùng cách này)
                _isLocationReady.value = true
                _currentAddress.value = "Vui lòng bật Google Maps để mồi GPS"
            }
        }.addOnFailureListener {
            _isLocationReady.value = true
            _currentAddress.value = "Lỗi GPS: ${it.message}"
        }
    }

    private fun getAddressFromLocation(context: Context, lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val district = address.subAdminArea ?: address.locality ?: ""
                    val province = address.adminArea ?: ""
                    _currentAddress.value = if (district.isNotEmpty()) "$district, $province" else province
                }
            } catch (e: Exception) { Log.e("Geo", "Lỗi: ${e.message}") }
        }
    }

    private fun signInAnonymously() {
        if (auth.currentUser == null) auth.signInAnonymously()
    }

    fun sendMessage(content: String, severity: String, isAnonymous: Boolean, context: Context) {
        val currentUserId = auth.currentUser?.uid ?: "Unknown"

        // Logic chọn tên hiển thị
        val finalName = if (isAnonymous) "Ẩn danh" else if (_nickname.value.isNotBlank()) _nickname.value else "User-${currentUserId.take(4)}"

        val lat = currentUserLocation?.latitude ?: 0.0
        val lng = currentUserLocation?.longitude ?: 0.0

        val messageData = hashMapOf(
            "userId" to finalName,
            "realUserId" to currentUserId,
            "message" to content,
            "severity" to severity,
            "anonymous" to isAnonymous,
            "timestamp" to System.currentTimeMillis(),
            "latitude" to lat,
            "longitude" to lng
        )

        db.collection("messages").add(messageData)
            .addOnSuccessListener {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "✅ Đã gửi!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "❌ Lỗi mạng", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun listenToMessages() {
        db.collection("messages").orderBy("timestamp").addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            if (snapshots != null) {
                val listMsg = ArrayList<CommunityMessage>()
                for (doc in snapshots) {
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lng = doc.getDouble("longitude") ?: 0.0
                    val msgLocation = Location("msg").apply { latitude = lat; longitude = lng }
                    var distance = 0f
                    if (currentUserLocation != null) distance = currentUserLocation!!.distanceTo(msgLocation)

                    if (distance <= 10000 || currentUserLocation == null) {
                        listMsg.add(CommunityMessage(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            message = doc.getString("message") ?: "",
                            severity = doc.getString("severity") ?: "info",
                            anonymous = doc.getBoolean("anonymous") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            latitude = lat,
                            longitude = lng,
                            realUserId = doc.getString("realUserId")
                        ))
                    }
                }
                _messages.value = listMsg
            }
        }
    }
}