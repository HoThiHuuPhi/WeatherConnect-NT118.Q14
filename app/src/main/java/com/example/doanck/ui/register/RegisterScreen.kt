@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.doanck.ui.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

data class CloudParticleReg(
    var x: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val alpha: Float
)

val LightBlueSkyReg = Color(0xFF87CEEB)
val LightGoldenSunReg = Color(0xFFFDB813)
val TextDarkBlueReg = Color(0xFF1E3A8A) // Màu chữ tối
val GlassBorderLightReg = Color(0xFFFFFFFF).copy(alpha = 0.5f)

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Animation xuất hiện
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // Tạo danh sách các đám mây
    val clouds = remember {
        List(8) { // Số lượng mây
            CloudParticleReg(
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
                    if (cloud.x < -200f * cloud.scale) {
                        cloud.x = screenWidth + 200f * cloud.scale
                    }
                }
            }
        }
    }

    // Animation cho mặt trời tỏa sáng nhẹ
    val infiniteTransition = rememberInfiniteTransition(label = "sunReg")
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "sunScaleReg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Gradient bầu trời nắng: Xanh da trời -> Vàng nhạt
                Brush.verticalGradient(
                    listOf(LightBlueSkyReg, Color(0xFFB0E0E6), Color(0xFFFFFACD))
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
                    colors = listOf(LightGoldenSunReg.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(sunCenterX, sunCenterY),
                    radius = sunRadius * 2
                ),
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius * 2
            )
            // Lõi mặt trời
            drawCircle(
                color = LightGoldenSunReg,
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius
            )

            // Vẽ các đám mây
            clouds.forEach { cloud ->
                drawCloudReg(offset = Offset(cloud.x, cloud.y), scale = cloud.scale, alpha = cloud.alpha)
            }
        }

        // 2. Nội dung chính (Card Đăng Ký)
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
                        "THAM GIA",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkBlueReg, // Chữ màu tối
                        letterSpacing = 3.sp
                    )
                    Text(
                        "CỘNG ĐỒNG",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFF59E0B), // Màu vàng cam nắng
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(500, delayMillis = 300)) + fadeIn(tween(1000, delayMillis = 300))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                    listOf(GlassBorderLightReg, Color.Transparent, GlassBorderLightReg)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Email
                            CustomTextFieldRegLight(email, { email = it }, "Email", Icons.Default.Email)
                            Spacer(Modifier.height(12.dp))

                            // Password
                            CustomTextFieldRegLight(password, { password = it }, "Mật khẩu", Icons.Default.Lock, true, passwordVisible, { passwordVisible = !passwordVisible })
                            Spacer(Modifier.height(12.dp))

                            // Confirm Password
                            CustomTextFieldRegLight(confirmPassword, { confirmPassword = it }, "Nhập lại mật khẩu", Icons.Default.Lock, true, passwordVisible, { passwordVisible = !passwordVisible })

                            Spacer(Modifier.height(20.dp))

                            error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)) }

                            // Button (Gradient màu nắng: Cam -> Vàng)
                            val scale by animateFloatAsState(if (loading) 0.95f else 1f, label = "s")
                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) { error = "Nhập thiếu thông tin"; return@Button }
                                    if (password != confirmPassword) { error = "Mật khẩu không khớp"; return@Button }
                                    loading = true
                                    auth.createUserWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            loading = false
                                            if (task.isSuccessful) onRegisterSuccess() else error = task.exception?.message
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).scale(scale),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxSize().background(
                                        // Gradient Nắng: Cam -> Vàng rực
                                        Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if(loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("ĐĂNG KÝ NGAY", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đã có tài khoản?", color = TextDarkBlueReg.copy(alpha = 0.7f))
                TextButton(onClick = onBackToLogin) { Text("Đăng nhập", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Hàm vẽ đám mây đơn giản (Copy lại để file độc lập)
private fun DrawScope.drawCloudReg(offset: Offset, scale: Float, alpha: Float) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val baseRadius = 30.dp.toPx() * scale
    drawCircle(color = cloudColor, radius = baseRadius, center = offset)
    drawCircle(color = cloudColor, radius = baseRadius * 0.8f, center = Offset(offset.x - baseRadius * 0.7f, offset.y + baseRadius * 0.2f))
    drawCircle(color = cloudColor, radius = baseRadius * 0.9f, center = Offset(offset.x + baseRadius * 0.7f, offset.y + baseRadius * 0.1f))
}

// TextField dùng chung cho màn hình này (Phiên bản Light)
@Composable
fun CustomTextFieldRegLight(
    value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false, passwordVisible: Boolean = false, onTogglePassword: () -> Unit = {}
) {
    val contentColor = TextDarkBlueReg.copy(alpha = 0.8f)
    val focusColor = Color(0xFFF59E0B) // Màu cam nắng khi focus

    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = contentColor) },
        leadingIcon = { Icon(icon, null, tint = contentColor) },
        trailingIcon = if (isPassword) { { IconButton(onTogglePassword) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = contentColor) } } } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = focusColor,
            unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
            cursorColor = focusColor,
            focusedTextColor = TextDarkBlueReg,
            unfocusedTextColor = TextDarkBlueReg,
            focusedContainerColor = Color.White.copy(alpha = 0.3f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
        )
    )
}