package com.example.doanck.ui.main

import android.app.DatePickerDialog
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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun SettingsScreen(
    appDataStore: AppDataStore,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val auth = Firebase.auth
    val storage = Firebase.storage
    val currentUser = auth.currentUser

    // --- STATES DỮ LIỆU TỪ CLOUD ---
    var userProfile by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isAvatarUploading by remember { mutableStateOf(false) }

    // Lắng nghe dữ liệu Realtime từ Firestore
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.data ?: emptyMap()
                    }
                }
        }
    }

    // Giá trị hiển thị từ Cloud
    val dateOfBirth = userProfile["dob"]?.toString() ?: "01/01/2000"
    val phoneNumber = userProfile["phone"]?.toString() ?: "Chưa cập nhật"
    val gender = userProfile["gender"]?.toString() ?: "Nam"
    val avatarUrl = userProfile["avatarUrl"]?.toString()

    // Cài đặt Local từ DataStore
    val enableAnimation by appDataStore.enableAnimation.collectAsState(initial = true)
    val tempUnit by appDataStore.tempUnit.collectAsState(initial = "C")

    // --- UI STATES ---
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    var tempPhoneInput by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // --- HÀM CẬP NHẬT FIRESTORE ---
    fun updateProfile(key: String, value: Any) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).set(mapOf(key to value), SetOptions.merge())
                .addOnFailureListener { Toast.makeText(context, "Lỗi đồng bộ!", Toast.LENGTH_SHORT).show() }
        }
    }

    // --- LOGIC UPLOAD ẢNH ---
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            currentUser?.uid?.let { uid ->
                isAvatarUploading = true
                val ref = storage.reference.child("avatars/$uid.jpg")
                ref.putFile(selectedUri).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updateProfile("avatarUrl", downloadUrl.toString())
                        isAvatarUploading = false
                    }
                }.addOnFailureListener { isAvatarUploading = false }
            }
        }
    }

    // --- DATE PICKER ---
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        updateProfile("dob", "$d/${m + 1}/$y")
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF0F4F8), Color(0xFFD9E2EC))))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // --- AVATAR ---
            Box(contentAlignment = Alignment.BottomEnd) {
                if (isAvatarUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(110.dp))
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(avatarUrl ?: android.R.drawable.sym_def_app_icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                            .clickable { avatarLauncher.launch("image/*") }
                    )
                }
                IconButton(onClick = { avatarLauncher.launch("image/*") }, modifier = Modifier.size(32.dp).background(Color(0xFF3B82F6), CircleShape)) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SettingsGroup("Hồ sơ cá nhân (Cloud)") {
                    ProSettingActionItem(Icons.Outlined.CalendarToday, Color(0xFFEC4899), "Ngày sinh", dateOfBirth) { datePickerDialog.show() }
                    ProSettingActionItem(Icons.Outlined.Phone, Color(0xFF0EA5E9), "Số điện thoại", phoneNumber) {
                        tempPhoneInput = if(phoneNumber == "Chưa cập nhật") "" else phoneNumber
                        showPhoneDialog = true
                    }
                    ProSettingActionItem(Icons.Outlined.Person, Color(0xFF8B5CF6), "Giới tính", gender) { showGenderDialog = true }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsGroup("Cài đặt ứng dụng") {
                    ProSettingSwitchItem(Icons.Default.Animation, Color(0xFF8B5CF6), "Hiệu ứng động", enableAnimation) {
                        scope.launch { appDataStore.setEnableAnimation(it) }
                    }
                    ProSettingActionItem(Icons.Default.Thermostat, Color(0xFFF59E0B), "Đơn vị nhiệt độ", "Độ $tempUnit") {
                        scope.launch { appDataStore.setTempUnit(if (tempUnit == "C") "F" else "C") }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsGroup("Hệ thống") {
                    ProSettingActionItem(Icons.Outlined.Lock, Color(0xFF10B981), "Đổi mật khẩu") { showChangePassDialog = true }
                    ProSettingActionItem(Icons.Outlined.Info, Color(0xFF3B82F6), "Thông tin app") { showAppInfoDialog = true }
                    ProSettingActionItem(Icons.Outlined.Logout, Color(0xFFEF4444), "Đăng xuất", textColor = Color.Red) {
                        scope.launch { appDataStore.clearSession(); onLogout() }
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }

        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(16.dp).background(Color.White.copy(0.5f), CircleShape)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
        }
    }

    // --- DIALOGS ---

    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Số điện thoại") },
            text = { OutlinedTextField(value = tempPhoneInput, onValueChange = { tempPhoneInput = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) },
            confirmButton = { Button(onClick = { updateProfile("phone", tempPhoneInput); showPhoneDialog = false }) { Text("Lưu") } }
        )
    }

    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text("Giới tính") },
            text = {
                Column {
                    listOf("Nam", "Nữ", "Khác").forEach { opt ->
                        Row(Modifier.fillMaxWidth().clickable { updateProfile("gender", opt); showGenderDialog = false }.padding(12.dp)) {
                            RadioButton(selected = (gender == opt), onClick = null)
                            Text(opt, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGenderDialog = false }) { Text("Đóng") } }
        )
    }

    if (showChangePassDialog) {
        var isProcessing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showChangePassDialog = false },
            title = { Text("Đổi mật khẩu") },
            text = {
                Column {
                    OutlinedTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = { Text("Mật khẩu cũ") }, visualTransformation = PasswordVisualTransformation())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val user = Firebase.auth.currentUser
                    if (user != null && oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                        isProcessing = true
                        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                        user.reauthenticate(credential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                user.updatePassword(newPassword).addOnSuccessListener {
                                    Toast.makeText(context, "Thành công", Toast.LENGTH_SHORT).show()
                                    showChangePassDialog = false
                                }
                            } else { Toast.makeText(context, "Sai mật khẩu", Toast.LENGTH_SHORT).show() }
                            isProcessing = false
                        }
                    }
                }) { Text("Lưu") }
            }
        )
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text("Weather Connect") },
            text = { Text("Phiên bản 1.0.0\nPhát triển bởi Minh Châu & Hữu Phi") },
            confirmButton = { Button(onClick = { showAppInfoDialog = false }) { Text("Đóng") } }
        )
    }
}

// --- UI HELPERS ---

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        Surface(color = Color.White.copy(0.8f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
fun ProSettingActionItem(icon: ImageVector, color: Color, title: String, valueText: String? = null, textColor: Color = Color.Black, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)), Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), color = textColor)
        if (valueText != null) Text(valueText, color = Color.Gray, fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

@Composable
fun ProSettingSwitchItem(icon: ImageVector, color: Color, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(color, RoundedCornerShape(8.dp)), Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}