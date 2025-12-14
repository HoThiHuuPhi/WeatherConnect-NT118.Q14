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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity // Cần thiết để chuyển đổi Dp -> Int
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset // Cần thiết cho Popup offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.doanck.R

enum class MainTab {
    WEATHER,
    COMMUNITY,
    SEARCH,
    SETTINGS
}

private val SmokedGlassGradient = Brush.verticalGradient(
    colors = listOf(
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

private val MapMenuSmokedGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF7BCBEC).copy(alpha = 0.25f),
        Color(0xFFB0BEC5).copy(alpha = 0.10f)
    )
)

@Composable
fun SmokedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = Modifier
            .background(MapMenuSmokedGradient, RoundedCornerShape(4.dp))
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}


@Composable
private fun MenuItemContent(
    text: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color(0xFF1E3A8A),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun MainTopNavBar(
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
                .size(66.dp)
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

            SmokedDropdownMenu(
                expanded = mapMenuExpanded,
                onDismissRequest = { mapMenuExpanded = false }
            ) {
                MenuItemContent(
                    text = "Bản đồ thời tiết",
                    iconRes = R.drawable.ic_weather_map,
                    onClick = { mapMenuExpanded = false; onOpenWeatherMap() }
                )

                MenuItemContent(
                    text = "Bản đồ cứu trợ",
                    iconRes = R.drawable.ic_sos_map,
                    onClick = { mapMenuExpanded = false; onOpenRescueMap() }
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(66.dp)
                .clip(RoundedCornerShape(100))
                .background(SmokedGlassGradient)
                .border(BorderStroke(1.dp, SoftBorderGradient), RoundedCornerShape(100))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
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
                    label = "Tìm kiếm",
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
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(Modifier.height(8.dp))
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