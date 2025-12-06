@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.doanck.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import kotlin.random.Random

// --- HỆ THỐNG MÂY BAY ---
data class CloudParticle(
    var x: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val alpha: Float
)

// Màu sắc chủ đạo cho theme sáng
val LightBlueSky = Color(0xFF87CEEB)
val LightGoldenSun = Color(0xFFFDB813)
val TextDarkBlue = Color(0xFF1E3A8A) // Màu chữ tối để dễ đọc
val GlassBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.5f)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animation xuất hiện
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // Tạo danh sách các đám mây
    val clouds = remember {
        List(8) { // Số lượng mây
            CloudParticle(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * (screenHeight / 2), // Mây chỉ bay ở nửa trên
                speed = Random.nextFloat() * 2f + 0.5f,
                scale = Random.nextFloat() * 0.5f + 0.8f,
                alpha = Random.nextFloat() * 0.3f + 0.6f
            )
        }
    }

    // Loop animation cho mây bay
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                time = it
                clouds.forEach { cloud ->
                    cloud.x -= cloud.speed // Bay từ phải sang trái
                    // Nếu bay hết màn hình thì reset lại về bên phải
                    if (cloud.x < -200f * cloud.scale) {
                        cloud.x = screenWidth + 200f * cloud.scale
                    }
                }
            }
        }
    }

    // Animation cho mặt trời tỏa sáng nhẹ
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "sunScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Gradient bầu trời nắng: Xanh da trời -> Vàng nhạt
                Brush.verticalGradient(
                    listOf(LightBlueSky, Color(0xFFB0E0E6), Color(0xFFFFFACD))
                )
            )
    ) {
        // 1. Vẽ Mặt Trời và Mây (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trigger = time // Kích hoạt vẽ lại

            // Vẽ Mặt Trời tỏa sáng ở góc trên phải
            val sunCenterX = size.width * 0.85f
            val sunCenterY = size.height * 0.15f
            val sunRadius = 60.dp.toPx() * sunScale

            // Vòng sáng ngoài (Hào quang)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LightGoldenSun.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(sunCenterX, sunCenterY),
                    radius = sunRadius * 2
                ),
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius * 2
            )
            // Lõi mặt trời
            drawCircle(
                color = LightGoldenSun,
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius
            )

            // Vẽ các đám mây
            clouds.forEach { cloud ->
                // Hàm vẽ một đám mây đơn giản bằng các hình tròn
                drawCloud(offset = Offset(cloud.x, cloud.y), scale = cloud.scale, alpha = cloud.alpha)
            }
        }

        // 2. Nội dung chính (Card đăng nhập)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(1000))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "WEATHER",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkBlue, // Chữ màu tối
                        letterSpacing = 4.sp
                    )
                    Text(
                        "CONNECT",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFF59E0B), // Màu vàng cam nắng
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(40.dp))
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(500, delayMillis = 300)) + fadeIn(tween(1000, delayMillis = 300))
            ) {
                // ★ BRIGHT GLASS CARD ★ (Kính sáng)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    // Nền trắng mờ
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Viền trắng sáng
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(GlassBorderLight, Color.Transparent, GlassBorderLight)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            // Input Email (Dùng phiên bản Light)
                            CustomTextFieldLight(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email",
                                icon = Icons.Default.Email
                            )

                            Spacer(Modifier.height(16.dp))

                            // Input Password (Dùng phiên bản Light)
                            CustomTextFieldLight(
                                value = password,
                                onValueChange = { password = it },
                                label = "Mật khẩu",
                                icon = Icons.Default.Lock,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible }
                            )

                            // Forgot Password Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onNavigateToForgotPassword) {
                                    // Màu chữ link tối hơn
                                    Text("Quên mật khẩu?", color = Color(0xFF4F46E5), fontSize = 13.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Error Message
                            error?.let {
                                Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                            }

                            // Login Button (Gradient màu nắng: Cam -> Vàng)
                            val buttonScale by animateFloatAsState(targetValue = if (loading) 0.95f else 1f, label = "btn")

                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        error = "Vui lòng nhập đầy đủ thông tin"
                                        return@Button
                                    }
                                    loading = true
                                    error = null
                                    auth.signInWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            loading = false
                                            if (task.isSuccessful) onLoginSuccess()
                                            else error = task.exception?.message
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .scale(buttonScale),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            // Gradient Nắng: Cam -> Vàng rực
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("ĐĂNG NHẬP", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer Text
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, delayMillis = 600))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chưa có tài khoản?", color = TextDarkBlue.copy(alpha = 0.7f))
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Tạo mới ngay", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Hàm vẽ đám mây đơn giản
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    offset: Offset,
    scale: Float,
    alpha: Float
) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val baseRadius = 30.dp.toPx() * scale

    // Vẽ 3 hình tròn chồng lên nhau để tạo hình đám mây
    drawCircle(color = cloudColor, radius = baseRadius, center = offset)
    drawCircle(color = cloudColor, radius = baseRadius * 0.8f, center = Offset(offset.x - baseRadius * 0.7f, offset.y + baseRadius * 0.2f))
    drawCircle(color = cloudColor, radius = baseRadius * 0.9f, center = Offset(offset.x + baseRadius * 0.7f, offset.y + baseRadius * 0.1f))
}

// Custom TextField cho nền sáng (Chữ màu tối)
@Composable
fun CustomTextFieldLight(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    val contentColor = TextDarkBlue.copy(alpha = 0.8f)
    val focusColor = Color(0xFFF59E0B) // Màu cam nắng khi focus

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = contentColor) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = contentColor) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = focusColor,
            unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
            cursorColor = focusColor,
            focusedTextColor = TextDarkBlue, // Chữ màu tối
            unfocusedTextColor = TextDarkBlue,
            focusedContainerColor = Color.White.copy(alpha = 0.3f), // Nền input sáng hơn
            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
        )
    )
}