package com.example.doanck.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map // ✅ Import icon Bản đồ
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.data.model.SOSRequest
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. HÀM XÓA DẤU TIẾNG VIỆT
fun removeAccents(str: String): String {
    try {
        val temp = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").lowercase().replace("đ", "d")
    } catch (e: Exception) {
        return str.lowercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSMonitorScreen(
    onBack: () -> Unit,
    onNavigateToMap: (Double, Double, String) -> Unit,
    onOpenMapOverview: () -> Unit // 🟢 2. Thêm tham số này để mở bản đồ tổng quan
) {
    var sosList by remember { mutableStateOf<List<SOSRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Lắng nghe dữ liệu Realtime
    DisposableEffect(Unit) {
        val query = Firebase.firestore.collection("sos_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                sosList = snapshot.toObjects(SOSRequest::class.java)
                isLoading = false
            }
        }
        onDispose { listener.remove() }
    }

    // Logic Lọc danh sách (Tìm kiếm không dấu)
    val filteredList = sosList.filter { sos ->
        if (searchQuery.isBlank()) return@filter true

        val query = removeAccents(searchQuery.trim())
        val provinceNorm = removeAccents(sos.province ?: "")
        val messageNorm = removeAccents(sos.message)
        val phoneRaw = sos.phone

        provinceNorm.contains(query) || messageNorm.contains(query) || phoneRaw.contains(query)
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Danh sách Cứu Trợ", fontWeight = FontWeight.Bold, color = Color.Red) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Thanh tìm kiếm
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Nhập tỉnh, SĐT, nội dung...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },

                    // 🟢 3. THÊM NÚT BẢN ĐỒ VÀO THANH TÌM KIẾM
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Nút mở Bản đồ tổng quan
                            IconButton(onClick = onOpenMapOverview) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Bản đồ tổng quan",
                                    tint = Color(0xFF1976D2) // Màu xanh dương
                                )
                            }

                            // Nút Xóa tìm kiếm (chỉ hiện khi có chữ)
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Red,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = Color(0xFFF2F4F8)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredList.isEmpty()) {
            if (searchQuery.isNotEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy kết quả cho '$searchQuery'", color = Color.Gray)
                }
            } else {
                EmptyState(padding)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(filteredList) { sos ->
                    SOSCardItemNew(sos = sos, onNavigateToMap = onNavigateToMap)
                }
            }
        }
    }
}

@Composable
fun SOSCardItemNew(
    sos: SOSRequest,
    onNavigateToMap: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
    val timeString = try { dateFormat.format(Date(sos.timestamp)) } catch (e: Exception) { "Vừa xong" }

    val provinceDisplay = if (!sos.province.isNullOrBlank()) "📍 ${sos.province}" else "📍 Chưa xác định"

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Trạng thái + Thời gian
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                    Text("SOS KHẨN CẤP", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timeString, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Địa chỉ Tỉnh
            Text(
                text = provinceDisplay,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1976D2)
            )

            Spacer(Modifier.height(8.dp))

            // Nội dung tin nhắn
            Text(sos.message.ifBlank { "Không có nội dung mô tả" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), lineHeight = 24.sp)

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.Gray.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            // Thông tin liên hệ
            InfoRow(icon = Icons.Default.Person, text = sos.email.ifBlank { "Ẩn danh" })
            Spacer(Modifier.height(6.dp))
            InfoRow(icon = Icons.Default.Call, text = sos.phone, isBold = true)

            Spacer(Modifier.height(16.dp))

            // Nút hành động
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Gọi điện
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sos.phone}"))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gọi điện")
                }

                // Xem bản đồ cá nhân (chỉ đường)
                Button(
                    onClick = {
                        val safeName = if (sos.phone.isNotBlank()) sos.phone else "SOS"
                        val cleanName = safeName.replace("/", "-")
                        onNavigateToMap(sos.lat, sos.lon, cleanName)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Icon(Icons.Default.NearMe, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Xem Bản Đồ")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isBold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = if (isBold) Color.Black else Color.DarkGray, fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun EmptyState(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(120.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF2196F3), modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("An toàn!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}