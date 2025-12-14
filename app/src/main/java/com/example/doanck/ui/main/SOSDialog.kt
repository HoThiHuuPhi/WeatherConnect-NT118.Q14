package com.example.doanck.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "GỬI TÍN HIỆU CỨU TRỢ 🆘",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Tình trạng (ngập lụt, bị thương...)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            Toast.makeText(context, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (phone.isBlank() || message.isBlank()) {
                            Toast.makeText(context, "Nhập SĐT và nội dung", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSending = true

                        val sos = PendingSOS(
                            uid = user.uid,
                            email = user.email ?: "unknown",
                            phone = phone.trim(),
                            message = message.trim(),
                            lat = lat,
                            lon = lon
                        )

                        scope.launch {
                            if (networkMonitor.isOnline()) {
                                Firebase.firestore.collection("sos_requests")
                                    .add(sos)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✅ Đã gửi SOS!", Toast.LENGTH_LONG).show()
                                        isSending = false
                                        onDismiss()
                                    }
                                    .addOnFailureListener {
                                        scope.launch {
                                            appDataStore.addToQueue(sos)
                                            Toast.makeText(context, "⚠️ Gửi lỗi, đã lưu SOS chờ gửi.", Toast.LENGTH_LONG).show()
                                            isSending = false
                                            onDismiss()
                                        }
                                    }
                            } else {
                                appDataStore.addToQueue(sos)
                                Toast.makeText(context, "📡 Mất mạng! SOS đã lưu, sẽ tự gửi khi có mạng.", Toast.LENGTH_LONG).show()
                                isSending = false
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending
                ) {
                    Text(if (isSending) "ĐANG XỬ LÝ..." else "GỬI NGAY")
                }
            }
        }
    }
}
