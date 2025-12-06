package com.example.doanck.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // <--- MỚI: Import icon cây bút
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatScreen(
    onBack: () -> Unit = {},
    viewModel: CommunityChatViewModel = viewModel()
) {
    val context = LocalContext.current

    // ... (Phần Permission giữ nguyên) ...
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
        viewModel.getUserLocation(context)
    }
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.getUserLocation(context) else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val messages by viewModel.messages.collectAsState()
    val isLocationReady by viewModel.isLocationReady.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()

    // --- MỚI: Lấy tên hiện tại và trạng thái hiển thị Dialog ---
    val currentNickname by viewModel.nickname.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf("") }

    var input by remember { mutableStateOf(TextFieldValue("")) }
    var severity by remember { mutableStateOf("info") }
    var anonymous by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // --- MỚI: HỘP THOẠI NHẬP TÊN ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Đặt tên hiển thị") },
            text = {
                Column {
                    Text("Tên này sẽ hiện khi bạn nhắn tin:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        placeholder = { Text("Ví dụ: Minh Tuấn") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNickname(tempNameInput) // Lưu tên vào ViewModel
                    showNameDialog = false
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isLocationReady) currentAddress else "Đang định vị...", style = MaterialTheme.typography.titleMedium)
                        Text("Bán kính 10km • ${if(currentNickname.isEmpty()) "Chưa đặt tên" else currentNickname}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                // --- MỚI: Thêm nút sửa tên vào góc phải ---
                actions = {
                    IconButton(onClick = {
                        tempNameInput = currentNickname // Điền sẵn tên cũ nếu có
                        showNameDialog = true // Mở dialog
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Đổi tên")
                    }
                }
            )
        }
    ) { padding ->

        if (!isLocationReady) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                items(messages) { msg ->
                    MessageItem(msg)
                    Spacer(Modifier.height(8.dp))
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityChip("🟢 Tin", severity == "info") { severity = "info" }
                    Spacer(Modifier.width(8.dp))
                    SeverityChip("🟠 Quan trọng", severity == "warning") { severity = "warning" }
                    Spacer(Modifier.width(8.dp))
                    SeverityChip("🔴 Khẩn cấp", severity == "emergency") { severity = "emergency" }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = anonymous, onCheckedChange = { anonymous = it })
                    Text("Gửi ẩn danh", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            // Nhắc người dùng nếu chưa đặt tên
                            Text(if(currentNickname.isEmpty()) "Nhập tin nhắn..." else "Chat dưới tên $currentNickname...")
                        },
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (input.text.isNotBlank()) {
                                viewModel.sendMessage(input.text, severity, anonymous, context)
                                input = TextFieldValue("")
                            }
                        },
                        enabled = true
                    ) {
                        Text("Gửi")
                    }
                }
            }
        }
    }
}

// ... (Giữ nguyên các hàm SeverityChip, formatTimestamp, MessageItem như cũ không đổi) ...
@Composable
fun SeverityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
}
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
@Composable
fun MessageItem(msg: CommunityMessage) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""
    val isMe = msg.realUserId == currentUserId
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val cardColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE8F5E9)
    val finalColor = when (msg.severity) { "emergency" -> Color(0xFFFFEBEE); "warning" -> Color(0xFFFFF3E0); else -> cardColor }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Card(colors = CardDefaults.cardColors(containerColor = finalColor), modifier = Modifier.widthIn(max = 300.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (msg.severity) { "emergency" -> "🔴"; "warning" -> "🟠"; else -> "🟢" }
                    Text(text = icon)
                    Spacer(modifier = Modifier.width(4.dp))
                    if (msg.severity != "info") {
                        Text(text = if (msg.severity == "emergency") "KHẨN CẤP" else "QUAN TRỌNG", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = formatTimestamp(msg.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = msg.message, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                if (!isMe) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = if (msg.anonymous) "Người gửi: Ẩn danh" else "Người gửi: ${msg.userId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}