package com.example.doanck.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

class CommunityChatViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _messages = MutableStateFlow<List<CommunityMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLocationReady = MutableStateFlow(false)
    val isLocationReady = _isLocationReady.asStateFlow()

    private val _currentAddress = MutableStateFlow("Đang xác định...")
    val currentAddress = _currentAddress.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname = _nickname.asStateFlow()

    private var allRawMessages = listOf<CommunityMessage>()
    private var currentUserLocation: Location? = null

    init {
        signInAnonymous()
        listenToMessages()
    }

    fun setNickname(name: String) {
        _nickname.value = name
    }

    private fun signInAnonymous() {
        if (auth.currentUser == null) auth.signInAnonymously()
    }

    // ---------------------------------------------------------
    // Lấy vị trí GPS
    // ---------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun getUserLocation(context: Context) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    currentUserLocation = loc
                    _isLocationReady.value = true
                    getAddress(context, loc.latitude, loc.longitude)
                    filterMessages()
                } else {
                    _isLocationReady.value = true
                    _currentAddress.value = "Không lấy được GPS"
                }
            }
            .addOnFailureListener {
                _isLocationReady.value = true
                _currentAddress.value = "Lỗi GPS"
            }
    }

    // ---------------------------------------------------------
    // Chuyển GPS → tên quận/huyện
    // ---------------------------------------------------------
    private fun getAddress(context: Context, lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val geo = Geocoder(context, Locale.getDefault())
                val list = geo.getFromLocation(lat, lng, 1)
                if (!list.isNullOrEmpty()) {
                    val a = list[0]
                    val district = a.subAdminArea ?: a.locality ?: ""
                    val province = a.adminArea ?: ""
                    _currentAddress.value = "$district, $province"
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Lắng nghe Firestore realtime
    // ---------------------------------------------------------
    private fun listenToMessages() {
        db.collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, e ->

                if (e != null || snap == null) return@addSnapshotListener

                val temp = snap.map { doc ->
                    CommunityMessage(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        message = doc.getString("message") ?: "",
                        severity = doc.getString("severity") ?: "info",
                        anonymous = doc.getBoolean("anonymous") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        realUserId = doc.getString("realUserId"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        imageUrl = doc.getString("imageUrl") // BASE64
                    )
                }

                allRawMessages = temp
                filterMessages()
            }
    }

    // ---------------------------------------------------------
    // Lọc tin nhắn trong bán kính 5km
    // ---------------------------------------------------------
    private fun filterMessages() {
        val user = currentUserLocation ?: return
        val result = allRawMessages.filter { msg ->
            if (msg.latitude == null || msg.longitude == null) false
            else {
                val loc = Location("msg").apply {
                    latitude = msg.latitude
                    longitude = msg.longitude
                }
                user.distanceTo(loc) <= 5000
            }
        }
        _messages.value = result
    }

    // ---------------------------------------------------------
    // Gửi text + gửi ảnh
    // ---------------------------------------------------------

    fun sendMessage(text: String, severity: String, anonymous: Boolean, context: Context) {
        sendMessageInternal(text, severity, anonymous, context, null)
    }

    fun sendImageMessage(uri: Uri, caption: String, severity: String, anonymous: Boolean, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            val base64 = uriToBase64(context, uri)

            withContext(Dispatchers.Main) {
                if (base64 != null) {
                    sendMessageInternal(caption, severity, anonymous, context, base64)
                } else {
                    Toast.makeText(context, "Ảnh lỗi hoặc quá lớn!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessageInternal(text: String, severity: String, anonymous: Boolean, context: Context, base64Img: String?) {

        val uid = auth.currentUser?.uid ?: "unknown"
        val name = when {
            anonymous -> "Ẩn danh"
            _nickname.value.isNotBlank() -> _nickname.value
            else -> "User-${uid.take(4)}"
        }

        val lat = currentUserLocation?.latitude ?: 0.0
        val lng = currentUserLocation?.longitude ?: 0.0

        val data = hashMapOf(
            "userId" to name,
            "realUserId" to uid,
            "message" to text,
            "severity" to severity,
            "anonymous" to anonymous,
            "timestamp" to System.currentTimeMillis(),
            "latitude" to lat,
            "longitude" to lng,
            "imageUrl" to base64Img
        )

        db.collection("messages").add(data)
            .addOnSuccessListener { Toast.makeText(context, "Đã gửi!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Lỗi mạng!", Toast.LENGTH_SHORT).show() }
    }

    // ---------------------------------------------------------
    // Nén ảnh → BASE64
    // ---------------------------------------------------------
    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)

            val resized = Bitmap.createScaledBitmap(bmp, 600, (bmp.height * 600f / bmp.width).toInt(), true)

            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, out)

            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
