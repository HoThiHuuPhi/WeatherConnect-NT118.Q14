@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.doanck.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import kotlin.random.Random

data class CloudParticleForgot(
    var x: Float, val y: Float, val speed: Float, val scale: Float, val alpha: Float
)

val SkyBlue = Color(0xFF87CEEB)
val SunGold = Color(0xFFFDB813)
val DarkBlueText = Color(0xFF1E3A8A)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.5f)

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Hiệu ứng xuất hiện
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    //  Hiệu ứng động cho mặt trời và mây
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    val clouds = remember {
        List(6) {
            CloudParticleForgot(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * (screenHeight / 3),
                speed = Random.nextFloat() * 1.5f + 0.5f,
                scale = Random.nextFloat() * 0.5f + 0.8f,
                alpha = Random.nextFloat() * 0.3f + 0.6f
            )
        }
    }

    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                time = it
                clouds.forEach { cloud ->
                    cloud.x -= cloud.speed
                    if (cloud.x < -200f * cloud.scale) cloud.x = screenWidth + 200f * cloud.scale
                }
            }
        }
    }

    // Animation mặt trời
    val infiniteTransition = rememberInfiniteTransition(label = "sunAnim")
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "sunScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SkyBlue, Color(0xFFB0E0E6), Color(0xFFFFFACD))
                )
            )
    ) {
        // 1. Vẽ Bầu trời
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time // Trigger redraw

            // Vẽ Mặt trời (Góc phải trên)
            val sunCx = size.width * 0.85f
            val sunCy = size.height * 0.15f
            val sunRadius = 60.dp.toPx() * sunScale

            drawCircle(
                brush = Brush.radialGradient(listOf(SunGold.copy(0.6f), Color.Transparent), Offset(sunCx, sunCy), sunRadius * 2),
                center = Offset(sunCx, sunCy), radius = sunRadius * 2
            )
            drawCircle(SunGold, radius = sunRadius, center = Offset(sunCx, sunCy))

            // Vẽ Mây
            clouds.forEach { cloud ->
                drawCloudForgot(Offset(cloud.x, cloud.y), cloud.scale, cloud.alpha)
            }
        }

        // Nút Back
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkBlueText)
        }

        // Nội dung chính
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { -50 } + fadeIn()) {
                Icon(
                    Icons.Default.LockReset, null,
                    modifier = Modifier.size(90.dp),
                    tint = Color(0xFFF59E0B)
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = isVisible, enter = slideInVertically { 50 } + fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "QUÊN MẬT KHẨU?",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkBlueText,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nhập email đã đăng ký để nhận hướng dẫn khôi phục mật khẩu.",
                        color = DarkBlueText.copy(0.7f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.4f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Brush.linearGradient(listOf(GlassBorder, Color.Transparent, GlassBorder)), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email đăng ký", color = DarkBlueText.copy(0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = DarkBlueText.copy(0.7f)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B), // Cam nắng
                                unfocusedBorderColor = DarkBlueText.copy(0.2f),
                                cursorColor = Color(0xFFF59E0B),
                                focusedTextColor = DarkBlueText,
                                unfocusedTextColor = DarkBlueText,
                                focusedContainerColor = Color.White.copy(0.3f),
                                unfocusedContainerColor = Color.White.copy(0.3f)
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        val scale by animateFloatAsState(if (isLoading) 0.95f else 1f, label = "btn")
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.sendPasswordResetEmail(email.trim(), context) {
                                    isLoading = false
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp).scale(scale),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text("GỬI YÊU CẦU", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCloudForgot(offset: Offset, scale: Float, alpha: Float) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val baseRadius = 30.dp.toPx() * scale
    drawCircle(cloudColor, baseRadius, offset)
    drawCircle(cloudColor, baseRadius * 0.8f, Offset(offset.x - baseRadius * 0.7f, offset.y + baseRadius * 0.2f))
    drawCircle(cloudColor, baseRadius * 0.9f, Offset(offset.x + baseRadius * 0.7f, offset.y + baseRadius * 0.1f))
}