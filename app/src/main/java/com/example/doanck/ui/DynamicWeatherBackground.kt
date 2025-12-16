package com.example.doanck.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import com.example.doanck.utils.WeatherBackground
import com.example.doanck.utils.WeatherEffectType
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val color: Color = Color.White
)

data class Cloud(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val color: Color
)

@Composable
fun DynamicWeatherBackground(
    backgroundData: WeatherBackground,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val infiniteTransitionSun = rememberInfiniteTransition(label = "sun")
    val sunScale by infiniteTransitionSun.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunScale"
    )

    val particles = remember { mutableStateListOf<Particle>() }
    val clouds = remember { mutableStateListOf<Cloud>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(backgroundData.effectType) {
        particles.clear()
        clouds.clear()

        when (backgroundData.effectType) {

            WeatherEffectType.CLOUDY,
            WeatherEffectType.SUNNY -> {
                repeat(16) {
                    clouds += Cloud(
                        x = random.nextFloat(),
                        y = random.nextFloat() * 0.2f,
                        width = random.nextFloat() * 300f + 380f,
                        height = random.nextFloat() * 120f + 160f,
                        speed = random.nextFloat() * 0.00015f + 0.00005f,
                        color = Color(0xFFFFFFFF).copy(alpha = 0.95f)
                    )
                }
            }

            WeatherEffectType.RAIN -> {
                repeat(80) {
                    particles += Particle(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        size = random.nextFloat() * 8f + 24f,
                        speed = random.nextFloat() * 0.05f + 0.05f,
                        color = Color.White.copy(alpha = random.nextFloat() * 0.4f + 0.4f)
                    )
                }
                repeat(16) {
                    clouds += Cloud(
                        x = random.nextFloat(),
                        y = random.nextFloat() * 0.2f,
                        width = random.nextFloat() * 300f + 380f,
                        height = random.nextFloat() * 120f + 160f,
                        speed = random.nextFloat() * 0.00015f + 0.00005f,
                        color = Color(0xFFD3DAE8).copy(alpha = 0.95f)
                    )
                }
            }

            WeatherEffectType.SNOW -> {
                repeat(60) {
                    particles += Particle(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        size = random.nextFloat() * 8f + 4f,
                        speed = random.nextFloat() * 0.05f + 0.05f,
                        color = Color.White.copy(alpha = random.nextFloat() * 0.8f + 0.2f)
                    )
                }
                repeat(3) {
                    clouds += Cloud(
                        x = random.nextFloat(),
                        y = random.nextFloat() * 0.2f,
                        width = random.nextFloat() * 250f + 320f,
                        height = random.nextFloat() * 80f + 120f,
                        speed = random.nextFloat() * 0.0005f + 0.0002f,
                        color = Color(0xFFF2F2F2).copy(alpha = 0.95f)
                    )
                }
            }

            WeatherEffectType.STORM -> {
                repeat(140) {
                    particles += Particle(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        size = random.nextFloat() * 25f + 40f,
                        speed = random.nextFloat() * 0.09f + 0.12f,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                repeat(5) {
                    clouds += Cloud(
                        x = random.nextFloat(),
                        y = random.nextFloat() * 0.25f,
                        width = random.nextFloat() * 260f + 360f,
                        height = random.nextFloat() * 90f + 140f,
                        speed = random.nextFloat() * 0.0009f + 0.0005f,
                        color = Color(0xFF555555).copy(alpha = 0.95f)
                    )
                }
            }

            WeatherEffectType.STARRY_NIGHT -> {
                repeat(40) {
                    particles += Particle(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        size = random.nextFloat() * 4f + 1f,
                        speed = 0f,
                        color = Color.White.copy(alpha = random.nextFloat() * 0.9f + 0.1f)
                    )
                }
            }

            else -> {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition("weather_anim")

    val tick = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    LaunchedEffect(tick.value) {
        val updatedParticles = mutableListOf<Particle>()
        val updatedClouds = mutableListOf<Cloud>()

        particles.forEachIndexed { index, p ->
            when (backgroundData.effectType) {
                WeatherEffectType.RAIN -> {
                    var newY = p.y + p.speed * 0.12f
                    val newX = p.x
                    if (newY > 1f) newY = random.nextFloat() * -0.2f
                    updatedParticles += p.copy(x = newX, y = newY)
                }

                WeatherEffectType.SNOW -> {
                    var newY = p.y + p.speed * 0.1f
                    val sway = sin((tick.value * 10f) + index) * 0.005f
                    var newX = p.x + sway
                    if (newY > 1f) {
                        newY = random.nextFloat() * -0.2f
                        newX = random.nextFloat()
                    }
                    updatedParticles += p.copy(x = newX, y = newY)
                }

                WeatherEffectType.STORM -> {
                    var newY = p.y + p.speed * 0.2f
                    val newX = p.x
                    if (newY > 1f) newY = random.nextFloat() * -0.3f
                    updatedParticles += p.copy(x = newX, y = newY)
                }

                WeatherEffectType.STARRY_NIGHT -> {
                    updatedParticles += p
                }

                else -> updatedParticles += p
            }
        }

        clouds.forEach { c ->
            var newX = c.x + c.speed
            if (newX > 1.3f) newX = -0.3f
            updatedClouds += c.copy(x = newX)
        }

        particles.clear()
        particles.addAll(updatedParticles)

        clouds.clear()
        clouds.addAll(updatedClouds)
    }

    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        // Vẽ nền
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(backgroundData.gradientStartColor),
                    Color(backgroundData.gradientEndColor)
                )
            )
        )

        // Vẽ mây
        clouds.forEach { c ->
            val cx = c.x * w
            val cy = c.y * h
            val baseRadius = c.width / 4f

            drawCircle(c.color, radius = baseRadius * 1.1f, center = Offset(cx, cy))
            drawCircle(
                c.color,
                radius = baseRadius * 0.8f,
                center = Offset(cx - baseRadius * 1.5f, cy - 20f)
            )
            drawCircle(
                c.color,
                radius = baseRadius * 0.9f,
                center = Offset(cx - baseRadius * 0.8f, cy + 30f)
            )
            drawCircle(
                c.color,
                radius = baseRadius * 0.7f,
                center = Offset(cx + baseRadius * 1.4f, cy - 15f)
            )
            drawCircle(
                c.color,
                radius = baseRadius * 0.6f,
                center = Offset(cx + baseRadius * 0.7f, cy + 40f)
            )
        }

        // Vẽ particles
        particles.forEach { p ->
            val pos = Offset(p.x * w, p.y * h)

            when (backgroundData.effectType) {
                WeatherEffectType.RAIN -> {
                    drawLine(
                        color = p.color,
                        start = pos,
                        end = pos + Offset(0f, p.size),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                WeatherEffectType.SNOW, WeatherEffectType.STARRY_NIGHT -> {
                    drawCircle(color = p.color, radius = p.size, center = pos)
                }

                WeatherEffectType.STORM -> {
                    drawLine(
                        color = p.color,
                        start = pos,
                        end = pos + Offset(0f, p.size),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                else -> drawCircle(color = p.color, radius = p.size, center = pos)
            }
        }

        // --- Vẽ mặt trời **sau cùng** ---
        if (backgroundData.effectType == WeatherEffectType.SUNNY) {
            val sunCenterX = w * 0.85f
            val sunCenterY = h * 0.15f
            val sunRadius = 60f * sunScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDB813).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(sunCenterX, sunCenterY),
                    radius = sunRadius * 2
                ),
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius * 2
            )

            drawCircle(
                color = Color(0xFFFDB813),
                center = Offset(sunCenterX, sunCenterY),
                radius = sunRadius
            )
        }
    }
}

@Preview
@Composable
fun PreviewDynamicWeather() {
    val bg = WeatherBackground(WeatherEffectType.SNOW, 0xFF0B1026, 0xFF0B1026)
    DynamicWeatherBackground(backgroundData = bg)
}