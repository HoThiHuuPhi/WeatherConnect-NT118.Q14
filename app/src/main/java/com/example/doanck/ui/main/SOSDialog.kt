package com.example.doanck.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
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
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.data.model.SOSRequest
import com.example.doanck.utils.LocationHelper
import com.example.doanck.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// ❌ TUYỆT ĐỐI KHÔNG DÁN CODE LocationHelper VÀO ĐÂY NỮA

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
                Text("Vị trí sẽ được gửi tới cộng đồng.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

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
                                val provinceName = LocationHelper.getProvinceFromCoordinates(context, lat, lon)

                                // 👇 DÙNG CÁCH NÀY ĐỂ TRÁNH LỖI LỘN THỨ TỰ (DOUBLE/STRING)
                                // Nếu nó báo đỏ dòng nào, bạn chỉ cần xóa dòng đó đi là biết ngay lệch tên biến
                                val sos = SOSRequest(
                                    userId = user.uid,
                                    email = user.email ?: "Ẩn danh",
                                    phone = phone.trim(),
                                    message = message.trim(),
                                    lat = lat,
                                    lon = lon,
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