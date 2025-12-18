package com.example.doanck.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.doanck.R

enum class MainTab {
    WEATHER, COMMUNITY, SEARCH, SETTINGS
}

private val HourlyGlassDark = Color(0xFF020617).copy(alpha = 0.4f)

private val FloatingMenuGradient = Color(0xFF747F94).copy(alpha = 0.9f)

@Composable
fun MainTopNavBar(
    onTabSelected: (MainTab) -> Unit,
    onOpenWeatherMap: () -> Unit,
    onOpenRescueMap: () -> Unit
) {
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var mapMenuExpanded by remember { mutableStateOf(false) }

        // --- BUTTON BẢN ĐỒ ---
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(HourlyGlassDark)
                    .clickable { mapMenuExpanded = !mapMenuExpanded },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_map),
                    contentDescription = "Bản đồ",
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
            if (mapMenuExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { -136.dp.roundToPx() }),
                    onDismissRequest = { mapMenuExpanded = false }
                ) {
                    FloatingMenuContent(
                        onOpenWeatherMap = { mapMenuExpanded = false; onOpenWeatherMap() },
                        onOpenRescueMap = { mapMenuExpanded = false; onOpenRescueMap() }
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // --- THANH TAB NAVIGATION ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(66.dp)
                .clip(RoundedCornerShape(100))
                .background(HourlyGlassDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavPillItem(R.drawable.ic_tab_weather, "Thời tiết") { onTabSelected(MainTab.WEATHER) }
                NavPillItem(R.drawable.ic_tab_chat, "Chat") { onTabSelected(MainTab.COMMUNITY) }
                NavPillItem(R.drawable.ic_tab_search, "Tìm kiếm") { onTabSelected(MainTab.SEARCH) }
                NavPillItem(R.drawable.ic_tab_settings, "Cài đặt") { onTabSelected(MainTab.SETTINGS) }
            }
        }
    }
}

@Composable
private fun FloatingMenuContent(
    onOpenWeatherMap: () -> Unit,
    onOpenRescueMap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FloatingOptionItem(
            text = "Bản đồ thời tiết",
            iconRes = R.drawable.ic_weather_map,
            onClick = onOpenWeatherMap
        )

        FloatingOptionItem(
            text = "Bản đồ cứu trợ",
            iconRes = R.drawable.ic_sos_map,
            onClick = onOpenRescueMap
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FloatingOptionItem(
    text: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(FloatingMenuGradient)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                style = LocalTextStyle.current.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}


@Composable
private fun RowScope.NavPillItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    blurRadius = 4f
                )
            )
        )
    }
}