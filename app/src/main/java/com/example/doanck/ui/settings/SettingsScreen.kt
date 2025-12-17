package com.example.doanck.ui.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- MÀU SẮC GIAO DIỆN ---
val ModernBlue = Color(0xFF3B82F6)
val ModernBg = Color(0xFFF8FAFC)
val CardBg = Color.White.copy(alpha = 0.95f)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val ErrorColor = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appDataStore: AppDataStore,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Firebase Instances
    val db = Firebase.firestore
    val auth = Firebase.auth
    val storage = Firebase.storage
    val currentUser = auth.currentUser

    // --- STATE QUẢN LÝ DỮ LIỆU ---
    var userProfile by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isAvatarUploading by remember { mutableStateOf(false) }

    // --- 🔥 LOGIC ĐỒNG BỘ: CLOUD -> LOCAL (Để đổi máy không mất) ---
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            // Lắng nghe thay đổi thực tế trên Server
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data ?: emptyMap()
                        userProfile = data // Cập nhật UI ngay lập tức

                        // Tự động lưu dự phòng vào bộ nhớ máy (AppDataStore)
                        scope.launch {
                            val cloudDob = data["dob"]?.toString()
                            val cloudPhone = data["phone"]?.toString()
                            val cloudGender = data["gender"]?.toString()
                            val cloudAvatar = data["avatarUrl"]?.toString()

                            if (cloudDob != null) appDataStore.saveDob(cloudDob)
                            if (cloudPhone != null) appDataStore.savePhone(cloudPhone)
                            if (cloudGender != null) appDataStore.saveGender(cloudGender)
                            if (cloudAvatar != null) appDataStore.saveAvatarForCurrentUser(cloudAvatar)
                        }
                    }
                }
        }
    }

    // Dữ liệu hiển thị (Ưu tiên lấy từ Cloud mới tải về)
    val dateOfBirth = userProfile["dob"]?.toString() ?: "01/01/2000"
    val phoneNumber = userProfile["phone"]?.toString() ?: "Chưa cập nhật"
    val gender = userProfile["gender"]?.toString() ?: "Nam"
    val avatarUrl = userProfile["avatarUrl"]?.toString()

    // --- CÀI ĐẶT RIÊNG CỦA MÁY (Local) ---
    val enableAnimation by appDataStore.enableAnimation.collectAsState(initial = true)
    val enableNotifications by appDataStore.enableNotifications.collectAsState(initial = true)
    val tempUnit by appDataStore.tempUnit.collectAsState(initial = "C")

    // --- UI DIALOG STATES ---
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    // State cho DatePicker (Hybrid)
    var showDobEditDialog by remember { mutableStateOf(false) }
    var showDatePickerOverlay by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)

    // Temp Inputs & Validation Errors
    var tempPhoneInput by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var tempDobInput by remember { mutableStateOf("") }
    var dobError by remember { mutableStateOf<String?>(null) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // --- HÀM VALIDATION & UPDATE ---
    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^0\\d{9}$")) // Bắt đầu bằng 0, đủ 10 số
    }

    fun isValidDate(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            val date = sdf.parse(dateStr)
            date != null && date.before(Date()) // Phải là ngày trong quá khứ
        } catch (e: Exception) {
            false
        }
    }

    // Hàm đẩy dữ liệu lên Cloud
    fun updateProfile(key: String, value: Any) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).set(mapOf(key to value), SetOptions.merge())
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Xử lý chọn ảnh Avatar
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            currentUser?.uid?.let { uid ->
                isAvatarUploading = true
                val ref = storage.reference.child("avatars/$uid.jpg")
                ref.putFile(selectedUri).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Khi có link ảnh -> Cập nhật vào Firestore -> Listener ở trên sẽ tự bắt và lưu về máy
                        updateProfile("avatarUrl", downloadUrl.toString())
                        isAvatarUploading = false
                    }
                }.addOnFailureListener {
                    isAvatarUploading = false
                    Toast.makeText(context, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- GIAO DIỆN CHÍNH ---
    Scaffold(
        containerColor = ModernBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ModernBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- AVATAR ---
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(120.dp).shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    border = BorderStroke(4.dp, Color.White),
                    color = Color.LightGray
                ) {
                    if (isAvatarUploading) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(avatarUrl ?: android.R.drawable.sym_def_app_icon),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clickable { avatarLauncher.launch("image/*") }
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(34.dp).offset(x = (-2).dp, y = (-2).dp).clickable { avatarLauncher.launch("image/*") },
                    shape = CircleShape, color = ModernBlue, border = BorderStroke(2.dp, Color.White)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }

            Text(currentUser?.email ?: "", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

            // --- DANH SÁCH CÀI ĐẶT ---
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                SettingsGroup("HỒ SƠ CÁ NHÂN") {
                    ProSettingActionItem(Icons.Outlined.CalendarToday, Color(0xFFF43F5E), "Ngày sinh", dateOfBirth) {
                        tempDobInput = dateOfBirth
                        dobError = null
                        showDobEditDialog = true
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(0.05f))

                    ProSettingActionItem(Icons.Outlined.Phone, Color(0xFF0EA5E9), "Số điện thoại", phoneNumber) {
                        tempPhoneInput = if(phoneNumber == "Chưa cập nhật") "" else phoneNumber
                        phoneError = null
                        showPhoneDialog = true
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(0.05f))

                    ProSettingActionItem(Icons.Outlined.Person, Color(0xFF8B5CF6), "Giới tính", gender) { showGenderDialog = true }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsGroup("ỨNG DỤNG") {
                    ProSettingSwitchItem(Icons.Outlined.Notifications, Color(0xFFFF5722), "Nhận thông báo", enableNotifications) {
                        scope.launch { appDataStore.setEnableNotifications(it) }
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(0.05f))

                    ProSettingSwitchItem(Icons.Outlined.AutoAwesome, Color(0xFFF59E0B), "Hiệu ứng thời tiết", enableAnimation) {
                        scope.launch { appDataStore.setEnableAnimation(it) }
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(0.05f))

                    ProSettingActionItem(Icons.Outlined.Thermostat, Color(0xFF10B981), "Đơn vị nhiệt độ", "Độ $tempUnit") {
                        scope.launch { appDataStore.setTempUnit(if (tempUnit == "C") "F" else "C") }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsGroup("HỆ THỐNG") {
                    ProSettingActionItem(Icons.Outlined.Lock, Color(0xFF64748B), "Đổi mật khẩu") { showChangePassDialog = true }
                    Divider(Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(0.05f))

                    ProSettingActionItem(Icons.Outlined.Info, Color(0xFF3B82F6), "Về ứng dụng") { showAppInfoDialog = true }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { scope.launch { appDataStore.clearSession(); onLogout() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE4E6), contentColor = Color(0xFFE11D48))
                ) {
                    Icon(Icons.Outlined.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Đăng xuất", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // --- CÁC DIALOG (CỬA SỔ BẬT LÊN) ---

    // 1. DIALOG NGÀY SINH (Hybrid)
    if (showDobEditDialog) {
        AlertDialog(
            onDismissRequest = { showDobEditDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Cập nhật ngày sinh", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nhập định dạng dd/MM/yyyy hoặc chọn lịch", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tempDobInput,
                            onValueChange = { tempDobInput = it; dobError = null },
                            label = { Text("Ngày sinh") },
                            placeholder = { Text("01/01/2000") },
                            isError = dobError != null,
                            supportingText = { if (dobError != null) Text(dobError!!, color = ErrorColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { showDatePickerOverlay = true },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ModernBlue.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = ModernBlue)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (isValidDate(tempDobInput)) {
                        updateProfile("dob", tempDobInput)
                        showDobEditDialog = false
                    } else {
                        dobError = "Ngày không hợp lệ!"
                    }
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showDobEditDialog = false }) { Text("Hủy") } }
        )
    }

    // Overlay Lịch
    if (showDatePickerOverlay) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerOverlay = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        tempDobInput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                        dobError = null
                    }
                    showDatePickerOverlay = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerOverlay = false }) { Text("Đóng") } }
        ) { DatePicker(state = datePickerState) }
    }

    // 2. DIALOG SỐ ĐIỆN THOẠI (Validation)
    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Cập nhật số điện thoại", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPhoneInput,
                    onValueChange = {
                        tempPhoneInput = it
                        if (it.all { c -> c.isDigit() }) phoneError = null
                    },
                    label = { Text("Số điện thoại") },
                    isError = phoneError != null,
                    supportingText = { if (phoneError != null) Text(phoneError!!, color = ErrorColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (isValidPhoneNumber(tempPhoneInput)) {
                        updateProfile("phone", tempPhoneInput)
                        showPhoneDialog = false
                    } else {
                        phoneError = "SĐT phải 10 số, bắt đầu bằng 0"
                    }
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showPhoneDialog = false }) { Text("Hủy") } }
        )
    }

    // 3. DIALOG GIỚI TÍNH
    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Chọn giới tính", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Nam", "Nữ", "Khác").forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                updateProfile("gender", option)
                                showGenderDialog = false
                            }.padding(12.dp)
                        ) {
                            RadioButton(selected = (gender == option), onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGenderDialog = false }) { Text("Đóng") } }
        )
    }

    // 4. DIALOG ĐỔI MẬT KHẨU
    if (showChangePassDialog) {
        var isProcessing by remember { mutableStateOf(false) }
        DisposableEffect(Unit) { onDispose { oldPassword = ""; newPassword = "" } }

        AlertDialog(
            onDismissRequest = { if (!isProcessing) showChangePassDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Bảo mật tài khoản", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Xác nhận mật khẩu cũ trước khi đổi.", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Mật khẩu hiện tại") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, enabled = !isProcessing
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới (min 6 ký tự)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, enabled = !isProcessing
                    )
                    if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            },
            confirmButton = {
                Button(
                    enabled = !isProcessing && oldPassword.isNotEmpty() && newPassword.length >= 6,
                    onClick = {
                        val user = Firebase.auth.currentUser
                        if (user != null && user.email != null) {
                            isProcessing = true
                            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                            user.reauthenticate(credential).addOnSuccessListener {
                                user.updatePassword(newPassword).addOnSuccessListener {
                                    Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                                    showChangePassDialog = false
                                    isProcessing = false
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                                    isProcessing = false
                                }
                            }.addOnFailureListener {
                                Toast.makeText(context, "Sai mật khẩu cũ!", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                            }
                        }
                    }
                ) { Text("Cập nhật") }
            },
            dismissButton = { TextButton(enabled = !isProcessing, onClick = { showChangePassDialog = false }) { Text("Hủy") } }
        )
    }

    // 5. APP INFO
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text("Weather Connect") },
            text = { Text("Phiên bản 1.0.0\nNhóm Đồ Án Di Động") },
            confirmButton = { Button(onClick = { showAppInfoDialog = false }) { Text("Đóng") } }
        )
    }
}

// --- UI HELPERS ---
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = CardBg, border = BorderStroke(1.dp, Color.White), shadowElevation = 2.dp) {
            Column { content() }
        }
    }
}

@Composable
fun ProSettingActionItem(icon: ImageVector, color: Color, title: String, valueText: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(38.dp), shape = RoundedCornerShape(10.dp), color = color.copy(0.1f)) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium, color = TextPrimary)
        if (valueText != null) Text(valueText, color = TextSecondary, fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

@Composable
fun ProSettingSwitchItem(icon: ImageVector, color: Color, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(38.dp), shape = RoundedCornerShape(10.dp), color = color.copy(0.1f)) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = ModernBlue))
    }
}