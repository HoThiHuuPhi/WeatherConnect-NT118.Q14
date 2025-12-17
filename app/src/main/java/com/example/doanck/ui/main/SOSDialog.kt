package com.example.doanck.ui.main

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.data.model.SOSRequest
import com.example.doanck.utils.LocationHelper
import com.example.doanck.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SOSDialog(
    appDataStore: AppDataStore,
    networkMonitor: NetworkMonitor,
    lat: Double,
    lon: Double,
    onDismiss: () -> Unit,
    onNavigateToRescueList: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // --- CÁC BIẾN LƯU LỖI (VALIDATION) ---
    var phoneError by remember { mutableStateOf<String?>(null) }
    var messageError by remember { mutableStateOf<String?>(null) }

    var isReportForOthers by remember { mutableStateOf(false) }

    var selectedLat by remember { mutableDoubleStateOf(lat) }
    var selectedLon by remember { mutableDoubleStateOf(lon) }
    var addressInput by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    // THÊM STATE ĐỂ HIỂN THỊ DIALOG THÀNH CÔNG
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Hàm kiểm tra hợp lệ
    fun validateInputs(): Boolean {
        var isValid = true

        // 1. Kiểm tra SĐT: Phải 10 số, bắt đầu bằng 0, chỉ chứa số
        if (phone.isBlank()) {
            phoneError = "Vui lòng nhập số điện thoại"
            isValid = false
        } else if (!phone.matches(Regex("^0\\d{9}$"))) {
            phoneError = "SĐT không hợp lệ (Phải có 10 số, bắt đầu bằng 0)"
            isValid = false
        } else {
            phoneError = null
        }

        // 2. Kiểm tra Tin nhắn: Không được để trống
        if (message.isBlank()) {
            messageError = "Vui lòng nhập tình trạng khẩn cấp"
            isValid = false
        } else {
            messageError = null
        }

        return isValid
    }

    LaunchedEffect(isReportForOthers) {
        if (!isReportForOthers) {
            selectedLat = lat
            selectedLon = lon
            addressInput = ""
        }
    }

    if (showMapPicker) {
        Dialog(
            onDismissRequest = { showMapPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LocationPicker(
                initialLat = if (selectedLat != 0.0) selectedLat else lat,
                initialLon = if (selectedLon != 0.0) selectedLon else lon,
                onLocationSelected = { newLat, newLon, newAddr ->
                    selectedLat = newLat
                    selectedLon = newLon
                    addressInput = newAddr
                    showMapPicker = false
                },
                onDismiss = { showMapPicker = false }
            )
        }
    } else if (showSuccessDialog) {
        // DIALOG THÀNH CÔNG
        SuccessDialog(
            onViewRescueList = {
                showSuccessDialog = false
                onDismiss()
                onNavigateToRescueList()
            },
            onDismiss = {
                showSuccessDialog = false
                onDismiss()
            }
        )
    } else {
        Dialog(onDismissRequest = { if (!isSending) onDismiss() }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(72.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Gửi Tín Hiệu Cứu Trợ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isReportForOthers,
                            onCheckedChange = { isReportForOthers = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD32F2F))
                        )
                        Text("Báo hộ người ở xa / Định vị lại", fontSize = 14.sp)
                    }

                    AnimatedVisibility(visible = isReportForOthers) {
                        Column {
                            OutlinedTextField(
                                value = addressInput, onValueChange = { addressInput = it },
                                label = { Text("Địa chỉ") },
                                placeholder = { Text("Nhập tay hoặc chọn map...") },
                                leadingIcon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                            TextButton(
                                onClick = { showMapPicker = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.EditLocation, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ghim vị trí trên bản đồ", color = Color(0xFF1976D2))
                            }
                        }
                    }

                    // --- TRƯỜNG NHẬP SỐ ĐIỆN THOẠI (CÓ RÀNG BUỘC) ---
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            // Chỉ cho phép nhập số và tối đa 10 ký tự
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                phone = it
                                if (phoneError != null) phoneError = null // Xóa lỗi khi người dùng gõ lại
                            }
                        },
                        label = { Text("SĐT Liên hệ") },
                        leadingIcon = { Icon(Icons.Default.Call, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = {
                            if (phoneError != null) {
                                Text(text = phoneError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // --- TRƯỜNG NHẬP TIN NHẮN (CÓ RÀNG BUỘC) ---
                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            if (messageError != null) messageError = null
                        },
                        label = { Text("Tình trạng") },
                        leadingIcon = { Icon(Icons.Default.Description, null) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = messageError != null,
                        supportingText = {
                            if (messageError != null) {
                                Text(text = messageError!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Hủy") }
                        Button(
                            onClick = {
                                // 1. Gọi hàm kiểm tra Validation trước
                                if (!validateInputs()) {
                                    return@Button
                                }

                                val user = FirebaseAuth.getInstance().currentUser
                                if (user == null) {
                                    Toast.makeText(context, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSending = true
                                scope.launch {
                                    var finalLat = lat
                                    var finalLon = lon
                                    var provinceName = "Đang cập nhật"

                                    if (isReportForOthers) {
                                        if (selectedLat != lat || selectedLon != lon) {
                                            finalLat = selectedLat
                                            finalLon = selectedLon
                                            provinceName = if (addressInput.isNotBlank()) addressInput else LocationHelper.getProvinceFromCoordinates(context, finalLat, finalLon)
                                        }
                                        else if (addressInput.isNotBlank()) {
                                            val geoResults = withContext(Dispatchers.IO) {
                                                try {
                                                    Geocoder(context, Locale("vi", "VN")).getFromLocationName(addressInput, 1)
                                                } catch (e: Exception) { null }
                                            }
                                            if (!geoResults.isNullOrEmpty()) {
                                                finalLat = geoResults[0].latitude
                                                finalLon = geoResults[0].longitude
                                                provinceName = geoResults[0].adminArea ?: addressInput
                                            } else {
                                                provinceName = addressInput
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Hãy nhập địa chỉ hoặc chọn trên bản đồ!", Toast.LENGTH_SHORT).show()
                                                isSending = false
                                            }
                                            return@launch
                                        }
                                    } else {
                                        provinceName = LocationHelper.getProvinceFromCoordinates(context, lat, lon)
                                    }

                                    val sos = SOSRequest(
                                        userId = user.uid,
                                        email = user.email ?: "Ẩn danh",
                                        phone = phone.trim(),
                                        message = message.trim(),
                                        lat = finalLat,
                                        lon = finalLon,
                                        province = provinceName
                                    )

                                    if (networkMonitor.isOnline()) {
                                        Firebase.firestore.collection("sos_requests").add(sos)
                                            .addOnSuccessListener {
                                                isSending = false
                                                showSuccessDialog = true // HIỂN THỊ DIALOG THÀNH CÔNG
                                            }
                                            .addOnFailureListener {
                                                scope.launch {
                                                    appDataStore.addToQueue(sos)
                                                    isSending = false
                                                    Toast.makeText(context, "Lỗi mạng, đã lưu offline", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                }
                                            }
                                    } else {
                                        appDataStore.addToQueue(sos)
                                        isSending = false
                                        Toast.makeText(context, "Đã lưu offline", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            enabled = !isSending
                        ) {
                            if (isSending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("GỬI NGAY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessDialog(
    onViewRescueList: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon thành công
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Gửi Thành Công!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Tín hiệu cứu trợ của bạn đã được gửi đi.\nXin hãy giữ bình tĩnh.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(12.dp))

                // Nút "Xem danh sách cứu hộ"
                Button(
                    onClick = onViewRescueList,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Xem Danh Sách Cứu Hộ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Nút "Đóng"
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Đóng",
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}