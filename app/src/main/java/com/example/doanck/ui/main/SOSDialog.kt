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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.doanck.data.datastore.AppDataStore
import com.example.doanck.data.model.PendingSOS
import com.example.doanck.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Icon cảnh báo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFFFEBEE), CircleShape), // Màu đỏ rất nhạt
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F), // Màu đỏ đậm
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 2. Tiêu đề
                Text(
                    text = "Gửi Tín Hiệu Cứu Trợ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Vị trí của bạn sẽ được gửi ngay lập tức tới cộng đồng và đội cứu hộ.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                // 3. Ô nhập số điện thoại
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 11) phone = it }, // Giới hạn độ dài
                    label = { Text("Số điện thoại liên hệ") },
                    placeholder = { Text("VD: 0987654321") },
                    leadingIcon = { Icon(Icons.Default.Call, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // 4. Ô nhập tình trạng
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Tình trạng khẩn cấp") },
                    placeholder = { Text("VD: Nước ngập sâu, có người bị thương...") },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(24.dp))

                // 5. Hàng nút bấm (Hủy - Gửi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nút Hủy
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        enabled = !isSending
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Bold)
                    }

                    // Nút Gửi (Màu đỏ nổi bật)
                    Button(
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser

                            // Nếu muốn test nhanh không cần login thì mở comment dòng dưới:
                            // val uid = user?.uid ?: "test_user"; val email = user?.email ?: "test@email.com"

                            if (user == null) {
                                Toast.makeText(context, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (phone.isBlank() || message.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSending = true

                            // Tạo data gửi đi
                            val sos = PendingSOS(
                                userId = user.uid,
                                email = user.email ?: "Ẩn danh",
                                phone = phone.trim(),
                                message = message.trim(),
                                lat = lat,
                                lon = lon
                            )

                            scope.launch {
                                if (networkMonitor.isOnline()) {
                                    // Có mạng -> Gửi ngay
                                    Firebase.firestore.collection("sos_requests")
                                        .add(sos)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "✅ Đã gửi tín hiệu thành công!", Toast.LENGTH_LONG).show()
                                            isSending = false
                                            onDismiss()
                                        }
                                        .addOnFailureListener {
                                            // Lỗi mạng bất ngờ -> Lưu offline
                                            scope.launch { appDataStore.addToQueue(sos) }
                                            Toast.makeText(context, "⚠️ Mạng yếu, đã lưu tin chờ gửi.", Toast.LENGTH_LONG).show()
                                            isSending = false
                                            onDismiss()
                                        }
                                } else {
                                    // Mất mạng -> Lưu offline
                                    appDataStore.addToQueue(sos)
                                    Toast.makeText(context, "📡 Mất kết nối! Đã lưu SOS, sẽ tự gửi khi có mạng.", Toast.LENGTH_LONG).show()
                                    isSending = false
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Đỏ đậm
                        enabled = !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("GỬI NGAY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}