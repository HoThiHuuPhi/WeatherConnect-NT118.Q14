package com.example.doanck.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doanck.R

enum class MainTab {
    WEATHER,
    COMMUNITY,
    SEARCH,
    SETTINGS
}

private val SmokedGlassGradient = Brush.verticalGradient(
    colors = listOf(
        // Màu gốc: Blue Grey 100 (Xám xanh rất nhạt)
        Color(0xFF7BCBEC).copy(alpha = 0.25f),
        Color(0xFFB0BEC5).copy(alpha = 0.10f)
    )
)

private val SoftBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.05f)
    )
)

private val MenuBackground = Color(0xFF263238).copy(alpha = 0.95f)

@Composable
fun MainTopNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onOpenWeatherMap: () -> Unit,
    onOpenRescueMap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var mapMenuExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SmokedGlassGradient)
                .border(BorderStroke(1.dp, SoftBorderGradient), CircleShape)
                .clickable { mapMenuExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_map),
                contentDescription = "Bản đồ",
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            DropdownMenu(
                expanded = mapMenuExpanded,
                onDismissRequest = { mapMenuExpanded = false },
                modifier = Modifier.background(MenuBackground)
            ) {
                DropdownMenuItem(
                    text = { Text("Bản đồ thời tiết", color = Color.White) },
                    onClick = { mapMenuExpanded = false; onOpenWeatherMap() }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                DropdownMenuItem(
                    text = { Text("Bản đồ cứu trợ", color = Color.White) },
                    onClick = { mapMenuExpanded = false; onOpenRescueMap() }
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // --- 2. THANH MENU ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(100))
                .background(SmokedGlassGradient)
                .border(BorderStroke(1.dp, SoftBorderGradient), RoundedCornerShape(100))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavPillItem(
                    iconRes = R.drawable.ic_tab_weather,
                    label = "Thời tiết",
                    onClick = { onTabSelected(MainTab.WEATHER) }
                )
                NavPillItem(
                    iconRes = R.drawable.ic_tab_chat,
                    label = "Chat",
                    onClick = { onTabSelected(MainTab.COMMUNITY) }
                )
                NavPillItem(
                    iconRes = R.drawable.ic_tab_search,
                    label = "Tìm",
                    onClick = { onTabSelected(MainTab.SEARCH) }
                )
                NavPillItem(
                    iconRes = R.drawable.ic_tab_settings,
                    label = "Cài đặt",
                    onClick = { onTabSelected(MainTab.SETTINGS) }
                )
            }
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
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    blurRadius = 4f
                )
            )
        )
    }
}