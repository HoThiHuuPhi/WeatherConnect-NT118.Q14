@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.doanck.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import androidx.compose.ui.geometry.Offset
import coil.compose.AsyncImage
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos

// --- MÀU CHỦ ĐẠO ---
val SkyBlueChat = Color(0xFF87CEEB)
val ChatBubbleMe = Color(0xFFF59E0B)
val TextDarkChat = Color(0xFF1E3A8A)

// --- MÂY BAY MODEL ---
data class CloudChat(var x: Float, val y: Float, val speed: Float, val scale: Float, val alpha: Float)

@Composable
fun CommunityChatScreen(
    onBack: () -> Unit = {},
    viewModel: CommunityChatViewModel = viewModel()
) {
    val context = LocalContext.current

    // Quyền lấy vị trí
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) viewModel.getUserLocation(context)
    }

    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.getUserLocation(context)
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val messages by viewModel.messages.collectAsState()
    val isLocationReady by viewModel.isLocationReady.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val currentNickname by viewModel.nickname.collectAsState()

    // UI States
    var showNameDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf("") }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var severity by remember { mutableStateOf("info") }
    var anonymous by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Auto Scroll
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Name Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Đặt tên hiển thị") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        placeholder = { Text("Ví dụ: Minh Tuấn") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNickname(tempNameInput)
                    showNameDialog = false
                }) {
                    Text("Lưu", color = ChatBubbleMe)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // -------------------------------
    // MÂY BAY ANIMATION
    // -------------------------------
    val config = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { config.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { config.screenHeightDp.dp.toPx() }

    val clouds = remember {
        List(6) {
            CloudChat(
                Random.nextFloat() * screenWidth,
                Random.nextFloat() * (screenHeight / 2),
                Random.nextFloat() * 1.5f + 0.3f,
                Random.nextFloat() * 0.4f + 0.7f,
                Random.nextFloat() * 0.3f + 0.4f
            )
        }
    }

    var time by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                time = it
                clouds.forEach { c ->
                    c.x -= c.speed
                    if (c.x < -200f * c.scale) {
                        c.x = screenWidth + 200f * c.scale
                    }
                }
            }
        }
    }

    // -------------------------------
    // UI CHÍNH
    // -------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SkyBlueChat, Color(0xFFB0E0E6), Color(0xFFFFFACD))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time
            clouds.forEach { c ->
                drawCloudChat(Offset(c.x, c.y), c.scale, c.alpha)
            }
        }

        Scaffold(
            topBar = {
                // AppBar kính mờ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.4f))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextDarkChat)
                        }

                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = if (isLocationReady) currentAddress else "Đang định vị...",
                                color = TextDarkChat,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bán kính 5km • ${if (currentNickname.isEmpty()) "Chưa đặt tên" else currentNickname}",
                                color = TextDarkChat.copy(0.6f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { viewModel.getUserLocation(context) }) {
                            Icon(Icons.Default.Refresh, null, tint = TextDarkChat)
                        }

                        IconButton(onClick = {
                            tempNameInput = currentNickname
                            showNameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, null, tint = ChatBubbleMe)
                        }
                    }
                }
            },
            containerColor = Color.Transparent,

            bottomBar = {
                BottomInputBar(
                    selectedImageUri = selectedImageUri,
                    onImageRemove = { selectedImageUri = null },
                    onPickImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    severity = severity,
                    onChangeSeverity = { severity = it },
                    anonymous = anonymous,
                    onToggleAnonymous = { anonymous = it },
                    input = input,
                    onInputChange = { input = it },
                    onSend = {
                        if (selectedImageUri != null) {
                            viewModel.sendImageMessage(
                                uri = selectedImageUri!!,
                                caption = input.text,
                                severity = severity,
                                anonymous = anonymous,
                                context = context
                            )
                            selectedImageUri = null
                        } else if (input.text.isNotEmpty()) {
                            viewModel.sendMessage(
                                input.text,
                                severity,
                                anonymous,
                                context
                            )
                        }
                        input = TextFieldValue("")
                    }
                )
            }
        ) { padding ->

            if (!isLocationReady) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChatBubbleMe)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageItemSunny(msg)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

//
// ----------------------
// 📌 Bottom Input Bar
// ----------------------
@Composable
fun BottomInputBar(
    selectedImageUri: Uri?,
    onImageRemove: () -> Unit,
    onPickImage: () -> Unit,
    severity: String,
    onChangeSeverity: (String) -> Unit,
    anonymous: Boolean,
    onToggleAnonymous: (Boolean) -> Unit,
    input: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.7f))
            .padding(12.dp)
    ) {

        // --- PREVIEW ẢNH ---
        AnimatedVisibility(visible = selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ChatBubbleMe, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onImageRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .background(Color.Black.copy(0.55f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }

        // --- Severity Chips ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeverityChipSunny("Thông tin", Color(0xFF10B981), severity == "info") {
                onChangeSeverity("info")
            }
            SeverityChipSunny("Quan trọng", Color(0xFFF59E0B), severity == "warning") {
                onChangeSeverity("warning")
            }
            SeverityChipSunny("Khẩn cấp", Color(0xFFEF4444), severity == "emergency") {
                onChangeSeverity("emergency")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {

            // Pick image button
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.Image, null, tint = TextDarkChat)
            }

            Checkbox(
                checked = anonymous,
                onCheckedChange = onToggleAnonymous,
                colors = CheckboxDefaults.colors(checkedColor = ChatBubbleMe)
            )
            Text("Ẩn danh", fontSize = 12.sp, color = TextDarkChat)

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = input,
                onValueChange = onInputChange,
                maxLines = 3,
                placeholder = {
                    Text(
                        text = if (selectedImageUri != null) "Thêm chú thích..." else "Nhập tin nhắn...",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChatBubbleMe,
                    focusedTextColor = TextDarkChat,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(ChatBubbleMe, Color(0xFFFBBF24))
                    ),
                    CircleShape
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
            }
        }
    }
}

//
// ----------------------
// Bubble Chat Item
// ----------------------
@Composable
fun MessageItemSunny(msg: CommunityMessage) {

    val currentUser = Firebase.auth.currentUser?.uid ?: ""
    val isMe = msg.realUserId == currentUser

    val bubbleColor = when {
        msg.severity == "emergency" -> Color(0xFFFFE4E6)
        msg.severity == "warning" -> Color(0xFFFFF1C2)
        isMe -> ChatBubbleMe
        else -> Color.White
    }

    val textColor = if (isMe) Color.White else TextDarkChat

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = if (msg.anonymous) "Ẩn danh" else msg.userId,
                fontSize = 12.sp,
                color = TextDarkChat.copy(0.6f),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            border = if (msg.severity == "emergency") BorderStroke(1.dp, Color(0xFFDC2626)) else null,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                if (msg.severity != "info") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = if (msg.severity == "emergency") Color(0xFFDC2626) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (msg.severity == "emergency") "KHẨN CẤP" else "QUAN TRỌNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // --- ẢNH BASE64 HIỂN THỊ ---
                msg.imageUrl?.let { base64 ->
                    val bitmap = remember(base64) {
                        try {
                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Nội dung text
                if (msg.message.isNotEmpty())
                    Text(msg.message, color = textColor)

                Text(
                    formatTimestamp(msg.timestamp),
                    color = textColor.copy(0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

//
// ----------------------
// Chip lựa chọn mức độ
// ----------------------
@Composable
fun SeverityChipSunny(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                color = if (selected) Color.White else TextDarkChat
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            containerColor = Color.White.copy(0.6f)
        ),
        shape = RoundedCornerShape(50)
    )
}

//
// ----------------------
// Mây bay
// ----------------------
private fun DrawScope.drawCloudChat(offset: Offset, scale: Float, alpha: Float) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val r = 30.dp.toPx() * scale

    drawCircle(cloudColor, r, offset)
    drawCircle(cloudColor, r * 0.8f, Offset(offset.x - r * 0.7f, offset.y + r * 0.3f))
    drawCircle(cloudColor, r * 0.9f, Offset(offset.x + r * 0.7f, offset.y + r * 0.2f))
}

//
// ----------------------
// Format giờ
// ----------------------
fun formatTimestamp(t: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
}
