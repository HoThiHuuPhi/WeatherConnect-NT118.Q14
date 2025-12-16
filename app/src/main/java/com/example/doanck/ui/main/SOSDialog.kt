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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var isReportForOthers by remember { mutableStateOf(false) }

    // Biến lưu vị trí đã chọn
    var selectedLat by remember { mutableDoubleStateOf(lat) }
    var selectedLon by remember { mutableDoubleStateOf(lon) }
    var addressInput by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

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
            // 👇 Dùng DialogProperties để Map chiếm toàn màn hình
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

                    OutlinedTextField(
                        value = phone, onValueChange = { if (it.length <= 11) phone = it },
                        label = { Text("SĐT Liên hệ") }, leadingIcon = { Icon(Icons.Default.Call, null) },
                        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message, onValueChange = { message = it },
                        label = { Text("Tình trạng") }, leadingIcon = { Icon(Icons.Default.Description, null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Hủy") }
                        Button(
                            onClick = {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user == null || phone.isBlank() || message.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "Đã gửi!", Toast.LENGTH_SHORT).show(); onDismiss()
                                            }
                                            .addOnFailureListener {
                                                scope.launch { appDataStore.addToQueue(sos) }
                                                Toast.makeText(context, "Lỗi mạng, đã lưu offline", Toast.LENGTH_SHORT).show(); onDismiss()
                                            }
                                    } else {
                                        appDataStore.addToQueue(sos)
                                        Toast.makeText(context, "Đã lưu offline", Toast.LENGTH_SHORT).show(); onDismiss()
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