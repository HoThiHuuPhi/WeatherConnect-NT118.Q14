package com.example.doanck.ui.main

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.doanck.data.datastore.AppDataStore
import kotlinx.coroutines.launch
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Calendar

// --- MÀU SẮC CHUYÊN NGHIỆP ---
val BackgroundGradientStart = Color(0xFFF0F4F8)
val BackgroundGradientEnd = Color(0xFFD9E2EC)
val SurfaceColor = Color.White.copy(alpha = 0.75f)
val PrimaryTextColor = Color(0xFF102A43)
val SecondaryTextColor = Color(0xFF486581)

@Composable
fun SettingsScreen(
    appDataStore: AppDataStore,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // --- DATASTORE STATES (Dữ liệu thực từ bộ nhớ máy) ---
    val enableAnimation by appDataStore.enableAnimation.collectAsState(initial = true)
    val tempUnit by appDataStore.tempUnit.collectAsState(initial = "C")
    val userEmail by appDataStore.userEmail.collectAsState(initial = "Đang tải...")
    val avatarUriString by appDataStore.userAvatar.collectAsState(initial = null)

    // ✅ 1. Thay đổi: Lấy thông tin cá nhân từ DataStore thay vì biến tạm
    val dateOfBirth by appDataStore.userDob.collectAsState(initial = "01/01/2000")
    val phoneNumber by appDataStore.userPhone.collectAsState(initial = "Chưa cập nhật")
    val gender by appDataStore.userGender.collectAsState(initial = "Nam")

    // --- UI States (Dialogs) ---
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) } // Thêm dialog sửa SĐT

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var tempPhoneInput by remember { mutableStateOf("") } // Biến tạm nhập SĐT

    // --- Logic DatePicker ---
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newDob = "$dayOfMonth/${month + 1}/$year"
            // ✅ 2. Thay đổi: Lưu vào DataStore ngay khi chọn xong
            scope.launch { appDataStore.saveDob(newDob) }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // --- Logic Thay đổi Avatar ---
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            scope.launch {
                appDataStore.saveAvatarForCurrentUser(it.toString())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            )
    ) {
        // --- NỘI DUNG CHÍNH ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Cài đặt",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryTextColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- PROFILE SECTION ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    val painter = if (avatarUriString != null) {
                        rememberAsyncImagePainter(avatarUriString)
                    } else {
                        rememberAsyncImagePainter(android.R.drawable.sym_def_app_icon)
                    }

                    Image(
                        painter = painter,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .shadow(8.dp, CircleShape)
                            .clickable { avatarLauncher.launch(arrayOf("image/*")) }
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3B82F6),
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier.size(36.dp).offset(x = 4.dp, y = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userEmail,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTextColor
                )
                Text(
                    text = "Thành viên tích cực",
                    fontSize = 14.sp,
                    color = SecondaryTextColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {

                // === GROUP: HỒ SƠ CÁ NHÂN ===
                SettingsGroup(title = "Hồ sơ cá nhân") {
                    // Ngày sinh
                    ProSettingActionItem(
                        icon = Icons.Outlined.CalendarToday,
                        iconBgColor = Color(0xFFEC4899),
                        title = "Ngày sinh",
                        valueText = dateOfBirth
                    ) {
                        datePickerDialog.show()
                    }

                    Divider(color = Color.Gray.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))

                    // Số điện thoại
                    ProSettingActionItem(
                        icon = Icons.Outlined.Phone,
                        iconBgColor = Color(0xFF0EA5E9),
                        title = "Số điện thoại",
                        valueText = phoneNumber
                    ) {
                        tempPhoneInput = phoneNumber // Load số cũ vào dialog
                        showPhoneDialog = true
                    }

                    Divider(color = Color.Gray.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))

                    // Giới tính
                    ProSettingActionItem(
                        icon = Icons.Outlined.Person,
                        iconBgColor = Color(0xFF8B5CF6),
                        title = "Giới tính",
                        valueText = gender
                    ) {
                        showGenderDialog = true
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Group: Chung
                SettingsGroup(title = "Chung") {
                    ProSettingSwitchItem(
                        icon = Icons.Default.Animation,
                        iconBgColor = Color(0xFF8B5CF6),
                        title = "Hiệu ứng thời tiết động",
                        checked = enableAnimation,
                        onCheckedChange = { scope.launch { appDataStore.setEnableAnimation(it) } }
                    )
                    Divider(color = Color.Gray.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ProSettingActionItem(
                        icon = Icons.Default.Thermostat,
                        iconBgColor = Color(0xFFF59E0B),
                        title = "Đơn vị nhiệt độ",
                        valueText = "Độ $tempUnit"
                    ) {
                        scope.launch {
                            val newUnit = if (tempUnit == "C") "F" else "C"
                            appDataStore.setTempUnit(newUnit)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Group: Tài khoản
                SettingsGroup(title = "Tài khoản") {
                    ProSettingActionItem(
                        icon = Icons.Outlined.Lock,
                        iconBgColor = Color(0xFF10B981),
                        title = "Đổi mật khẩu"
                    ) {
                        oldPassword = ""
                        newPassword = ""
                        showChangePassDialog = true
                    }
                    Divider(color = Color.Gray.copy(0.1f), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ProSettingActionItem(
                        icon = Icons.Outlined.Logout,
                        iconBgColor = Color(0xFFEF4444),
                        title = "Đăng xuất",
                        textColor = Color(0xFFEF4444)
                    ) {
                        scope.launch {
                            appDataStore.clearSession()
                            onLogout()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Group: Thông tin
                SettingsGroup(title = "Thông tin") {
                    ProSettingActionItem(
                        icon = Icons.Outlined.Info,
                        iconBgColor = Color(0xFF3B82F6),
                        title = "Về ứng dụng này"
                    ) {
                        showAppInfoDialog = true
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }

        // --- NÚT BACK ---
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryTextColor,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    // ================== DIALOGS ==================

    // 1. Dialog Đổi mật khẩu
    if (showChangePassDialog) {
        var isProcessing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showChangePassDialog = false },
            containerColor = Color.White,
            title = { Text("Đổi mật khẩu", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Mật khẩu cũ") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới (min 6 ký tự)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isProcessing) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isProcessing,
                    onClick = {
                        val user = Firebase.auth.currentUser
                        if (user != null && user.email != null && oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                            if (newPassword.length < 6) {
                                Toast.makeText(context, "Mật khẩu quá ngắn", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isProcessing = true
                            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                            user.reauthenticate(credential).addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                        isProcessing = false
                                        if (updateTask.isSuccessful) {
                                            Toast.makeText(context, "Thành công!", Toast.LENGTH_SHORT).show()
                                            showChangePassDialog = false
                                        } else {
                                            Toast.makeText(context, "Lỗi: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isProcessing = false
                                    Toast.makeText(context, "Sai mật khẩu cũ", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(enabled = !isProcessing, onClick = { showChangePassDialog = false }) { Text("Hủy") }
            }
        )
    }

    // 2. Dialog Giới tính
    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            containerColor = Color.White,
            title = { Text("Chọn giới tính", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Nam", "Nữ", "Khác").forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ✅ 3. Thay đổi: Lưu vào DataStore
                                    scope.launch { appDataStore.saveGender(option) }
                                    showGenderDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = (gender == option),
                                onClick = {
                                    scope.launch { appDataStore.saveGender(option) }
                                    showGenderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGenderDialog = false }) { Text("Đóng") }
            }
        )
    }

    // 3. Dialog Đổi Số điện thoại (Mới thêm)
    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            containerColor = Color.White,
            title = { Text("Cập nhật Số điện thoại", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPhoneInput,
                    onValueChange = { tempPhoneInput = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { appDataStore.savePhone(tempPhoneInput) }
                        showPhoneDialog = false
                        Toast.makeText(context, "Đã lưu số điện thoại", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) { Text("Hủy") }
            }
        )
    }

    // 4. Dialog Thông tin App
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            icon = {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0EA5E9))
                }
            },
            containerColor = Color.White,
            title = { Text("Weather Connect", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Phiên bản 1.0.0 (Beta)", color = SecondaryTextColor)
                    Spacer(Modifier.height(8.dp))
                    Text("Phát triển bởi:", fontSize = 12.sp, color = SecondaryTextColor)
                    Text("Minh Châu & Hữu Phi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryTextColor)
                }
            },
            confirmButton = {
                Button(onClick = { showAppInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                    Text("Đóng")
                }
            }
        )
    }
}

// ==========================================
// 🔥 CUSTOM COMPONENTS
// ==========================================

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryTextColor,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            color = SurfaceColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ProSettingSwitchItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, color = PrimaryTextColor, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
        )
    }
}

@Composable
fun ProSettingActionItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    valueText: String? = null,
    textColor: Color = PrimaryTextColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, color = textColor, modifier = Modifier.weight(1f))
        if (valueText != null) {
            Text(text = valueText, color = SecondaryTextColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray.copy(0.5f))
    }
}